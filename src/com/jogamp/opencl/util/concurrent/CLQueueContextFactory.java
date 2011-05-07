/*
 * Created onSaturday, May 07 2011 00:40
 */
package com.jogamp.opencl.util.concurrent;

import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLProgram;

/**
 *
 * @author Michael Bien
 */
public abstract class CLQueueContextFactory<C extends CLQueueContext> {

    /**
     * Creates a new queue context for the given queue.
     * @param old the old context or null.
     */
    public abstract C setup(CLCommandQueue queue, CLQueueContext old);


    /**
     * Creates a simple context factory producing single program contexts.
     * @param source sourcecode of a OpenCL program.
     */
    public static CLSimpleContextFactory createSimple(String source) {
        return new CLSimpleContextFactory(source);
    }

    public static class CLSimpleContextFactory extends CLQueueContextFactory<CLQueueContext.CLSimpleQueueContext> {

        private final String source;

        public CLSimpleContextFactory(String source) {
            this.source = source;
        }

        @Override
        public CLQueueContext.CLSimpleQueueContext setup(CLCommandQueue queue, CLQueueContext old) {
            CLProgram program = queue.getContext().createProgram(source).build(queue.getDevice());
            return new CLQueueContext.CLSimpleQueueContext(queue, program);
        }

    }

}
