package com.mbien.opencl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Michael Bien
 */
public class QueueBarrier {

    private final CountDownLatch latch;
    private final Set<CLCommandQueue> queues;

    public QueueBarrier(int queueCount) {
        this.latch = new CountDownLatch(queueCount);
        this.queues = null;
    }

    public QueueBarrier(CLCommandQueue... allowedQueues) {
        this.latch = new CountDownLatch(allowedQueues.length);

        HashSet<CLCommandQueue> set = new HashSet<CLCommandQueue>(allowedQueues.length);
        for (CLCommandQueue queue : allowedQueues) {
            set.add(queue);
        }
        this.queues = Collections.unmodifiableSet(set);
    }

    /**
     * Blocks the current Thread until all commands on the CLCommandQueue finished excecution.
     * This method may be invoked concurrently without synchronization on the QueueBarrier object
     * as long each Thread passes a distinct CLCommandQueue as parameter to this method.
     */
    public QueueBarrier waitFor(CLCommandQueue queue) {
        checkQueue(queue);

        queue.putBarrier();
        latch.countDown();
        return this;
    }

    /**
     * Blocks the current Thread until the given events on the CLCommandQueue occurred.
     * This method may be invoked concurrently without synchronization on the QueueBarrier object
     * as long each Thread passes a distinct CLCommandQueue as parameter to this method.
     */
    public QueueBarrier waitFor(CLCommandQueue queue, CLEventList events) {
        checkQueue(queue);

        queue.putWaitForEvents(events);
        latch.countDown();
        return this;
    }

    /**
     * Blocks until all Threads which called {@link #waitFor}
     * continue excecution.
     * This method blocks only once, all subsequent calls are ignored.
     */
    public QueueBarrier await() throws InterruptedException {
        latch.await();
        return this;
    }
    
    /**
     * @see #await()
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     */
    public QueueBarrier await(long timeout, TimeUnit unit) throws InterruptedException {
        latch.await(timeout, unit);
        return this;
    }

    private final void checkQueue(CLCommandQueue queue) throws IllegalArgumentException {
        if (queues != null && !queues.contains(queue)) {
            throw new IllegalArgumentException(queue + " is not in the allowedQueues Set: " + queues);
        }
    }

}
