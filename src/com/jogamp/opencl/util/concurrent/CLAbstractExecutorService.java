/*
 * Copyright (c) 2011, Michael Bien
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

/*
 * Created on Tuesday, August 02 2011 02:20
 */
package com.jogamp.opencl.util.concurrent;

import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLResource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Common superclass for Executor services supporting OpenCL driven tasks.
 * @author Michael Bien
 */
public abstract class CLAbstractExecutorService implements CLResource {

    protected final ExecutorService excecutor;
    protected final List<CLCommandQueue> queues;

    private FinishAction finishAction = FinishAction.DO_NOTHING;
    private boolean released;

    protected CLAbstractExecutorService(ExecutorService executor, List<CLCommandQueue> queues) {
        this.queues = queues;
        this.excecutor = executor;
    }


    <R> TaskWrapper<R> wrapTask(CLPoolable<? extends CLQueueContext, R> task) {
        return new TaskWrapper(task, finishAction);
    }

    private <R> List<TaskWrapper<R>> wrapTasks(Collection<? extends CLPoolable<? extends CLQueueContext, R>> tasks) {
        List<TaskWrapper<R>> wrapper = new ArrayList<TaskWrapper<R>>(tasks.size());
        for (CLPoolable<? extends CLQueueContext, R> task : tasks) {
            if(task == null) {
                throw new NullPointerException("at least one task was null");
            }
            wrapper.add(new TaskWrapper<R>((CLPoolable<CLQueueContext, R>)task, finishAction));
        }
        return wrapper;
    }

    /**
     * Submits all tasks to the pool for immediate execution (blocking) and returns their {@link Future} holding the result.
     * @see ExecutorService#invokeAll(java.util.Collection)
     */
    public <R> List<Future<R>> invokeAll(Collection<? extends CLPoolable<? extends CLQueueContext, R>> tasks) throws InterruptedException {
        List<TaskWrapper<R>> wrapper = wrapTasks(tasks);
        return excecutor.invokeAll(wrapper);
    }

    /**
     * Submits all tasks to the pool for immediate execution (blocking) and returns their {@link Future} holding the result.
     * @see ExecutorService#invokeAll(java.util.Collection, long, java.util.concurrent.TimeUnit)
     */
    public <R> List<Future<R>> invokeAll(Collection<? extends CLPoolable<? extends CLQueueContext, R>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        List<TaskWrapper<R>> wrapper = wrapTasks(tasks);
        return excecutor.invokeAll(wrapper, timeout, unit);
    }

    /**
     * Submits all tasks for immediate execution (blocking) until a result can be returned.
     * All other unfinished but started tasks are cancelled.
     * @see ExecutorService#invokeAny(java.util.Collection)
     */
    public <R> R invokeAny(Collection<? extends CLPoolable<? extends CLQueueContext, R>> tasks) throws InterruptedException, ExecutionException {
        List<TaskWrapper<R>> wrapper = wrapTasks(tasks);
        return excecutor.invokeAny(wrapper);
    }

    /**
     * Submits all tasks for immediate execution (blocking) until a result can be returned.
     * All other unfinished but started tasks are cancelled.
     * @see ExecutorService#invokeAny(java.util.Collection, long, java.util.concurrent.TimeUnit)
     */
    public <R> R invokeAny(Collection<? extends CLPoolable<? super CLQueueContext, R>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        List<TaskWrapper<R>> wrapper = wrapTasks(tasks);
        return excecutor.invokeAny(wrapper, timeout, unit);
    }

    /**
     * Submits this task to the pool for execution returning its {@link Future}.
     * @see ExecutorService#submit(java.util.concurrent.Callable)
     */
    public <R> Future<R> submit(CLPoolable<? extends CLQueueContext, R> task) {
        return excecutor.submit(wrapTask(task));
    }

    /**
     * Submits all tasks to the pool for execution and returns their {@link Future}.
     * Calls {@link #submit(com.jogamp.opencl.util.concurrent.CLPoolable)} for every task.
     */
    public <R> List<Future<R>> submitAll(Collection<? extends CLPoolable<? extends CLQueueContext, R>> tasks) {
        List<Future<R>> futures = new ArrayList<Future<R>>(tasks.size());
        for (CLPoolable<? extends CLQueueContext, R> task : tasks) {
            futures.add(submit(task));
        }
        return futures;
    }

    /**
     * Calls {@link CLCommandQueue#flush()} on all queues.
     */
    public void flushQueues() {
        for (CLCommandQueue queue : queues) {
            queue.flush();
        }
    }

    /**
     * Calls {@link CLCommandQueue#finish()} on all queues.
     */
    public void finishQueues() {
        for (CLCommandQueue queue : queues) {
            queue.finish();
        }
    }

    /**
     * Returns the command queues used in this pool.
     */
    public List<CLCommandQueue> getQueues() {
        return Collections.unmodifiableList(queues);
    }

    /**
     * Returns the size of this pool (number of command queues).
     */
    public int getPoolSize() {
        return queues.size();
    }
    /**
     * Returns the action which is executed when a task finishes.
     */
    public FinishAction getFinishAction() {
        return finishAction;
    }

    ExecutorService getExcecutor() {
        return excecutor;
    }

    @Override
    public boolean isReleased() {
        return released;
    }

    /**
     * Sets the action which is run after every completed task.
     * This is mainly intended for debugging, default value is {@link FinishAction#DO_NOTHING}.
     */
    public void setFinishAction(FinishAction action) {
        this.finishAction = action;
    }

    /**
     * Releases the queue context, all queues including a shutdown of the internal threadpool.
     * The call will block until all currently executing tasks have finished, no new tasks are started.
     */
    @Override
    public void release() {
        if (released) {
            throw new RuntimeException(getClass().getSimpleName() + " already released");
        }
        released = true;
        excecutor.shutdownNow(); // threads will cleanup CL resources on exit
        try {
            excecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected static interface CommandQueueThread {

        Map<Object, CLQueueContext> getContextMap();

        CLCommandQueue getQueue();
    }

    protected static class TaskWrapper<R> implements Callable<R> {

        protected final CLPoolable<CLQueueContext, R> task;
        private final FinishAction mode;

        private TaskWrapper(CLPoolable<CLQueueContext, R> task, FinishAction mode) {
            this.task = task;
            this.mode = mode;
        }

        @Override
        public R call() throws Exception {

            CommandQueueThread thread = (CommandQueueThread)Thread.currentThread();

            final Object key = task.getContextKey();

            CLQueueContext context = thread.getContextMap().get(key);
            if(context == null) {
                context = task.createQueueContext(thread.getQueue());
                thread.getContextMap().put(key, context);
            }

            R result = task.execute(context);
            if(mode.equals(FinishAction.FLUSH)) {
                context.queue.flush();
            }else if(mode.equals(FinishAction.FINISH)) {
                context.queue.finish();
            }
            return result;
        }

    }

    /**
     * The action executed after a task completes.
     */
    public enum FinishAction {

        /**
         * Does nothing, the task is responsible to make sure all computations
         * have finished when the task finishes
         */
        DO_NOTHING,

        /**
         * Flushes the queue on task completion.
         */
        FLUSH,

        /**
         * Finishes the queue on task completion.
         */
        FINISH
    }

}
