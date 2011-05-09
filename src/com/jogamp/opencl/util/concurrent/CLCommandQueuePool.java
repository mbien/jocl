/*
 * Created on Tuesday, May 03 2011
 */
package com.jogamp.opencl.util.concurrent;

import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLResource;
import com.jogamp.opencl.util.CLMultiContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A multithreaded, fixed size pool of OpenCL command queues.
 * It serves as a multiplexer distributing tasks over N queues usually run on N devices.
 * The usage of this pool is similar to {@link ExecutorService} but it uses {@link CLTask}s
 * instead of {@link Callable}s and provides a per-queue context for resource sharing across all tasks of one queue.
 * @author Michael Bien
 */
public class CLCommandQueuePool<C extends CLQueueContext> implements CLResource {

    private List<CLQueueContext> contexts;
    private ExecutorService excecutor;
    private FinishAction finishAction = FinishAction.DO_NOTHING;

    private CLCommandQueuePool(CLQueueContextFactory factory, Collection<CLCommandQueue> queues) {
        this.contexts = initContexts(queues, factory);
        initExecutor();
    }

    private List<CLQueueContext> initContexts(Collection<CLCommandQueue> queues, CLQueueContextFactory factory) {
        List<CLQueueContext> newContexts = new ArrayList<CLQueueContext>(queues.size());
        
        int index = 0;
        for (CLCommandQueue queue : queues) {
            
            CLQueueContext old = null;
            if(this.contexts != null && !this.contexts.isEmpty()) {
                old = this.contexts.get(index++);
                old.release();
            }
            
            newContexts.add(factory.setup(queue, old));
        }
        return newContexts;
    }

    private void initExecutor() {
        this.excecutor = Executors.newFixedThreadPool(contexts.size(), new QueueThreadFactory(contexts));
    }

    public static <C extends CLQueueContext> CLCommandQueuePool<C> create(CLQueueContextFactory<C> factory, CLMultiContext mc, CLCommandQueue.Mode... modes) {
        return create(factory, mc.getDevices(), modes);
    }

    public static <C extends CLQueueContext> CLCommandQueuePool<C> create(CLQueueContextFactory<C> factory, Collection<CLDevice> devices, CLCommandQueue.Mode... modes) {
        List<CLCommandQueue> queues = new ArrayList<CLCommandQueue>(devices.size());
        for (CLDevice device : devices) {
            queues.add(device.createCommandQueue(modes));
        }
        return create(factory, queues);
    }

    public static <C extends CLQueueContext> CLCommandQueuePool create(CLQueueContextFactory<C> factory, Collection<CLCommandQueue> queues) {
        return new CLCommandQueuePool(factory, queues);
    }

    /**
     * Submits this task to the pool for execution returning its {@link Future}.
     * @see ExecutorService#submit(java.util.concurrent.Callable)
     */
    public <R> Future<R> submit(CLTask<? super C, R> task) {
        return excecutor.submit(new TaskWrapper(task, finishAction));
    }

    /**
     * Submits all tasks to the pool for execution and returns their {@link Future}.
     * Calls {@link #submit(com.jogamp.opencl.util.concurrent.CLTask)} for every task.
     */
    public <R> List<Future<R>> submitAll(Collection<? extends CLTask<? super C, R>> tasks) {
        List<Future<R>> futures = new ArrayList<Future<R>>(tasks.size());
        for (CLTask<? super C, R> task : tasks) {
            futures.add(submit(task));
        }
        return futures;
    }

    /**
     * Submits all tasks to the pool for immediate execution (blocking) and returns their {@link Future} holding the result.
     * @see ExecutorService#invokeAll(java.util.Collection) 
     */
    public <R> List<Future<R>> invokeAll(Collection<? extends CLTask<? super C, R>> tasks) throws InterruptedException {
        List<TaskWrapper<C, R>> wrapper = wrapTasks(tasks);
        return excecutor.invokeAll(wrapper);
    }

