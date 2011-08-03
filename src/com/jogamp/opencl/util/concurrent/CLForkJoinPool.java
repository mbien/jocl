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
 * Created on Tuesday, August 02 2011 01:53
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.Future;

/**
 * A multithreaded, fixed size pool of OpenCL command queues supporting fork-join tasks.
 * <p>
 * The usage is similar to {@link ForkJoinPool} but uses {@link CLRecursiveTask}s.
 * </p>
 * @see CLRecursiveTask
 * @author Michael Bien
 */
public class CLForkJoinPool extends CLExecutorService {

    private CLForkJoinPool(ExecutorService executor, List<CLCommandQueue> queues) {
        super(executor, queues);
    }

    public static CLForkJoinPool create(CLMultiContext mc, CLCommandQueue.Mode... modes) {
        return create(mc.getDevices(), modes);
    }

    public static CLForkJoinPool create(Collection<? extends CLDevice> devices, CLCommandQueue.Mode... modes) {
        List<CLCommandQueue> queues = new ArrayList<CLCommandQueue>(devices.size());
        for (CLDevice device : devices) {
            queues.add(device.createCommandQueue(modes));
        }
        return create(queues);
    }

    public static CLForkJoinPool create(Collection<CLCommandQueue> queues) {

        List<CLCommandQueue> list = new ArrayList<CLCommandQueue>(queues);

        CLThreadFactory factory = new CLThreadFactory(list);
        int size = list.size();

        ExecutorService service = new ForkJoinPool(size, factory, null, false);
        return new CLForkJoinPool(service, list);
    }

    /**
     * Performs the given task, returning its result upon completion.
     * @see ForkJoinPool#invoke(java.util.concurrent.ForkJoinTask) 
     */
    public <R> R invoke(CLRecursiveTask<? extends CLQueueContext, R> task) {
        // shortcut, prevents redundant wrapping
        return getExcecutor().invoke(task);
    }

    /**
     * Submits this task to the pool for execution returning its {@link Future}.
     * @see ForkJoinPool#submit(java.util.concurrent.ForkJoinTask)
     */
    public <R> Future<R> submit(CLRecursiveTask<? extends CLQueueContext, R> task) {
        // shortcut, prevents redundant wrapping
        return getExcecutor().submit(task);
    }

    @Override
    ForkJoinPool getExcecutor() {
        return (ForkJoinPool) super.getExcecutor();
    }

    /**
     * Returns an estimate of the total number of tasks stolen from
     * one thread's work queue by another.
     * @see ForkJoinPool#getStealCount()
     */
    public long getStealCount() {
        return getExcecutor().getStealCount();
    }

    /**
     * Returns an estimate of the number of tasks submitted to this
     * pool that have not yet begun executing. This method may take
     * time proportional to the number of submissions.
     * @see ForkJoinPool#getQueuedSubmissionCount()
     */
    public int getQueuedSubmissionCount() {
        return getExcecutor().getQueuedSubmissionCount();
    }

    /**
     * Returns an estimate of the total number of tasks currently held
     * in queues by worker threads (but not including tasks submitted
     * to the pool that have not begun executing). This value is only
     * an approximation, obtained by iterating across all threads in
     * the pool. This method may be useful for tuning task
     * granularities.
     * @see ForkJoinPool#getQueuedTaskCount()
     */
    public long getQueuedTaskCount() {
        return getExcecutor().getQueuedTaskCount();
    }

    /**
     * Returns {@code true} if there are any tasks submitted to this
     * pool that have not yet begun executing.
     * @see ForkJoinPool#hasQueuedSubmissions()
     */
    public boolean hasQueuedSubmissions() {
        return getExcecutor().hasQueuedSubmissions();
    }

    /**
     * Returns {@code true} if all worker threads are currently idle.
     * An idle worker is one that cannot obtain a task to execute
     * because none are available to steal from other threads, and
     * there are no pending submissions to the pool. This method is
     * conservative; it might not return {@code true} immediately upon
     * idleness of all threads, but will eventually become true if
     * threads remain inactive.
     * @see ForkJoinPool#isQuiescent()
     */
    public boolean isQuiescent() {
        return getExcecutor().isQuiescent();
    }

    private static class CLThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

        private int index = 0;
        private final List<CLCommandQueue> queues;

        private CLThreadFactory(List<CLCommandQueue> queues) {
            this.queues = queues;
        }

        @Override
        public synchronized ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            return new ForkJoinQueueWorkerThread(pool, queues.get(index++));
        }

    }

    final static class ForkJoinQueueWorkerThread extends ForkJoinWorkerThread implements CommandQueueThread {

        private final CLCommandQueue queue;
        private final Map<Object, CLQueueContext> contextMap;

        public ForkJoinQueueWorkerThread(ForkJoinPool pool, CLCommandQueue queue) {
            super(pool);
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
        public Map<Object, CLQueueContext> getContextMap() {
            return contextMap;
        }

        @Override
        public CLCommandQueue getQueue() {
            return queue;
        }
    }


}
