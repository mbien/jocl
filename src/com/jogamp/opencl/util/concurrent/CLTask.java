/*
 * Created on Tuesday, May 03 2011 18:09
 */
package com.jogamp.opencl.util.concurrent;


/**
 * A task executed on a command queue.
 * @author Michael Bien
 */
public interface CLTask<C extends CLQueueContext, R> {

    /**
     * Runs the task on a queue and returns a result.
     */
    R execute(C context);

}
