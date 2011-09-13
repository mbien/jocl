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
 * Created on Monday, July 25 2011 20:51
 */
package com.jogamp.opencl;

import com.jogamp.common.nio.CachedBufferFactory;
import com.jogamp.common.nio.NativeSizeBuffer;


/**
 * CLWork represents a N dimensional range of work items using a specific {@link CLKernel}.
 *
 * @see #create1D(com.jogamp.opencl.CLKernel)
 * @see #create2D(com.jogamp.opencl.CLKernel)
 * @see #create2D(com.jogamp.opencl.CLKernel)
 *
 * @author Michael Bien
 */
public class CLWork {

    public final int dimension;
    public final CLKernel kernel;

    protected final NativeSizeBuffer groupSize;
    protected final NativeSizeBuffer workSize;
    protected final NativeSizeBuffer workOffset;

    protected CLWork(CLKernel kernel, int dimension) {

        int size = dimension * NativeSizeBuffer.elementSize();

        CachedBufferFactory factory = CachedBufferFactory.create(3*size, true);
        this.workOffset = NativeSizeBuffer.wrap(factory.newDirectByteBuffer(size));
        this.workSize   = NativeSizeBuffer.wrap(factory.newDirectByteBuffer(size));
        this.groupSize  = NativeSizeBuffer.wrap(factory.newDirectByteBuffer(size));

        this.dimension  = dimension;
        this.kernel     = kernel;
    }

    /**
     * Creates work representing a 1D range of work items using the supplied kernel.
     */
    public static CLWork1D create1D(CLKernel kernel) {
        return new CLWork1D(kernel);
    }

    /**
     * Creates work representing a 2D range of work items using the supplied kernel.
     */
    public static CLWork2D create2D(CLKernel kernel) {
        return new CLWork2D(kernel);
    }

    /**
     * Creates work representing a 3D range of work items using the supplied kernel.
     */
    public static CLWork3D create3D(CLKernel kernel) {
        return new CLWork3D(kernel);
    }

    public static long roundUp(long value, long multiple) {
        long remaining = value % multiple;
        if (remaining == 0) {
            return value;
        } else {
            return value + multiple - remaining;
        }
    }

    protected void checkSize(long worksize, long groupsize) {
        if(groupsize != 0 && worksize%groupsize != 0) {
            throw new IllegalArgumentException("worksize must be a multiple of groupsize: {ws: "+worksize+", gs:"+groupsize+"}");
        }
    }

    /**
     * Optimizes the work sizes by rounding to the next device-specific preferred multiple.
     * This optimization can break kernels if they where designed to be executed with a fixed
     * group or worksize. This method will do nothing if no group size has been specified.
     * @since OpenCL 1.1
     */
    public CLWork optimizeFor(CLDevice device) {

        long multiple = kernel.getPreferredWorkGroupSizeMultiple(device);
        int[] maxWorkItemSizes = device.getMaxWorkItemSizes();

        for (int i = 0; i < dimension; i++) {
            long group = groupSize.get(i);
            if(group > 0) {
                group = roundUp(group, multiple);
                if(group <= maxWorkItemSizes[i]) {
                    groupSize.put(i, group);

                    long work = workSize.get(i);
                    workSize.put(i, roundUp(work, group));
                }
                //else {can not optimize}
            }
        }
        return this;
    }

    public CLKernel getKernel() {
        return kernel;
    }

    public int getDimension() {
        return dimension;
    }

    public NativeSizeBuffer getGroupSize() {
        return groupSize;
    }

    public NativeSizeBuffer getWorkOffset() {
        return workOffset;
    }

    public NativeSizeBuffer getWorkSize() {
        return workSize;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+ " [" +kernel+toStr(workOffset)+toStr(workSize)+toStr(groupSize)+"]";
    }

    private String toStr(NativeSizeBuffer buffer) {
        if(buffer == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (int i = buffer.position(); i < buffer.capacity(); i++) {
            sb.append(buffer.get(i));
            if(i != buffer.capacity()-1) {
                sb.append(", ");
            }
        }
        return sb.append('}').toString();
    }


    /**
     * 1 dimensional {@link CLWork}.
     * @author Michael Bien
     */
    public static class CLWork1D extends CLWork {

        private CLWork1D(CLKernel kernel) {
            super(kernel, 1);
        }

        public CLWork1D setWorkOffset(long offset) {
            workOffset.put(0, offset);
            return this;
        }

        public CLWork1D setWorkSize(long worksize) {
            setWorkSize(worksize, 0);
            return this;
        }

        public CLWork1D setWorkSize(long worksize, long groupsize) {
            checkSize(worksize, groupsize);
            workSize.put(0, worksize);
            groupSize.put(0, groupsize);
            return this;
        }

        @Override
        public CLWork1D optimizeFor(CLDevice device) {
            super.optimizeFor(device);
            return this;
        }

    }


    /**
     * 2 dimensional {@link CLWork}.
     * @author Michael Bien
     */
    public static class CLWork2D extends CLWork {

        private CLWork2D(CLKernel kernel) {
            super(kernel, 2);
        }

        public CLWork2D setWorkOffset(long offsetX, long offsetY) {
            workOffset.put(0, offsetX).put(1, offsetY);
            return this;
        }

        public CLWork2D setWorkSize(long worksizeX, long worksizeY) {
            setWorkSize(worksizeX, worksizeY, 0,  0);
            return this;
        }

        public CLWork2D setWorkSize(long worksizeX, long worksizeY, long groupsizeX, long groupsizeY) {
            checkSize(worksizeX, groupsizeX);
            checkSize(worksizeY, groupsizeY);
            workSize.put(0, worksizeX).put(1, worksizeY);
            groupSize.put(0, groupsizeX).put(1, groupsizeY);
            return this;
        }

        @Override
        public CLWork2D optimizeFor(CLDevice device) {
            super.optimizeFor(device);
            return this;
        }

    }

    /**
     * 3 dimensional {@link CLWork}.
     * @author Michael Bien
     */
    public static class CLWork3D extends CLWork {

        private CLWork3D(CLKernel kernel) {
            super(kernel, 3);
        }

        public CLWork3D setWorkOffset(long offsetX, long offsetY, long offsetZ) {
            workOffset.put(0, offsetX).put(1, offsetY).put(2, offsetZ);
            return this;
        }

        public CLWork3D setWorkSize(long worksizeX, long worksizeY, long worksizeZ) {
            setWorkSize(worksizeX, worksizeY, worksizeZ, 0,  0,  0);
            return this;
        }

        public CLWork3D setWorkSize(long worksizeX, long worksizeY, long worksizeZ, long groupsizeX, long groupsizeY, long groupsizeZ) {
            checkSize(worksizeX, groupsizeX);
            checkSize(worksizeY, groupsizeY);
            checkSize(worksizeZ, groupsizeZ);
            workSize.put(0, worksizeX).put(1, worksizeY).put(2, worksizeZ);
            groupSize.put(0, groupsizeX).put(1, groupsizeY).put(2, groupsizeZ);
            return this;
        }

        @Override
        public CLWork3D optimizeFor(CLDevice device) {
            super.optimizeFor(device);
            return this;
        }

    }


}
