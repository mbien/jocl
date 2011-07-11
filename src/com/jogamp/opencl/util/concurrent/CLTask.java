/*
 * Created on Tuesday, May 03 2011 18:09
 */
package com.jogamp.opencl.util.concurrent;

import com.jogamp.opencl.CLCommandQueue;


/**
 * A task executed on a command queue.
 * @author Michael Bien
 */
public abstract class CLTask<C extends CLQueueContext, R> {


    /**
     * Creates a CLQueueContext for this task. A context may contain static resources
     * like OpenCL program binaries or pre allocated buffers. A context can be used by an group
     * of tasks identified by a common context key ({@link #getContextKey()}). This method
     * won't be called if a context was already created by an previously executed task with the
     * same context key as this task.
     */
    public abstract C createQueueContext(CLCommandQueue queue);

    /**
     * Returns the context key for this task. Default implementation returns {@link #getClass()}.
     */
    public Object getContextKey() {
        return getClass();
    }

    /**
     * Runs the task on a queue and returns a result.
     */
    public abstract R execute(C context);

}
