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
 * The usage of this pool is similar to {@link ExecutorService} but it uses {@link CLTask}s
 * instead of {@link Callable}s.
 * @author Michael Bien
 */
public class CLCommandQueuePool implements CLResource {

    private final List<CLCommandQueue> queues;
    private final ExecutorService excecutor;
    private FinishAction finishAction = FinishAction.DO_NOTHING;

    private CLCommandQueuePool(Collection<CLCommandQueue> queues) {
        this.queues = Collections.unmodifiableList(new ArrayList<CLCommandQueue>(queues));
        this.excecutor = Executors.newFixedThreadPool(queues.size(), new QueueThreadFactory(this.queues));
    }

    public static CLCommandQueuePool create(CLMultiContext mc, CLCommandQueue.Mode... modes) {
        return create(mc.getDevices(), modes);
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

    public <R> Future<R> submit(CLTask<R> task) {
        return excecutor.submit(new TaskWrapper(task, finishAction));
    }

    public <R> List<Future<R>> invokeAll(Collection<CLTask<R>> tasks) throws InterruptedException {
        List<TaskWrapper<R>> wrapper = new ArrayList<TaskWrapper<R>>(tasks.size());
        for (CLTask<R> task : tasks) {
            wrapper.add(new TaskWrapper<R>(task, finishAction));
        }
        return excecutor.invokeAll(wrapper);
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
        excecutor.shutdown();
    }

    /**
     * Returns the command queues used in this pool.
     */
    public List<CLCommandQueue> getQueues() {
        return queues;
    }

    /**
     * Returns the size of this pool (number of command queues).
     */
    public int getSize() {
        return queues.size();
    }

    public FinishAction getFinishAction() {
        return finishAction;
    }

    public void setFinishAction(FinishAction action) {
        this.finishAction = action;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+" [queues: "+queues.size()+" on finish: "+finishAction+"]";
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
        private final FinishAction mode;
        
        public TaskWrapper(CLTask<T> task, FinishAction mode) {
            this.task = task;
            this.mode = mode;
        }

        public T call() throws Exception {
            CLCommandQueue queue = ((QueueThread)Thread.currentThread()).queue;
            T result = task.run(queue);
            if(mode.equals(FinishAction.FLUSH)) {
                queue.flush();
            }else if(mode.equals(FinishAction.FINISH)) {
                queue.finish();
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
