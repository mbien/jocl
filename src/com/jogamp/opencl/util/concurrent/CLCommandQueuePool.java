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
 * Created on Tuesday, May 03 2011
 */
package com.jogamp.opencl.util.concurrent;

import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.util.CLMultiContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A multithreaded, fixed size pool of OpenCL command queues.
 * <p>
 * CLCommandQueuePool serves as a multiplexer distributing tasks over N queues usually connected to N devices.
 * The usage of this pool is similar to {@link ExecutorService} but it uses {@link CLTask}s
 * instead of {@link Callable}s and provides a per-queue context for resource sharing across all tasks of one queue.
 * </p>
 * @author Michael Bien
 */
public class CLCommandQueuePool extends CLAbstractExecutorService {


    private CLCommandQueuePool(ExecutorService executor, List<CLCommandQueue> queues) {
        super(executor, queues);
    }

    public static CLCommandQueuePool create(CLMultiContext mc, CLCommandQueue.Mode... modes) {
        return create(mc.getDevices(), modes);
    }

    public static CLCommandQueuePool create(Collection<? extends CLDevice> devices, CLCommandQueue.Mode... modes) {
        List<CLCommandQueue> queues = new ArrayList<CLCommandQueue>(devices.size());
        for (CLDevice device : devices) {
            queues.add(device.createCommandQueue(modes));
        }
        return create(queues);
    }

    public static CLCommandQueuePool create(Collection<CLCommandQueue> queues) {
        
        List<CLCommandQueue> list = new ArrayList<CLCommandQueue>(queues);

        BlockingQueue<Runnable> queue = new LinkedBlockingDeque<Runnable>();
        CommandQueuePoolThreadFactory factory = new CommandQueuePoolThreadFactory(list);
        int size = list.size();

        ExecutorService executor = new CLThreadPoolExecutor(size, size, 0L, TimeUnit.MILLISECONDS, queue, factory);
        return new CLCommandQueuePool(executor, list);
    }

    /*public*/ CLPoolable<? extends CLQueueContext, ?> takeCLTask() throws InterruptedException {
        return ((CLFutureTask<?>)getExcecutor().getQueue().take()).getCLPoolable();
    }

    /**
     * Returns the approximate total number of tasks that have ever been scheduled for execution.
     * Because the states of tasks and threads may change dynamically during computation, the returned
     * value is only an approximation.
     */
    public long getTaskCount() {
        return getExcecutor().getTaskCount();
    }

    /**
     * Returns the approximate total number of tasks that have completed execution.
     * Because the states of tasks and threads may change dynamically during computation,
     * the returned value is only an approximation, but one that does not ever decrease across successive calls.
     */
    public long getCompletedTaskCount() {
        return getExcecutor().getCompletedTaskCount();
    }

    /**
     * Returns the approximate number of queues that are actively executing tasks.
     */
    public int getActiveCount() {
        return getExcecutor().getActiveCount();
    }

    @Override
    ThreadPoolExecutor getExcecutor() {
        return (ThreadPoolExecutor) excecutor;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+" [queues: "+getPoolSize()+" on finish: "+getFinishAction()+"]";
    }

    private static class CommandQueuePoolThreadFactory implements ThreadFactory {

        private final List<CLCommandQueue> queues;
        private int index;

        public CommandQueuePoolThreadFactory(List<CLCommandQueue> queues) {
            this.queues = queues;
            this.index = 0;
        }

        @Override
        public synchronized Thread newThread(Runnable runnable) {

            SecurityManager sm = System.getSecurityManager();
            ThreadGroup group = (sm != null) ? sm.getThreadGroup() : Thread.currentThread().getThreadGroup();

            CLCommandQueue queue = queues.get(index);
            Thread thread = new CommandQueuePoolThread(group, runnable, queue, index++);
            thread.setDaemon(true);

            return thread;
        }

    }

    private static class CommandQueuePoolThread extends Thread implements CommandQueueThread {

        private final CLCommandQueue queue;
        private final Map<Object, CLQueueContext> contextMap;

        public CommandQueuePoolThread(ThreadGroup group, Runnable runnable, CLCommandQueue queue, int index) {
            super(group, runnable, "queue-worker-thread-"+index+"["+queue+"]");
            this.queue = queue;
            this.contextMap = new HashMap<Object, CLQueueContext>();
        }

        @Override
        public void run() {
            super.run();
            //release threadlocal contexts
            queue.finish();
            for (CLQueueContext context : contextMap.values()) {
                context.release();
            }
        }

        @Override
        public CLCommandQueue getQueue() {
            return queue;
        }

        @Override
        public Map<Object, CLQueueContext> getContextMap() {
            return contextMap;
        }

    }

    private static class CLFutureTask<R> extends FutureTask<R> {

        private final TaskWrapper<R> wrapper;

        public CLFutureTask(TaskWrapper<R> wrapper) {
            super(wrapper);
            this.wrapper = wrapper;
        }

        public CLPoolable<? extends CLQueueContext, R> getCLPoolable() {
            return wrapper.task;
        }

    }
    
    private static class CLThreadPoolExecutor extends ThreadPoolExecutor {

        public CLThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        }

        @Override
        protected <R> RunnableFuture<R> newTaskFor(Callable<R> callable) {
            TaskWrapper<R> wrapper = (TaskWrapper<R>)callable;
            return new CLFutureTask<R>(wrapper);
        }

    }

}
