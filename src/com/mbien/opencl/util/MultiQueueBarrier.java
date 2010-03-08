package com.mbien.opencl.util;

import com.mbien.opencl.CLCommandQueue;
import com.mbien.opencl.CLEventList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * An utility for synchronizing multiple concurrent {@link CLCommandQueue}s.
 * @author Michael Bien
 */
public class MultiQueueBarrier {

    private CountDownLatch latch;
    private final Set<CLCommandQueue> queues;

    /**
     * Creates a new MultiQueueBarrier with the given queueCount.
     * It is recommented to use {@link #MultiQueueBarrier(CLCommandQueue... allowedQueues)} if possible
     * which restricts the set of allowed queues for the barrier.
     */
    public MultiQueueBarrier(int queueCount) {
        this.latch = new CountDownLatch(queueCount);
        this.queues = null;
    }

    /**
     * Creates a new MultiQueueBarrier for the given queues.
     */
    public MultiQueueBarrier(CLCommandQueue... allowedQueues) {
        this.latch = new CountDownLatch(allowedQueues.length);

        HashSet<CLCommandQueue> set = new HashSet<CLCommandQueue>(allowedQueues.length);
        for (CLCommandQueue queue : allowedQueues) {
            set.add(queue);
        }
        this.queues = Collections.unmodifiableSet(set);
    }

    /**
     * Blocks the current Thread until all commands on the {@link CLCommandQueue} finished excecution.
     * This method may be invoked concurrently without synchronization on the MultiQueueBarrier object
     * as long each Thread passes a distinct CLCommandQueue as parameter to this method.
     */
    public MultiQueueBarrier waitFor(CLCommandQueue queue) {
        checkQueue(queue);

        queue.putBarrier();
        latch.countDown();
        return this;
    }

    /**
     * Blocks the current Thread until the given events on the {@link CLCommandQueue} occurred.
     * This method may be invoked concurrently without synchronization on the MultiQueueBarrier object
     * as long each Thread passes a distinct CLCommandQueue as parameter to this method.
     */
    public MultiQueueBarrier waitFor(CLCommandQueue queue, CLEventList events) {
        checkQueue(queue);

        queue.putWaitForEvents(events, true);
        latch.countDown();
        return this;
    }

    /**
     * Blocks until all Threads which called {@link #waitFor}
     * continue execution.
     * This method blocks only once, all subsequent calls are ignored.
     */
    public MultiQueueBarrier await() throws InterruptedException {
        latch.await();
        return this;
    }
    
    /**
     * @see #await()
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     */
    public MultiQueueBarrier await(long timeout, TimeUnit unit) throws InterruptedException {
        latch.await(timeout, unit);
        return this;
    }
    
    /**
     * Cancels this barrier and unblocks all waiting threads.
     */
    public void cancelBarrier() {
        while(latch.getCount() > 0)
            latch.countDown();
    }

    /**
     * Returns the current number of events which must occure before this barrier unblocks the waiting threads.
     * This method is typically used for debugging and testing purposes.
     */
    public long getCount() {
        return latch.getCount();
    }

    private void checkQueue(CLCommandQueue queue) throws IllegalArgumentException {
        if (queues != null && !queues.contains(queue)) {
            throw new IllegalArgumentException(queue + " is not in the allowedQueues Set: " + queues);
        }
    }

}
