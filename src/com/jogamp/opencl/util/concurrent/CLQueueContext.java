/*
 * Copyright (c) 2011, Michael Bien
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

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
    public static class CLSingleProgramQueueContext extends CLResourceQueueContext<CLProgram> {

        public final Map<String, CLKernel> kernels;

        public CLSingleProgramQueueContext(CLCommandQueue queue, CLProgram program) {
            super(queue, program);
            this.kernels = program.createCLKernels();
        }

        public CLSingleProgramQueueContext(CLCommandQueue queue, String... source) {
            this(queue, queue.getContext().createProgram(source).build());
        }

        public Map<String, CLKernel> getKernels() {
            return kernels;
        }

        public CLKernel getKernel(String name) {
            return kernels.get(name);
        }

        public CLProgram getProgram() {
            return getResource();
        }

    }
    
    /**
     * {@link CLQueueContext} serving a single {@link CLResource}.
     */
    public static class CLResourceQueueContext<R extends CLResource> extends CLQueueContext {
        
        public final R resource;

        public CLResourceQueueContext(CLCommandQueue queue, R resource) {
            super(queue);
            this.resource = resource;
        }
        
        @Override
        public void release() {
            synchronized(resource) {
                if(!resource.isReleased()) {
                    resource.release();
                }
            }
        }

        @Override
        public boolean isReleased() {
            return resource.isReleased();
        }

        public R getResource() {
            return resource;
        }
        
        @Override
        public String toString() {
            return getClass().getSimpleName()+"["+resource+"]";
        }
        
    }
    

}