    /**
     * Submits all tasks to the pool for immediate execution (blocking) and returns their {@link Future} holding the result.
     * @see ExecutorService#invokeAll(java.util.Collection, long, java.util.concurrent.TimeUnit)
     */
    public <R> List<Future<R>> invokeAll(Collection<? extends CLTask<? super C, R>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        List<TaskWrapper<C, R>> wrapper = wrapTasks(tasks);
        return excecutor.invokeAll(wrapper, timeout, unit);
    }

    private <R> List<TaskWrapper<C, R>> wrapTasks(Collection<? extends CLTask<? super C, R>> tasks) {
        List<TaskWrapper<C, R>> wrapper = new ArrayList<TaskWrapper<C, R>>(tasks.size());
        for (CLTask<? super C, R> task : tasks) {
            if(task == null) {
                throw new NullPointerException("at least one task was null");
            }
            wrapper.add(new TaskWrapper<C, R>(task, finishAction));
        }
        return wrapper;
    }
    
    /**
     * Switches the context of all queues - this operation can be expensive.
     * Blocks until all tasks finish and sets up a new context for all queues.
     * @return this
     */
    public <C extends CLQueueContext> CLCommandQueuePool switchContext(CLQueueContextFactory<C> factory) {
        
        excecutor.shutdown();
        finishQueues(); // just to be sure
        
        contexts = initContexts(getQueues(), factory);
        initExecutor();
        return this;
    }

    /**
     * Calls {@link CLCommandQueue#flush()} on all queues.
     */
    public void flushQueues() {
        for (CLQueueContext context : contexts) {
            context.queue.flush();
        }
    }

    /**
     * Calls {@link CLCommandQueue#finish()} on all queues.
     */
    public void finishQueues() {
        for (CLQueueContext context : contexts) {
            context.queue.finish();
        }
    }

    /**
     * Releases all queues.
     */
    public void release() {
        excecutor.shutdown();
        for (CLQueueContext context : contexts) {
            context.queue.finish().release();
            context.release();
        }
    }

    /**
     * Returns the command queues used in this pool.
     */
    public List<CLCommandQueue> getQueues() {
        List<CLCommandQueue> queues = new ArrayList<CLCommandQueue>(contexts.size());
        for (CLQueueContext context : contexts) {
            queues.add(context.queue);
        }
        return queues;
    }

    /**
     * Returns the size of this pool (number of command queues).
     */
    public int getSize() {
        return contexts.size();
    }

    public FinishAction getFinishAction() {
        return finishAction;
    }

    /**
     * Sets the action which is run after every completed task.
     * This is mainly intended for debugging, default value is {@link FinishAction#DO_NOTHING}.
     */
    public void setFinishAction(FinishAction action) {
        this.finishAction = action;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+" [queues: "+contexts.size()+" on finish: "+finishAction+"]";
    }

    private static class QueueThreadFactory implements ThreadFactory {

        private final List<CLQueueContext> context;
        private int index;

        private QueueThreadFactory(List<CLQueueContext> queues) {
            this.context = queues;
            this.index = 0;
        }

        public synchronized Thread newThread(Runnable runnable) {

            SecurityManager sm = System.getSecurityManager();
            ThreadGroup group = (sm != null) ? sm.getThreadGroup() : Thread.currentThread().getThreadGroup();

            CLQueueContext queue = context.get(index);
            QueueThread thread = new QueueThread(group, runnable, queue, index++);
            thread.setDaemon(true);
            
            return thread;
        }

    }
    
    private static class QueueThread extends Thread {
        private final CLQueueContext context;
        public QueueThread(ThreadGroup group, Runnable runnable, CLQueueContext context, int index) {
            super(group, runnable, "queue-worker-thread-"+index+"["+context+"]");
            this.context = context;
        }
    }

    private static class TaskWrapper<C extends CLQueueContext, R> implements Callable<R> {

        private final CLTask<? super C, R> task;
        private final FinishAction mode;
        
        public TaskWrapper(CLTask<? super C, R> task, FinishAction mode) {
            this.task = task;
            this.mode = mode;
        }

        public R call() throws Exception {
            CLQueueContext context = ((QueueThread)Thread.currentThread()).context;
            R result = task.execute((C)context);
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
