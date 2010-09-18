package com.jogamp.opencl.util;

import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLEventList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * An utility for synchronizing multiple concurrent {@link CLCommandQueue}s.
 * This Barrier can be reused after it has been broken.
 * @author Michael Bien
 */
public class MultiQueueBarrier {

    private CountDownLatch latch;
    private final Set<CLCommandQueue> queues;
    private final int count;

    /**
     * Creates a new MultiQueueBarrier with the given queueCount.
     * It is recommented to use {@link #MultiQueueBarrier(CLCommandQueue... allowedQueues)} if possible
     * which restricts the set of allowed queues for the barrier.
     */
    public MultiQueueBarrier(int queueCount) {
        if(queueCount == 0) {
            throw new IllegalArgumentException("queueCount was 0");
        }
        this.latch = new CountDownLatch(queueCount);
        this.queues = null;
        this.count = queueCount;
    }

    /**
     * Creates a new MultiQueueBarrier for the given queues.
     */
    public MultiQueueBarrier(CLCommandQueue... allowedQueues) {
        if(allowedQueues.length == 0) {
            throw new IllegalArgumentException("allowedQueues was empty");
        }
        this.latch = new CountDownLatch(allowedQueues.length);
        this.count = allowedQueues.length;

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
        synchronized(this) {
            latch.countDown();
        }
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
        synchronized(this) {
            latch.countDown();
        }
        return this;
    }

    /**
     * Blocks until all Threads which called {@link #waitFor}
     * continue execution.
     * This method blocks only once, all subsequent calls are ignored.
     */
    public MultiQueueBarrier await() throws InterruptedException {
        latch.await();
        rebuildBarrierIfBroken();
        return this;
    }
    
    /**
     * @see #await()
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     */
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        boolean ret = latch.await(timeout, unit);
        rebuildBarrierIfBroken();
        return ret;
    }
    
    /**
     * Resets this barrier and unblocks all waiting threads.
     */
    public void resetBarrier() {
        synchronized(this) {
            while(latch.getCount() > 0) {
                latch.countDown();
            }
            // thats OK. Another Thread can not rebuild the barrier since we have the lock.
            // we have to rebuid it here in case there was no thread waiting.
            latch = new CountDownLatch(count);
        }
    }

    private void rebuildBarrierIfBroken() {
        synchronized (this) {
            if (latch.getCount() == 0) {
                latch = new CountDownLatch(count);
            }
        }
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
