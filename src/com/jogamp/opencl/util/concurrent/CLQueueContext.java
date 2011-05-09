/*
 * Created on Friday, May 06 2011 21:02
 */
package com.jogamp.opencl.util.concurrent;

import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLProgram;
import com.jogamp.opencl.CLResource;
import java.util.Map;

/**
 * Superclass for all per-queue contexts as used in {@link CLCommandQueuePool}s.
 * A context will usually hold queue (and therefore often device) specific resources used
 * in tasks of the same queue.
 * <p>
 * Possible candidates for those resources can be compiled CLPrograms, CLKernels
 * or even pre allocated CLBuffers.
 * </p>
 * @author Michael Bien
 */
public abstract class CLQueueContext implements CLResource {

    public final CLCommandQueue queue;

    public CLQueueContext(CLCommandQueue queue) {
        this.queue = queue;
    }

    public CLCommandQueue getQueue() {
        return queue;
    }

    public CLContext getCLContext() {
        return queue.getContext();
    }

    /**
     * A simple queue context holding a precompiled program and its kernels.
     * @author Michael Bien
     */
    public static class CLSimpleQueueContext extends CLQueueContext {

        public final CLProgram program;
        public final Map<String, CLKernel> kernels;

        public CLSimpleQueueContext(CLCommandQueue queue, CLProgram program) {
            super(queue);
            this.program = program;
            this.kernels = program.createCLKernels();
        }

        public Map<String, CLKernel> getKernels() {
            return kernels;
        }

        public CLKernel getKernel(String name) {
            return kernels.get(name);
        }

        public CLProgram getProgram() {
            return program;
        }

        public void release() {
            program.release();
        }

    }

}
