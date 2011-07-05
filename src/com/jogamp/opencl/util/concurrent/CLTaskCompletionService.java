/*
 * Created on Tuesday, July 05 2011 00:26
 */
package com.jogamp.opencl.util.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A {@link CompletionService} for {@link CLTask}s executed in a {@link CLCommandQueuePool}.
 * It simplifies asynchronous execution of tasks with the same result type in a potentially shared pool.
 * @see CompletionService
 * @author Michael Bien
 */
public class CLTaskCompletionService<C extends CLQueueContext, R> {

    private final ExecutorCompletionService<R> service;
    private final CLCommandQueuePool pool;

    /**
     * Creates an CLTaskCompletionService using the supplied pool for base
     * task execution and a LinkedBlockingQueue with the capacity of {@link Integer#MAX_VALUE}
     * as a completion queue.
     */
    public CLTaskCompletionService(CLCommandQueuePool<C> pool) {
        this.service = new ExecutorCompletionService<R>(pool.getExcecutor());
        this.pool = pool;
    }

    /**
     * Creates an CLTaskCompletionService using the supplied pool for base
     * task execution the supplied queue as its completion queue.
     */
    public CLTaskCompletionService(CLCommandQueuePool<C> pool, BlockingQueue queue) {
        this.service = new ExecutorCompletionService<R>(pool.getExcecutor(), queue);
        this.pool = pool;
    }

    /**
     * Submits a CLTask for execution and returns a Future representing the pending
     * results of the task. Upon completion, this task may be taken or polled.
     * @see CompletionService#submit(java.util.concurrent.Callable)
     */
    public Future<R> submit(CLTask<? super C, R> task) {
        return service.submit(pool.wrapTask(task));
    }

    /**
     * Retrieves and removes the Future representing the next completed task, waiting if none are yet present.
     * @see CompletionService#take()
     */
    public Future<R> take() throws InterruptedException {
        return service.take();
    }

    /**
     * Retrieves and removes the Future representing the next completed task or null if none are present.
     * @see CompletionService#poll()
     */
    public Future<R> poll() {
        return service.poll();
    }

    /**
     * Retrieves and removes the Future representing the next completed task, waiting if necessary
     * up to the specified wait time if none are yet present.
     * @see CompletionService#poll(long, java.util.concurrent.TimeUnit)
     */
    public Future<R> poll(long timeout, TimeUnit unit) throws InterruptedException {
        return service.poll(timeout, unit);
    }

}
