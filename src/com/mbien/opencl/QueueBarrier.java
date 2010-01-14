package com.mbien.opencl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Michael Bien
 */
public class QueueBarrier {

    private final CountDownLatch latch;

    public QueueBarrier(int queueCount) {
        this.latch = new CountDownLatch(queueCount);
    }

    /**
     * Blocks the current Thread until the given events on the CLCommandQueue occurred.
     * This method may be invoked concurrently without synchronization on the QueueBarrier object
     * as long each Thread passes a distinct CLCommandQueue as parameter to this method.
     */
    public QueueBarrier waitFor(CLCommandQueue queue, CLEventList events) {
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
     * @see {@link #await()}
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     */
    public QueueBarrier await(long timeout, TimeUnit unit) throws InterruptedException {
        latch.await(timeout, unit);
        return this;
    }

}
