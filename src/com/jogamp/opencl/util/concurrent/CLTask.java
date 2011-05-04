/*
 * Created on Tuesday, May 03 2011 18:09
 */
package com.jogamp.opencl.util.concurrent;

import com.jogamp.opencl.CLCommandQueue;

/**
 * A task executed on a command queue.
 * @author Michael Bien
 */
public interface CLTask<R> {

    /**
     * Runs the task on a queue and returns its result.
     */
    R run(CLCommandQueue queue);

}
