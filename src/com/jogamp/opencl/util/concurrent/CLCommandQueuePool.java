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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * A multithreaded pool of OpenCL command queues.
 * It serves as a multiplexer distributing tasks over N queues.
 * @author Michael Bien
 */
public class CLCommandQueuePool implements CLResource {

    private final List<CLCommandQueue> queues;
    private final ExecutorService excecutor;

    private CLCommandQueuePool(Collection<CLCommandQueue> queues) {
        this.queues = Collections.unmodifiableList(new ArrayList<CLCommandQueue>(queues));
        this.excecutor = Executors.newFixedThreadPool(queues.size(), new QueueThreadFactory(this.queues));
    }

    public static CLCommandQueuePool create(CLMultiContext mc) {
        return create(mc.getDevices());
    }

    public static CLCommandQueuePool create(Collection<CLDevice> devices, CLCommandQueue.Mode... modes) {
        List<CLCommandQueue> queues = new ArrayList<CLCommandQueue>(devices.size());
        for (CLDevice device : devices) {
            queues.add(device.createCommandQueue(modes));
        }
        return create(queues);
    }

    public static CLCommandQueuePool create(Collection<CLCommandQueue> queues) {
        return new CLCommandQueuePool(queues);
    }

    public <T> Future<T> submit(CLTask<T> task) {
        return excecutor.submit(new TaskWrapper(task));
    }

    /**
     * Calls {@link CLCommandQueue#flush()} on all queues.
     */
    public void flush() {
        for (CLCommandQueue queue : queues) {
            queue.flush();
        }
    }

    /**
     * Calls {@link CLCommandQueue#finish()} on all queues.
     */
    public void finish() {
        for (CLCommandQueue queue : queues) {
            queue.finish();
        }
    }

    /**
     * Releases all queues.
     */
    public void release() {
        for (CLCommandQueue queue : queues) {
            queue.finish().release();
        }
    }

    public List<CLCommandQueue> getQueues() {
        return queues;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+" [queues: "+queues.size()+"]";
    }

    private static class QueueThreadFactory implements ThreadFactory {

        private final List<CLCommandQueue> queues;
        private int index;

        private QueueThreadFactory(List<CLCommandQueue> queues) {
            this.queues = queues;
            this.index = 0;
        }

        public synchronized Thread newThread(Runnable r) {
            CLCommandQueue queue = queues.get(index++);
            return new QueueThread(queue);
        }

    }
    
    private static class QueueThread extends Thread {
        private final CLCommandQueue queue;
        public QueueThread(CLCommandQueue queue) {
            this.queue = queue;
        }
    }

    private static class TaskWrapper<T> implements Callable<T> {

        private final CLTask<T> task;
        
        public TaskWrapper(CLTask<T> task) {
            this.task = task;
        }

        public T call() throws Exception {
            QueueThread thread = (QueueThread) Thread.currentThread();
            return task.run(thread.queue);
        }

    }

}
