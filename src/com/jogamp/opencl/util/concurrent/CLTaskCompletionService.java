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
 * Created on Tuesday, July 05 2011 00:26
 */
package com.jogamp.opencl.util.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A {@link CompletionService} for {@link CLTask}s executed in a {@link CLAbstractExecutorService}.
 * It simplifies asynchronous execution of tasks with the same result type in a potentially shared pool.
 * @see CompletionService
 * @author Michael Bien
 */
public class CLTaskCompletionService<R> {

    private final ExecutorCompletionService<R> service;
    private final CLAbstractExecutorService executor;

    /**
     * Creates an CLTaskCompletionService using the supplied executor for base
     * task execution and a LinkedBlockingQueue with the capacity of {@link Integer#MAX_VALUE}
     * as a completion queue.
     */
    public CLTaskCompletionService(CLAbstractExecutorService executor) {
        this.service = new ExecutorCompletionService<R>(executor.getExcecutor());
        this.executor = executor;
    }

    /**
     * Creates an CLTaskCompletionService using the supplied executor for base
     * task execution the supplied queue as its completion queue.
     */
    public CLTaskCompletionService(CLAbstractExecutorService pool, BlockingQueue<Future<R>> queue) {
        this.service = new ExecutorCompletionService<R>(pool.getExcecutor(), queue);
        this.executor = pool;
    }

    /**
     * Submits a CLTask for execution and returns a Future representing the pending
     * results of the task. Upon completion, this task may be taken or polled.
     * @see CompletionService#submit(java.util.concurrent.Callable)
     */
    public Future<R> submit(CLPoolable<? extends CLQueueContext, R> task) {
        return service.submit(executor.wrapTask(task));
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
