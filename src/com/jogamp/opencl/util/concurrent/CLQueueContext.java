/*
 * Created on Friday, May 06 2011 21:02
 */
package com.jogamp.opencl.util.concurrent;

import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLProgram;
import com.jogamp.opencl.CLResource;
import java.util.Map;

/**
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

        public CLProgram getProgram() {
            return program;
        }

        public void release() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }

}
