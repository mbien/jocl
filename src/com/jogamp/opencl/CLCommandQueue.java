/*
 * Copyright 2009 - 2010 JogAmp Community. All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package com.jogamp.opencl;

import com.jogamp.common.nio.CachedBufferFactory;
import com.jogamp.opencl.llb.gl.CLGL;
import com.jogamp.common.nio.NativeSizeBuffer;
import com.jogamp.opencl.gl.CLGLObject;
import com.jogamp.opencl.llb.CLCommandQueueBinding;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static com.jogamp.opencl.CLException.*;
import static com.jogamp.opencl.llb.CL.*;
import static com.jogamp.opencl.util.CLUtil.*;

/**
 * The command queue is used to queue a set of operations for a specific {@link CLDevice}.
 * Having multiple command-queues allows applications to queue multiple independent commands without
 * requiring synchronization. Note that this should work as long as these objects are
 * not being shared.
 * <p>
 * concurrency note:<br/>
 * Sharing of objects across multiple queues or using a CLCommandQueue
 * form multiple Threads will require the application to perform appropriate synchronization.
 * </p>
 * @see CLDevice#createCommandQueue(com.jogamp.opencl.CLCommandQueue.Mode...)
 * @author Michael Bien
 */
public class CLCommandQueue extends CLObjectResource {

    private final CLCommandQueueBinding cl;
    private final CLDevice device;
    private long properties;

    /*
     * Those direct memory buffers are used to move data between the JVM and OpenCL.
     */
    private final IntBuffer pbA;
    private final NativeSizeBuffer ibA;
    private final NativeSizeBuffer ibB;
    private final NativeSizeBuffer ibC;

    private CLCommandQueue(CLContext context, long id, CLDevice device, long properties) {
        super(context, id);

        this.device = device;
        this.properties = properties;
        this.cl = context.getPlatform().getCommandQueueBinding();

        int pbsize = NativeSizeBuffer.elementSize();
        CachedBufferFactory factory = CachedBufferFactory.create(9*pbsize + 4, true);

        this.ibA = NativeSizeBuffer.wrap(factory.newDirectByteBuffer(3*pbsize));
        this.ibB = NativeSizeBuffer.wrap(factory.newDirectByteBuffer(3*pbsize));
        this.ibC = NativeSizeBuffer.wrap(factory.newDirectByteBuffer(3*pbsize));

        this.pbA = factory.newDirectIntBuffer(1);

    }

    static CLCommandQueue create(CLContext context, CLDevice device, long properties) {
        int[] status = new int[1];
        CLCommandQueueBinding binding = context.getPlatform().getCommandQueueBinding();
        long id = binding.clCreateCommandQueue(context.ID, device.ID, properties, status, 0);

        if(status[0] != CL_SUCCESS) {
            throw newException(status[0], "can not create command queue on " + device +" with properties: " + Mode.valuesOf(properties));
        }

        return new CLCommandQueue(context, id, device, properties);
    }

    /**
     * Calls {@native clEnqueueWriteBuffer}.
     */
    public CLCommandQueue putWriteBuffer(CLBuffer<?> writeBuffer, boolean blockingRead) {
        return putWriteBuffer(writeBuffer, blockingRead, null, null);
    }

    /**
     * Calls {@native clEnqueueWriteBuffer}.
     */
    public CLCommandQueue putWriteBuffer(CLBuffer<?> writeBuffer, boolean blockingRead, CLEventList events) {
        return putWriteBuffer(writeBuffer, blockingRead, null, events);
    }
    
    /**
     * Calls {@native clEnqueueWriteBuffer}.
     */
    public CLCommandQueue putWriteBuffer(CLBuffer<?> writeBuffer, boolean blockingWrite, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }
        
        int ret = cl.clEnqueueWriteBuffer(
                ID, writeBuffer.ID, clBoolean(blockingWrite),
                0, writeBuffer.getNIOSize(), writeBuffer.buffer,
                conditions, conditionIDs, events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue write-buffer: " + writeBuffer + " with " + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    /**
     * Calls {@native clEnqueueReadBuffer}.
     */
    public CLCommandQueue putReadBuffer(CLBuffer<?> readBuffer, boolean blockingRead) {
        putReadBuffer(readBuffer, blockingRead, null, null);
        return this;
    }

    /**
     * Calls {@native clEnqueueReadBuffer}.
     */
    public CLCommandQueue putReadBuffer(CLBuffer<?> readBuffer, boolean blockingRead, CLEventList events) {
        putReadBuffer(readBuffer, blockingRead, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueReadBuffer}.
     */
    public CLCommandQueue putReadBuffer(CLBuffer<?> readBuffer, boolean blockingRead, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }
        
        int ret = cl.clEnqueueReadBuffer(
                ID, readBuffer.ID, clBoolean(blockingRead),
                0, readBuffer.getNIOSize(), readBuffer.buffer,
                conditions, conditionIDs, events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue read-buffer: " + readBuffer + " with " + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    /**
     * Calls {@native clEnqueueCopyBuffer}.
     */
    public CLCommandQueue putCopyBuffer(CLBuffer<?> src, CLBuffer<?> dest) {
        return putCopyBuffer(src, dest, 0, 0, src.getCLSize(), null, null);
    }

    /**
     * Calls {@native clEnqueueCopyBuffer}.
     */
    public CLCommandQueue putCopyBuffer(CLBuffer<?> src, CLBuffer<?> dest, long bytesToCopy) {
        return putCopyBuffer(src, dest, 0, 0, bytesToCopy, null, null);
    }

    /**
     * Calls {@native clEnqueueCopyBuffer}.
     */
    public CLCommandQueue putCopyBuffer(CLBuffer<?> src, CLBuffer<?> dest, int srcOffset, int destOffset, long bytesToCopy, CLEventList events) {
        return putCopyBuffer(src, dest, 0, 0, bytesToCopy, null, events);
    }

    /**
     * Calls {@native clEnqueueCopyBuffer}.
     */
    public CLCommandQueue putCopyBuffer(CLBuffer<?> src, CLBuffer<?> dest, int srcOffset, int destOffset, long bytesToCopy, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        int ret = cl.clEnqueueCopyBuffer(
                        ID, src.ID, dest.ID, srcOffset, destOffset, bytesToCopy,
                        conditions, conditionIDs, events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue copy-buffer from " + src + " to " + dest + " with srcOffset: "+ srcOffset
                    + " dstOffset: " + destOffset + " bytesToCopy: " + bytesToCopy + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    //2D
    /**
     * Calls {@native clEnqueueWriteBufferRect}.
     */
    public CLCommandQueue putWriteBufferRect(CLBuffer<?> writeBuffer,
            int originX, int originY, int hostX, int hostY, int rangeX, int rangeY,
            boolean blockingWrite, CLEventList condition, CLEventList events) {
        putWriteBufferRect(writeBuffer, originX, originY, hostX, hostY, rangeX, rangeY, 0, 0, 0, 0, blockingWrite, condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueWriteBufferRect}.
     */
    public CLCommandQueue putWriteBufferRect(CLBuffer<?>  writeBuffer,
            int originX, int originY, int hostX, int hostY, int rangeX, int rangeY,
            long rowPitch, long slicePitch, long hostRowPitch, long hostSlicePitch,
            boolean blockingWrite, CLEventList condition, CLEventList events) {
        // spec: if 2d: origin/hostpos=0, range=1
        putWriteBufferRect( writeBuffer, originX, originY, 0,
                                         hostX, hostY, 0,
                                         rangeX, rangeY, 1,
                                         rowPitch, slicePitch, hostRowPitch, hostSlicePitch,
                                         blockingWrite, condition, events);
        return this;
    }

    //3D
    /**
     * Calls {@native clEnqueueWriteBufferRect}.
     */
    public CLCommandQueue putWriteBufferRect(CLBuffer<?> writeBuffer,
            int originX, int originY, int originZ, int hostX, int hostY, int hostZ, int rangeX, int rangeY, int rangeZ,
            boolean blockingWrite, CLEventList condition, CLEventList events) {
        putWriteBufferRect(writeBuffer, originX, originY, originZ,
                                        hostX, hostY, hostZ,
                                        rangeX, rangeY, rangeZ, 0, 0, 0, 0, blockingWrite, condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueWriteBufferRect}.
     */
    public CLCommandQueue putWriteBufferRect(CLBuffer<?> writeBuffer,
            int originX, int originY, int originZ, int hostX, int hostY, int hostZ, int rangeX, int rangeY, int rangeZ,
            long rowPitch, long slicePitch, long hostRowPitch, long hostSlicePitch,
            boolean blockingWrite, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        copy2NIO(ibA, originX, originY, originZ);
        copy2NIO(ibB, hostX, hostY, hostZ);
        copy2NIO(ibC, rangeX, rangeY, rangeZ);

        int ret = cl.clEnqueueWriteBufferRect(
                ID, writeBuffer.ID, clBoolean(blockingWrite), ibA, ibB, ibC,
                rowPitch, slicePitch, hostRowPitch, hostSlicePitch, writeBuffer.getBuffer(),
                conditions, conditionIDs, events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, bufferRectToString("write", writeBuffer,
                                        rowPitch, slicePitch, hostRowPitch, hostSlicePitch,
                                        originX, originY, originZ, hostX, hostY, hostZ,
                                        rangeX, rangeY, rangeZ, condition, events)  );
        }

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    //2D
    /**
     * Calls {@native clEnqueueReadBufferRect}.
     */
    public CLCommandQueue putReadBufferRect(CLBuffer<?> readBuffer,
            int originX, int originY, int hostX, int hostY, int rangeX, int rangeY,
            boolean blockingRead, CLEventList condition, CLEventList events) {
        putReadBufferRect(readBuffer, originX, originY, hostX, hostY, rangeX, rangeY, 0, 0, 0, 0, blockingRead, condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueReadBufferRect}.
     */
    public CLCommandQueue putReadBufferRect(CLBuffer<?> readBuffer,
            int originX, int originY, int hostX, int hostY, int rangeX, int rangeY,
            long rowPitch, long slicePitch, long hostRowPitch, long hostSlicePitch,
            boolean blockingRead, CLEventList condition, CLEventList events) {
        // spec: if 2d: origin/hostpos=0, range=1
        putReadBufferRect(  readBuffer, originX, originY, 0,
                            hostX, hostY, 0,
                            rangeX, rangeY, 1,
                            rowPitch, slicePitch, hostRowPitch, hostSlicePitch,
                            blockingRead, condition, events);
        return this;
    }

    //3D
    /**
     * Calls {@native clEnqueueReadBufferRect}.
     */
    public CLCommandQueue putReadBufferRect(CLBuffer<?> readBuffer,
            int originX, int originY, int originZ, int hostX, int hostY, int hostZ, int rangeX, int rangeY, int rangeZ,
            boolean blockingRead, CLEventList condition, CLEventList events) {
        putReadBufferRect(  readBuffer, originX, originY, originZ,
                            hostX, hostY, hostZ,
                            rangeX, rangeY, rangeZ,
                            0, 0, 0, 0, blockingRead, condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueReadBufferRect}.
     */
    public CLCommandQueue putReadBufferRect(CLBuffer<?> readBuffer,
            int originX, int originY, int originZ, int hostX, int hostY, int hostZ, int rangeX, int rangeY, int rangeZ,
            long rowPitch, long slicePitch, long hostRowPitch, long hostSlicePitch,
            boolean blockingRead, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        copy2NIO(ibA, originX, originY, originZ);
        copy2NIO(ibB, hostX, hostY, hostZ);
        copy2NIO(ibC, rangeX, rangeY, rangeZ);

        int ret = cl.clEnqueueReadBufferRect(
                ID, readBuffer.ID, clBoolean(blockingRead), ibA, ibB, ibC,
                rowPitch, slicePitch, hostRowPitch, hostSlicePitch, readBuffer.getBuffer(),
                conditions, conditionIDs, events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, bufferRectToString("read", readBuffer,
                                        rowPitch, slicePitch, hostRowPitch, hostSlicePitch,
                                        originX, originY, originZ, hostX, hostY, hostZ,
                                        rangeX, rangeY, rangeZ, condition, events)  );
        }

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    //2D
    /**
     * Calls {@native clEnqueueCopyBufferRect}.
     */
    public CLCommandQueue putCopyBufferRect(CLBuffer<?> src, CLBuffer<?> dest,
            int srcOriginX, int srcOriginY, int destOriginX, int destOriginY, int rangeX, int rangeY,
            CLEventList condition, CLEventList events) {
        // spec: if 2d: origin/destpos=0, range=1
        putCopyBufferRect(  src, dest, srcOriginX, srcOriginY, 0,
                            destOriginX, destOriginY, 0,
                            rangeX, rangeY, 1,
                            0, 0, 0, 0, condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueCopyBufferRect}.
     */
    public CLCommandQueue putCopyBufferRect(CLBuffer<?> src, CLBuffer<?> dest,
            int srcOriginX, int srcOriginY, int destOriginX, int destOriginY, int rangeX, int rangeY,
            long srcRowPitch, long srcSlicePitch, long destRowPitch, long destSlicePitch,
            CLEventList condition, CLEventList events) {
        putCopyBufferRect(  src, dest, srcOriginX, srcOriginY, 0,
                            destOriginX, destOriginY, 0,
                            rangeX, rangeY, 1,
                            srcRowPitch, srcSlicePitch, destSlicePitch, destRowPitch, condition, events);
        return this;
    }


    //3D
    /**
     * Calls {@native clEnqueueCopyBufferRect}.
     */
    public CLCommandQueue putCopyBufferRect(CLBuffer<?> src, CLBuffer<?> dest,
            int srcOriginX, int srcOriginY, int srcOriginZ, int destOriginX, int destOriginY, int destOriginZ, int rangeX, int rangeY, int rangeZ,
            CLEventList condition, CLEventList events) {
        putCopyBufferRect(  src, dest, srcOriginX, srcOriginY, srcOriginZ,
                            destOriginX, destOriginY, destOriginZ,
                            rangeX, rangeY, rangeZ,
                            0, 0, 0, 0, condition, events);
        return this;
    }
    /**
     * Calls {@native clEnqueueCopyBufferRect}.
     */
    public CLCommandQueue putCopyBufferRect(CLBuffer<?> src, CLBuffer<?> dest,
            int srcOriginX, int srcOriginY, int srcOriginZ, int destOriginX, int destOriginY, int destOriginZ, int rangeX, int rangeY, int rangeZ,
            long srcRowPitch, long srcSlicePitch, long destRowPitch, long destSlicePitch,
            CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        copy2NIO(ibA, srcOriginX, srcOriginY, srcOriginZ);
        copy2NIO(ibB, destOriginX, destOriginY, destOriginZ);
        copy2NIO(ibC, rangeX, rangeY, rangeZ);

        int ret = cl.clEnqueueCopyBufferRect(
                        ID, src.ID, dest.ID, ibA, ibB, ibC,
                        srcRowPitch, srcSlicePitch, destRowPitch, destSlicePitch,
                        conditions, conditionIDs, events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue copy-buffer-rect from " + src + " to " + dest + "\n"
                       + " with srcRowPitch: " + srcRowPitch + " srcSlicePitch: " + srcSlicePitch
                       + " destRowPitch: " + destRowPitch + " destSlicePitch: " + destSlicePitch + "\n"
                       + " srcOrigin: " + toStr(srcOriginX, srcOriginY, srcOriginZ)+ " destOrigin: " + toStr(destOriginX, destOriginY, destOriginZ)
                       + " range: " + toStr(rangeX, rangeY, rangeZ) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }


    //2D
    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(CLImage2d<?> writeImage, boolean blockingWrite) {
        return putWriteImage(writeImage, 0, 0, 0, writeImage.width, writeImage.height, blockingWrite, null, null);
    }

    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(CLImage2d<?> writeImage, boolean blockingWrite, CLEventList events) {
        return putWriteImage(writeImage, 0, 0, 0, writeImage.width, writeImage.height, blockingWrite, null, events);
    }

    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(CLImage2d<?> writeImage, boolean blockingWrite, CLEventList condition, CLEventList events) {
        return putWriteImage(writeImage, 0, 0, 0, writeImage.width, writeImage.height, blockingWrite, condition, events);
    }

    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(CLImage2d<?> writeImage, int inputRowPitch,
            int originX, int originY, int rangeX, int rangeY, boolean blockingWrite) {
        return putWriteImage(writeImage, inputRowPitch, originX, originY, rangeX, rangeY, blockingWrite, null, null);
    }

    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(CLImage2d<?> writeImage, int inputRowPitch,
            int originX, int originY, int rangeX, int rangeY, boolean blockingWrite, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        // spec: CL_INVALID_VALUE if image is a 2D image object and origin[2] is not equal to 0
        // or region[2] is not equal to 1 or slice_pitch is not equal to 0.
        copy2NIO(ibA, originX, originY, 0);
        copy2NIO(ibB, rangeX, rangeY, 1);

        int ret = cl.clEnqueueWriteImage(ID, writeImage.ID, clBoolean(blockingWrite),
                                         ibA, ibB, inputRowPitch, 0, writeImage.buffer,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue write-image " + writeImage + " with inputRowPitch: " + inputRowPitch
                       + " origin: " + toStr(originX, originY)+ " range: " + toStr(rangeX, rangeY) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //3D
    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(CLImage3d<?> writeImage, boolean blockingWrite) {
        return putWriteImage(writeImage, 0, 0, 0, 0, 0, writeImage.width, writeImage.height, writeImage.depth, blockingWrite, null, null);
    }

    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(CLImage3d<?> writeImage, boolean blockingWrite, CLEventList events) {
        return putWriteImage(writeImage, 0, 0, 0, 0, 0, writeImage.width, writeImage.height, writeImage.depth, blockingWrite, null, events);
    }

    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(CLImage3d<?> writeImage, boolean blockingWrite, CLEventList condition, CLEventList events) {
        return putWriteImage(writeImage, 0, 0, 0, 0, 0, writeImage.width, writeImage.height, writeImage.depth, blockingWrite, condition, events);
    }

    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(CLImage3d<?> writeImage, int inputRowPitch, int inputSlicePitch,
            int originX, int originY, int originZ, int rangeX, int rangeY, int rangeZ, boolean blockingWrite) {
        return putWriteImage(writeImage, inputRowPitch, inputSlicePitch, originX, originY, originZ, rangeX, rangeY, rangeZ, blockingWrite, null, null);
    }

    /**
     * Calls {@native clEnqueueWriteImage}.
     */
    public CLCommandQueue putWriteImage(CLImage3d<?> writeImage, int inputRowPitch, int inputSlicePitch,
            int originX, int originY, int originZ, int rangeX, int rangeY, int rangeZ, boolean blockingWrite, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        copy2NIO(ibA, originX, originY, originZ);
        copy2NIO(ibB, rangeX, rangeY, rangeZ);

        int ret = cl.clEnqueueWriteImage(ID, writeImage.ID, clBoolean(blockingWrite),
                                         ibA, ibB, inputRowPitch, inputSlicePitch, writeImage.buffer,
                                         conditions, conditionIDs, events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue write-image " + writeImage + " with inputRowPitch: " + inputRowPitch + " inputSlicePitch: " + inputSlicePitch
                       + " origin: " + toStr(originX, originY, originZ)+ " range: " + toStr(rangeX, rangeY, rangeZ) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //2D
    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(CLImage2d<?> readImage, boolean blockingRead) {
        return putReadImage(readImage, 0, 0, 0, readImage.width, readImage.height, blockingRead, null, null);
    }

    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(CLImage2d<?> readImage, boolean blockingRead, CLEventList events) {
        return putReadImage(readImage, 0, 0, 0, readImage.width, readImage.height, blockingRead, null, events);
    }

    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(CLImage2d<?> readImage, boolean blockingRead, CLEventList condition, CLEventList events) {
        return putReadImage(readImage, 0, 0, 0, readImage.width, readImage.height, blockingRead, condition, events);
    }

    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(CLImage2d<?> readImage, int inputRowPitch,
            int originX, int originY, int rangeX, int rangeY, boolean blockingRead) {
        return putReadImage(readImage, inputRowPitch, originX, originY, rangeX, rangeY, blockingRead, null, null);
    }

    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(CLImage2d<?> readImage, int inputRowPitch,
            int originX, int originY, int rangeX, int rangeY, boolean blockingRead, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        // spec: CL_INVALID_VALUE if image is a 2D image object and origin[2] is not equal to 0
        // or region[2] is not equal to 1 or slice_pitch is not equal to 0.
        copy2NIO(ibA, originX, originY, 0);
        copy2NIO(ibB, rangeX, rangeY, 1);

        int ret = cl.clEnqueueReadImage(ID, readImage.ID, clBoolean(blockingRead),
                                         ibA, ibB, inputRowPitch, 0, readImage.buffer,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue read-image " + readImage + " with inputRowPitch: " + inputRowPitch
                       + " origin: " + toStr(originX, originY)+ " range: " + toStr(rangeX, rangeY) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //3D
    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(CLImage3d<?> readImage, boolean blockingRead) {
        return putReadImage(readImage, 0, 0, 0, 0, 0, readImage.width, readImage.height, readImage.depth, blockingRead, null, null);
    }

    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(CLImage3d<?> readImage, boolean blockingRead, CLEventList events) {
        return putReadImage(readImage, 0, 0, 0, 0, 0, readImage.width, readImage.height, readImage.depth, blockingRead, null, events);
    }

    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(CLImage3d<?> readImage, boolean blockingRead, CLEventList condition, CLEventList events) {
        return putReadImage(readImage, 0, 0, 0, 0, 0, readImage.width, readImage.height, readImage.depth, blockingRead, condition, events);
    }

    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(CLImage3d<?> readImage, int inputRowPitch, int inputSlicePitch,
            int originX, int originY, int originZ, int rangeX, int rangeY, int rangeZ, boolean blockingRead) {
        return putReadImage(readImage, inputRowPitch, inputSlicePitch, originX, originY, originZ, rangeX, rangeY, rangeZ, blockingRead, null, null);
    }

    /**
     * Calls {@native clEnqueueReadImage}.
     */
    public CLCommandQueue putReadImage(CLImage3d<?> readImage, int inputRowPitch, int inputSlicePitch,
            int originX, int originY, int originZ, int rangeX, int rangeY, int rangeZ, boolean blockingRead, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        copy2NIO(ibA, originX, originY, originZ);
        copy2NIO(ibB, rangeX, rangeY, rangeZ);

        int ret = cl.clEnqueueReadImage(ID, readImage.ID, clBoolean(blockingRead),
                                        ibA, ibB, inputRowPitch, inputSlicePitch, readImage.buffer,
                                        conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue read-image " + readImage + " with inputRowPitch: " + inputRowPitch + " inputSlicePitch: " + inputSlicePitch
                       + " origin: " + toStr(originX, originY, originZ)+ " range: " + toStr(rangeX, rangeY, rangeZ) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //2D
    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(CLImage2d<?> srcImage, CLImage2d<?> dstImage) {
        return putCopyImage(srcImage, dstImage, null);
    }

    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(CLImage2d<?> srcImage, CLImage2d<?> dstImage, CLEventList events) {
        return putCopyImage(srcImage, dstImage, 0, 0, 0, 0, srcImage.width, srcImage.height, null, events);
    }

    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(CLImage2d<?> srcImage, CLImage2d<?> dstImage, CLEventList condition, CLEventList events) {
        return putCopyImage(srcImage, dstImage, 0, 0, 0, 0, srcImage.width, srcImage.height, condition, events);
    }

    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(CLImage2d<?> srcImage, CLImage2d<?> dstImage,
                                        int srcOriginX, int srcOriginY,
                                        int dstOriginX, int dstOriginY,
                                        int rangeX, int rangeY) {
        return putCopyImage(srcImage, dstImage, srcOriginX, srcOriginY, dstOriginX, dstOriginY, rangeX, rangeY, null, null);
    }

    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(CLImage2d<?> srcImage, CLImage2d<?> dstImage,
                                        int srcOriginX, int srcOriginY,
                                        int dstOriginX, int dstOriginY,
                                        int rangeX, int rangeY, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        //spec: CL_INVALID_VALUE if src_image is a 2D image object and origin[2] or dst_origin[2] is not equal to 0
        // or region[2] is not equal to 1.
        copy2NIO(ibA, srcOriginX, srcOriginY, 0);
        copy2NIO(ibB, dstOriginX, dstOriginY, 0);
        copy2NIO(ibC, rangeX, rangeY, 1);

        int ret = cl.clEnqueueCopyImage(ID, srcImage.ID, dstImage.ID, ibA, ibB, ibC,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue copy-image " + srcImage +" to "+ dstImage
                    + " with srcOrigin: " + toStr(srcOriginX, srcOriginY) + " dstOrigin: " + toStr(dstOriginX, dstOriginY)
                    + " range:  " + toStr(rangeX, rangeY) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //3D
    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(CLImage3d<?> srcImage, CLImage3d<?> dstImage) {
        return putCopyImage(srcImage, dstImage, null);
    }

    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(CLImage3d<?> srcImage, CLImage3d<?> dstImage, CLEventList events) {
        return putCopyImage(srcImage, dstImage, 0, 0, 0, 0, 0, 0, srcImage.width, srcImage.height, srcImage.depth, null, events);
    }

    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(CLImage3d<?> srcImage, CLImage3d<?> dstImage, CLEventList condition, CLEventList events) {
        return putCopyImage(srcImage, dstImage, 0, 0, 0, 0, 0, 0, srcImage.width, srcImage.height, srcImage.depth, condition, events);
    }

    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(CLImage3d<?> srcImage, CLImage3d<?> dstImage,
                                        int srcOriginX, int srcOriginY, int srcOriginZ,
                                        int dstOriginX, int dstOriginY, int dstOriginZ,
                                        int rangeX, int rangeY, int rangeZ) {
        return putCopyImage(srcImage, dstImage, srcOriginX, srcOriginY, srcOriginZ,
                                                dstOriginX, dstOriginY, dstOriginZ,
                                                rangeX, rangeY, rangeZ, null, null);
    }

    /**
     * Calls {@native clEnqueueCopyImage}.
     */
    public CLCommandQueue putCopyImage(CLImage<?> srcImage, CLImage<?> dstImage,
                                        int srcOriginX, int srcOriginY, int srcOriginZ,
                                        int dstOriginX, int dstOriginY, int dstOriginZ,
                                        int rangeX, int rangeY, int rangeZ, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        copy2NIO(ibA, srcOriginX, srcOriginY, srcOriginZ);
        copy2NIO(ibB, dstOriginX, dstOriginY, dstOriginZ);
        copy2NIO(ibC, rangeX, rangeY, rangeZ);

        int ret = cl.clEnqueueCopyImage(ID, srcImage.ID, dstImage.ID, ibA, ibB, ibC,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue copy-image " + srcImage +" to "+ dstImage
                    + " with srcOrigin: " + toStr(srcOriginX, srcOriginY, srcOriginZ) + " dstOrigin: " + toStr(dstOriginX, dstOriginY, dstOriginZ)
                    + " range:  " + toStr(rangeX, rangeY, rangeZ) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }
    
    //2D
    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(CLBuffer<?> srcBuffer, CLImage2d<?> dstImage) {
        return putCopyBufferToImage(srcBuffer, dstImage, null);
    }
    
    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(CLBuffer<?> srcBuffer, CLImage2d<?> dstImage, CLEventList events) {
        return putCopyBufferToImage(srcBuffer, dstImage, 0, 0, 0, dstImage.width, dstImage.height, null, events);
    }

    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(CLBuffer<?> srcBuffer, CLImage2d<?> dstImage, CLEventList condition, CLEventList events) {
        return putCopyBufferToImage(srcBuffer, dstImage, 0, 0, 0, dstImage.width, dstImage.height, condition, events);
    }
        
    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(CLBuffer<?> srcBuffer, CLImage2d<?> dstImage,
                                        long srcOffset, int dstOriginX, int dstOriginY,
                                        int rangeX, int rangeY) {
        return putCopyBufferToImage(srcBuffer, dstImage, 
                srcOffset, dstOriginX, dstOriginY, rangeX, rangeY, null, null);
    }
    
    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(CLBuffer<?> srcBuffer, CLImage2d<?> dstImage,
                                        long srcOffset, int dstOriginX, int dstOriginY,
                                        int rangeX, int rangeY, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        // spec: CL_INVALID_VALUE if dst_image is a 2D image object and dst_origin[2] is not equal to 0
        // or region[2] is not equal to 1.
        copy2NIO(ibA, dstOriginX, dstOriginY, 0);
        copy2NIO(ibB, rangeX, rangeY, 1);

        int ret = cl.clEnqueueCopyBufferToImage(ID, srcBuffer.ID, dstImage.ID,
                                         srcOffset, ibA, ibB,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue a copy from " + srcBuffer +" to "+ dstImage
                    + " with srcOffset: " + srcOffset + " dstOrigin: " + toStr(dstOriginX, dstOriginY)
                    + " range:  " + toStr(rangeX, rangeY) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }
    
    //3D
    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(CLBuffer<?> srcBuffer, CLImage3d<?> dstImage) {
        return putCopyBufferToImage(srcBuffer, dstImage, null);
    }
    
    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(CLBuffer<?> srcBuffer, CLImage3d<?> dstImage, CLEventList events) {
        return putCopyBufferToImage(srcBuffer, dstImage, 0, 0, 0, 0, dstImage.width, dstImage.height, dstImage.depth, null, events);
    }

    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(CLBuffer<?> srcBuffer, CLImage3d<?> dstImage, CLEventList condition, CLEventList events) {
        return putCopyBufferToImage(srcBuffer, dstImage, 0, 0, 0, 0, dstImage.width, dstImage.height, dstImage.depth, condition, events);
    }
        
    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(CLBuffer<?> srcBuffer, CLImage3d<?> dstImage,
                                        long srcOffset, int dstOriginX, int dstOriginY, int dstOriginZ,
                                        int rangeX, int rangeY, int rangeZ) {
        return putCopyBufferToImage(srcBuffer, dstImage, 
                srcOffset, dstOriginX, dstOriginY, dstOriginZ, rangeX, rangeY, rangeZ, null, null);
        
    }
    
    /**
     * Calls {@native clEnqueueCopyBufferToImage}.
     */
    public CLCommandQueue putCopyBufferToImage(CLBuffer<?> srcBuffer, CLImage3d<?> dstImage,
                                        long srcOffset, int dstOriginX, int dstOriginY, int dstOriginZ,
                                        int rangeX, int rangeY, int rangeZ, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        copy2NIO(ibA, dstOriginX, dstOriginY, dstOriginZ);
        copy2NIO(ibB, rangeX, rangeY, rangeZ);

        int ret = cl.clEnqueueCopyBufferToImage(ID, srcBuffer.ID, dstImage.ID,
                                         srcOffset, ibA, ibB,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue a copy from " + srcBuffer +" to "+ dstImage
                    + " with srcOffset: " + srcOffset + " dstOrigin: " + toStr(dstOriginX, dstOriginY, dstOriginZ)
                    + " range:  " + toStr(rangeX, rangeY, dstOriginZ) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //2D
    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(CLImage2d<?> srcImage, CLBuffer<?> dstBuffer) {
        return putCopyImageToBuffer(srcImage, dstBuffer, null);
    }
    
    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(CLImage2d<?> srcImage, CLBuffer<?> dstBuffer, CLEventList events) {
        return putCopyImageToBuffer(srcImage, dstBuffer, 0, 0, srcImage.width, srcImage.height, 0, null, events);
    }

    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(CLImage2d<?> srcImage, CLBuffer<?> dstBuffer, CLEventList condition, CLEventList events) {
        return putCopyImageToBuffer(srcImage, dstBuffer, 0, 0, srcImage.width, srcImage.height, 0, condition, events);
    }
        
    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(CLImage2d<?> srcImage, CLBuffer<?> dstBuffer,
                                        int srcOriginX, int srcOriginY,
                                        int rangeX, int rangeY, long dstOffset) {
        return putCopyImageToBuffer(srcImage, dstBuffer, 
                srcOriginX, srcOriginY, rangeX, rangeY, dstOffset, null, null);
    }
    
    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(CLImage2d<?> srcImage, CLBuffer<?> dstBuffer,
                                        int srcOriginX, int srcOriginY,
                                        int rangeX, int rangeY, long dstOffset, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        // spec: CL_INVALID_VALUE if src_image is a 2D image object and src_origin[2] is not equal to 0
        // or region[2] is not equal to 1.
        copy2NIO(ibA, srcOriginX, srcOriginY, 0);
        copy2NIO(ibB, rangeX, rangeY, 1);

        int ret = cl.clEnqueueCopyImageToBuffer(ID, srcImage.ID, dstBuffer.ID,
                                         ibA, ibB, dstOffset,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue a copy from " + srcImage +" to "+ dstBuffer
                    + " with srcOrigin: " + toStr(srcOriginX, srcOriginY) + " range: " + toStr(rangeX, rangeY)
                    + " dstOffset: " + dstOffset + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }
    
    //3D
    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(CLImage3d<?> srcImage, CLBuffer<?> dstBuffer) {
        return putCopyImageToBuffer(srcImage, dstBuffer, 0, 0, 0, srcImage.width, srcImage.height, srcImage.depth, 0, null, null);
    }
    
    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(CLImage3d<?> srcImage, CLBuffer<?> dstBuffer, CLEventList events) {
        return putCopyImageToBuffer(srcImage, dstBuffer, 0, 0, 0, srcImage.width, srcImage.height, srcImage.depth, 0, null, events);
    }

    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(CLImage3d<?> srcImage, CLBuffer<?> dstBuffer, CLEventList condition, CLEventList events) {
        return putCopyImageToBuffer(srcImage, dstBuffer, 0, 0, 0, srcImage.width, srcImage.height, srcImage.depth, 0, condition, events);
    }
        
    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(CLImage3d<?> srcImage, CLBuffer<?> dstBuffer,
                                        int srcOriginX, int srcOriginY, int srcOriginZ,
                                        int rangeX, int rangeY, int rangeZ, long dstOffset) {
        return putCopyImageToBuffer(srcImage, dstBuffer, 
                srcOriginX, srcOriginY, srcOriginZ, rangeX, rangeY, rangeZ, dstOffset, null, null);
        
    }
    
    /**
     * Calls {@native clEnqueueCopyImageToBuffer}.
     */
    public CLCommandQueue putCopyImageToBuffer(CLImage3d<?> srcImage, CLBuffer<?> dstBuffer,
                                        int srcOriginX, int srcOriginY, int srcOriginZ, 
                                        int rangeX, int rangeY, int rangeZ, long dstOffset, CLEventList condition, CLEventList events) {
        
        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        copy2NIO(ibA, srcOriginX, srcOriginY, srcOriginZ);
        copy2NIO(ibB, rangeX, rangeY, rangeZ);

        int ret = cl.clEnqueueCopyImageToBuffer(ID, srcImage.ID, dstBuffer.ID,
                                         ibA, ibB, dstOffset,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue a copy from " + srcImage +" to "+ dstBuffer
                    + " with srcOrigin: " + toStr(srcOriginX, srcOriginY, srcOriginZ) + " range: " + toStr(rangeX, rangeY, rangeZ)
                    + " dstOffset: " + dstOffset + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    /**
     * Calls {@native clEnqueueMapBuffer}.
     */
    public ByteBuffer putMapBuffer(CLBuffer<?> buffer, CLMemory.Map flag, boolean blockingMap) {
        return putMapBuffer(buffer, flag, blockingMap, null);
    }

    /**
     * Calls {@native clEnqueueMapBuffer}.
     */
    public ByteBuffer putMapBuffer(CLBuffer<?> buffer, CLMemory.Map flag, boolean blockingMap, CLEventList events) {
        return putMapBuffer(buffer, flag, 0, buffer.getCLSize(), blockingMap, null, events);
    }

    /**
     * Calls {@native clEnqueueMapBuffer}.
     */
    public ByteBuffer putMapBuffer(CLBuffer<?> buffer, CLMemory.Map flag, boolean blockingMap, CLEventList condition, CLEventList events) {
        return putMapBuffer(buffer, flag, 0, buffer.getCLSize(), blockingMap, condition, events);
    }

    /**
     * Calls {@native clEnqueueMapBuffer}.
     */
    public ByteBuffer putMapBuffer(CLBuffer<?> buffer, CLMemory.Map flag, long offset, long length, boolean blockingMap) {
        return putMapBuffer(buffer, flag, offset, length, blockingMap, null, null);
    }

    /**
     * Calls {@native clEnqueueMapBuffer}.
     */
    public ByteBuffer putMapBuffer(CLBuffer<?> buffer, CLMemory.Map flag, long offset, long length, boolean blockingMap, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        IntBuffer error = pbA;
        ByteBuffer mappedBuffer = cl.clEnqueueMapBuffer(ID, buffer.ID, clBoolean(blockingMap),
                                         flag.FLAGS, offset, length,
                                         conditions, conditionIDs, events==null ? null : events.IDs, error);
        if(error.get(0) != CL_SUCCESS) {
            throw newException(error.get(0), "can not map " + buffer + " with: " + flag
                    + " offset: " + offset + " lenght: " + length + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }

        return mappedBuffer;
    }

    // 2D
    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(CLImage2d<?> image, CLMemory.Map flag, boolean blockingMap) {
        return putMapImage(image, flag, blockingMap, null);
    }

    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(CLImage2d<?> image, CLMemory.Map flag, boolean blockingMap, CLEventList events) {
        return putMapImage(image, flag, 0, 0, image.width, image.height, blockingMap, null, events);
    }

    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(CLImage2d<?> image, CLMemory.Map flag, boolean blockingMap, CLEventList condition, CLEventList events) {
        return putMapImage(image, flag, 0, 0, image.width, image.height, blockingMap, condition, events);
    }

    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(CLImage2d<?> buffer, CLMemory.Map flag, int offsetX, int offsetY,
                                    int rangeX, int rangeY, boolean blockingMap) {
        return putMapImage(buffer, flag, offsetX, offsetY, rangeX, rangeY, blockingMap, null, null);
    }

    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(CLImage2d<?> image, CLMemory.Map flag,
                                    int offsetX, int offsetY,
                                    int rangeX, int rangeY, boolean blockingMap, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        IntBuffer error = pbA;

        // spec: CL_INVALID_VALUE if image is a 2D image object and origin[2] is not equal to 0 or region[2] is not equal to 1
        copy2NIO(ibB, offsetX, offsetY, 0);
        copy2NIO(ibC, rangeX, rangeY, 1);

        ByteBuffer mappedImage = cl.clEnqueueMapImage(ID, image.ID, clBoolean(blockingMap),
                                         flag.FLAGS, ibB, ibC, null, null,
                                         conditions, conditionIDs, events==null ? null : events.IDs, error);
        if(error.get(0) != CL_SUCCESS) {
            throw newException(error.get(0), "can not map " + image + " with: " + flag
                    + " offset: " + toStr(offsetX, offsetY) + " range: " + toStr(rangeX, rangeY) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }

        return mappedImage;
    }

    // 3D
    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(CLImage3d<?> image, CLMemory.Map flag, boolean blockingMap) {
        return putMapImage(image, flag, blockingMap, null);
    }

    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(CLImage3d<?> image, CLMemory.Map flag, boolean blockingMap, CLEventList events) {
        return putMapImage(image, flag, 0, 0, 0, image.width, image.height, image.depth, blockingMap, null, events);
    }

    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(CLImage3d<?> image, CLMemory.Map flag, boolean blockingMap, CLEventList condition, CLEventList events) {
        return putMapImage(image, flag, 0, 0, 0, image.width, image.height, image.depth, blockingMap, condition, events);
    }

    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(CLImage3d<?> image, CLMemory.Map flag,
                                    int offsetX, int offsetY, int offsetZ,
                                    int rangeX, int rangeY, int rangeZ, boolean blockingMap) {
        return putMapImage(image, flag, offsetX, offsetY, offsetZ, rangeX, rangeY, rangeZ, blockingMap, null, null);
    }

    /**
     * Calls {@native clEnqueueMapImage}.
     */
    public ByteBuffer putMapImage(CLImage3d<?> image, CLMemory.Map flag,
                                    int offsetX, int offsetY, int offsetZ,
                                    int rangeX, int rangeY, int rangeZ, boolean blockingMap, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        IntBuffer error = pbA;
        copy2NIO(ibB, offsetX, offsetY, offsetZ);
        copy2NIO(ibC, rangeX, rangeY, rangeZ);
        ByteBuffer mappedImage = cl.clEnqueueMapImage(ID, image.ID, clBoolean(blockingMap),
                                         flag.FLAGS, ibB, ibC, null, null,
                                         conditions, conditionIDs, events==null ? null : events.IDs, error);
        if(error.get(0) != CL_SUCCESS) {
            throw newException(error.get(0), "can not map " + image + " with: " + flag
                    + " offset: " + toStr(offsetX, offsetY, offsetZ) + " range: " + toStr(rangeX, rangeY, rangeZ) + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }

        return mappedImage;
    }

    /**
     * Calls {@native clEnqueueUnmapMemObject}.
     */
    public CLCommandQueue putUnmapMemory(CLMemory<?> memory, Buffer mapped) {
        return putUnmapMemory(memory, mapped, null, null);
    }

    /**
     * Calls {@native clEnqueueUnmapMemObject}.
     */
    public CLCommandQueue putUnmapMemory(CLMemory<?> memory, Buffer mapped, CLEventList events) {
        return putUnmapMemory(memory, mapped, null, events);
    }

    /**
     * Calls {@native clEnqueueUnmapMemObject}.
     */
    public CLCommandQueue putUnmapMemory(CLMemory<?> memory, Buffer mapped, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        int ret = cl.clEnqueueUnmapMemObject(ID, memory.ID, mapped,
                                        conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not unmap " + memory + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    /**
     * Calls {@native clEnqueueMarker}.
     */
    public CLCommandQueue putMarker(CLEventList events) {
        int ret = cl.clEnqueueMarker(ID, events.IDs);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue marker " + events);
        }
        events.createEvent(context);
        return this;
    }

    /**
     * Calls {@native clWaitForEvents} if blockingWait equals true otherwise {@native clEnqueueWaitForEvents}.
     */
    public CLCommandQueue putWaitForEvent(CLEventList list, int index, boolean blockingWait) {

        if(blockingWait) {
            list.waitForEvent(index);
        }else{
            NativeSizeBuffer ids = list.getEventBuffer(index);
            int ret = cl.clEnqueueWaitForEvents(ID, 1, ids);
            if(ret != CL_SUCCESS) {
                throw newException(ret, "can not "+ (blockingWait?"blocking": "") +" wait for event #" + index+ " in "+list);
            }
        }

        return this;
    }

    /**
     * Calls {@native clWaitForEvents} if blockingWait equals true otherwise {@native clEnqueueWaitForEvents}.
     */
    public CLCommandQueue putWaitForEvents(CLEventList list, boolean blockingWait) {
        if(blockingWait) {
            list.waitForEvents();
        }else{
            int ret = cl.clEnqueueWaitForEvents(ID, list.size, list.IDsView);
            if(ret != CL_SUCCESS) {
                throw newException(ret, "can not "+ (blockingWait?"blocking": "") +" wait for events " + list);
            }
        }
        return this;
    }

    /**
     * Calls {@native clEnqueueBarrier}.
     */
    public CLCommandQueue putBarrier() {
        int ret = cl.clEnqueueBarrier(ID);
        checkForError(ret, "can not enqueue Barrier");
        return this;
    }

    /**
     * Equivalent to calling
     * {@link #put1DRangeKernel(CLKernel kernel, long globalWorkOffset, long globalWorkSize, long localWorkSize)}
     * with globalWorkOffset = null, globalWorkSize set to 1, and localWorkSize set to 1.
     * <p>Calls {@native clEnqueueTask}.</p>
     */
    public CLCommandQueue putTask(CLKernel kernel) {
        putTask(kernel, null, null);
        return this;
    }

    /**
     * <p>Calls {@native clEnqueueTask}.</p>
     * @see #putTask(com.jogamp.opencl.CLKernel)
     */
    public CLCommandQueue putTask(CLKernel kernel, CLEventList events) {
        putTask(kernel, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueTask}.
     * @see #putTask(com.jogamp.opencl.CLKernel)
     */
    public CLCommandQueue putTask(CLKernel kernel, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        int ret = cl.clEnqueueTask(ID, kernel.ID, conditions, conditionIDs, events==null ? null : events.IDs);
        if(ret != CL_SUCCESS) {
            checkForError(ret, "can not enqueue Task: " + kernel + toStr(condition, events));
        }
        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue put1DRangeKernel(CLKernel kernel, long globalWorkOffset, long globalWorkSize, long localWorkSize) {
        this.put1DRangeKernel(kernel, globalWorkOffset, globalWorkSize, localWorkSize, null, null);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue put1DRangeKernel(CLKernel kernel, long globalWorkOffset, long globalWorkSize, long localWorkSize, CLEventList events) {
        this.put1DRangeKernel(kernel, globalWorkOffset, globalWorkSize, localWorkSize, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue put1DRangeKernel(CLKernel kernel, long globalWorkOffset, long globalWorkSize, long localWorkSize, CLEventList condition, CLEventList events) {
        NativeSizeBuffer globWO = null;
        NativeSizeBuffer globWS = null;
        NativeSizeBuffer locWS = null;

        if(globalWorkOffset != 0) {
            globWO = copy2NIO(ibA, globalWorkOffset);
        }
        if(globalWorkSize != 0) {
            globWS = copy2NIO(ibB, globalWorkSize);
        }
        if(localWorkSize != 0) {
            locWS = copy2NIO(ibC, localWorkSize);
        }

        this.putNDRangeKernel(kernel, 1, globWO, globWS, locWS, condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue put2DRangeKernel(CLKernel kernel, long globalWorkOffsetX, long globalWorkOffsetY,
                                                            long globalWorkSizeX, long globalWorkSizeY,
                                                            long localWorkSizeX, long localWorkSizeY) {
        this.put2DRangeKernel(kernel,
                globalWorkOffsetX, globalWorkOffsetY,
                globalWorkSizeX, globalWorkSizeY,
                localWorkSizeX, localWorkSizeY, null, null);

        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue put2DRangeKernel(CLKernel kernel, long globalWorkOffsetX, long globalWorkOffsetY,
                                                            long globalWorkSizeX, long globalWorkSizeY,
                                                            long localWorkSizeX, long localWorkSizeY, CLEventList events) {
        this.put2DRangeKernel(kernel,
                globalWorkOffsetX, globalWorkOffsetY,
                globalWorkSizeX, globalWorkSizeY,
                localWorkSizeX, localWorkSizeY, null, events);
        return this;
    }
    
    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue put2DRangeKernel(CLKernel kernel, long globalWorkOffsetX, long globalWorkOffsetY,
                                                            long globalWorkSizeX, long globalWorkSizeY,
                                                            long localWorkSizeX, long localWorkSizeY, CLEventList condition, CLEventList events) {
        NativeSizeBuffer globalWorkOffset = null;
        NativeSizeBuffer globalWorkSize = null;
        NativeSizeBuffer localWorkSize = null;

        if(globalWorkOffsetX != 0 || globalWorkOffsetY != 0) {
            globalWorkOffset = copy2NIO(ibA, globalWorkOffsetX, globalWorkOffsetY);
        }
        if(globalWorkSizeX != 0 || globalWorkSizeY != 0) {
            globalWorkSize = copy2NIO(ibB, globalWorkSizeX, globalWorkSizeY);
        }
        if(localWorkSizeX != 0 || localWorkSizeY != 0) {
            localWorkSize = copy2NIO(ibC, localWorkSizeX, localWorkSizeY);
        }
        this.putNDRangeKernel(kernel, 2, globalWorkOffset, globalWorkSize, localWorkSize, condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue put3DRangeKernel(CLKernel kernel, long globalWorkOffsetX, long globalWorkOffsetY, long globalWorkOffsetZ,
                                                            long globalWorkSizeX, long globalWorkSizeY, long globalWorkSizeZ,
                                                            long localWorkSizeX, long localWorkSizeY, long localWorkSizeZ) {
        this.put3DRangeKernel(kernel,
                globalWorkOffsetX, globalWorkOffsetY, globalWorkOffsetZ,
                globalWorkSizeX, globalWorkSizeY, globalWorkSizeZ,
                localWorkSizeX, localWorkSizeY, localWorkSizeZ, null, null);

        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue put3DRangeKernel(CLKernel kernel, long globalWorkOffsetX, long globalWorkOffsetY, long globalWorkOffsetZ,
                                                            long globalWorkSizeX, long globalWorkSizeY, long globalWorkSizeZ,
                                                            long localWorkSizeX, long localWorkSizeY, long localWorkSizeZ, CLEventList events) {
        this.put3DRangeKernel(kernel,
                globalWorkOffsetX, globalWorkOffsetY, globalWorkOffsetZ,
                globalWorkSizeX, globalWorkSizeY, globalWorkSizeZ,
                localWorkSizeX, localWorkSizeY, localWorkSizeZ, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue put3DRangeKernel(CLKernel kernel, long globalWorkOffsetX, long globalWorkOffsetY, long globalWorkOffsetZ,
                                                            long globalWorkSizeX, long globalWorkSizeY, long globalWorkSizeZ,
                                                            long localWorkSizeX, long localWorkSizeY, long localWorkSizeZ, CLEventList condition, CLEventList events) {
        NativeSizeBuffer globalWorkOffset = null;
        NativeSizeBuffer globalWorkSize = null;
        NativeSizeBuffer localWorkSize = null;

        if(globalWorkOffsetX != 0 || globalWorkOffsetY != 0 || globalWorkOffsetZ != 0) {
            globalWorkOffset = copy2NIO(ibA, globalWorkOffsetX, globalWorkOffsetY, globalWorkOffsetZ);
        }
        if(globalWorkSizeX != 0 || globalWorkSizeY != 0 || globalWorkSizeZ != 0) {
            globalWorkSize = copy2NIO(ibB, globalWorkSizeX, globalWorkSizeY, globalWorkSizeZ);
        }
        if(localWorkSizeX != 0 || localWorkSizeY != 0 || localWorkSizeZ != 0) {
            localWorkSize = copy2NIO(ibC, localWorkSizeX, localWorkSizeY, localWorkSizeZ);
        }
        this.putNDRangeKernel(kernel, 3, globalWorkOffset, globalWorkSize, localWorkSize, condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue putNDRangeKernel(CLKernel kernel, int workDimension, NativeSizeBuffer globalWorkOffset, NativeSizeBuffer globalWorkSize, NativeSizeBuffer localWorkSize) {
        this.putNDRangeKernel(kernel, workDimension, globalWorkOffset, globalWorkSize, localWorkSize, null, null);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue putNDRangeKernel(CLKernel kernel, int workDimension, NativeSizeBuffer globalWorkOffset, NativeSizeBuffer globalWorkSize, NativeSizeBuffer localWorkSize, CLEventList events) {
        this.putNDRangeKernel(kernel, workDimension, globalWorkOffset, globalWorkSize, localWorkSize, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue putNDRangeKernel(CLKernel kernel, int workDimension, NativeSizeBuffer globalWorkOffset,
            NativeSizeBuffer globalWorkSize, NativeSizeBuffer localWorkSize, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        int ret = cl.clEnqueueNDRangeKernel(
                ID, kernel.ID, workDimension,
                globalWorkOffset,
                globalWorkSize, 
                localWorkSize, 
                conditions, conditionIDs,
                events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not enqueue "+workDimension+"DRange " + kernel+ "\n"
                    + " with gwo: " + toStr(globalWorkOffset)
                    + " gws: " + toStr(globalWorkSize)
                    + " lws: " + toStr(localWorkSize)
                    + " " + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue putWork(CLWork work) {
        this.putWork(work, null, null);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue putWork(CLWork work, CLEventList events) {
        this.putWork(work, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue putWork(CLWork work, CLEventList condition, CLEventList events) {
        NativeSizeBuffer groupSize = null;
        if(                        work.getGroupSize().get(0) != 0
         || work.dimension >= 2 && work.getGroupSize().get(1) != 0
         || work.dimension == 3 && work.getGroupSize().get(2) != 0) {
            groupSize = work.getGroupSize();
        }

        this.putNDRangeKernel(work.getKernel(), work.dimension, work.getWorkOffset(), work.getWorkSize(), groupSize, condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueAcquireGLObjects}.
     */
    public CLCommandQueue putAcquireGLObject(CLGLObject glObject) {
        this.putAcquireGLObject(glObject, null, null);
        return this;
    }

    /**
     * Calls {@native clEnqueueAcquireGLObjects}.
     */
    public CLCommandQueue putAcquireGLObject(CLGLObject glObject, CLEventList events) {
        this.putAcquireGLObject(glObject, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueAcquireGLObjects}.
     */
    public CLCommandQueue putAcquireGLObject(CLGLObject glObject, CLEventList condition, CLEventList events) {
        this.putAcquireGLObjects(copy2NIO(ibA, glObject.getID()), condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueAcquireGLObjects}.
     */
    public CLCommandQueue putAcquireGLObjects(CLGLObject glObject1, CLGLObject glObject2, CLEventList condition, CLEventList events) {
        this.putAcquireGLObjects(copy2NIO(ibA, glObject1.getID(), glObject2.getID()), condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueAcquireGLObjects}.
     */
    public CLCommandQueue putAcquireGLObjects(CLGLObject glObject1, CLGLObject glObject2, CLGLObject glObject3, CLEventList condition, CLEventList events) {
        this.putAcquireGLObjects(copy2NIO(ibA, glObject1.getID(), glObject2.getID(), glObject3.getID()), condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueAcquireGLObjects}.
     */
    public CLCommandQueue putAcquireGLObjects(NativeSizeBuffer glObjectIDs, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        CLGL xl = (CLGL) cl;

        int ret = xl.clEnqueueAcquireGLObjects(ID, glObjectIDs.remaining(), glObjectIDs,
                    conditions, conditionIDs,
                    events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not aquire " + glObjectIDs + " with " + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    /**
     * Calls {@native clEnqueueReleaseGLObjects}.
     */
    public CLCommandQueue putReleaseGLObject(CLGLObject glObject) {
        this.putReleaseGLObject(glObject, null);
        return this;
    }

    /**
     * Calls {@native clEnqueueReleaseGLObjects}.
     */
    public CLCommandQueue putReleaseGLObject(CLGLObject glObject, CLEventList events) {
        this.putReleaseGLObject(glObject, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueReleaseGLObjects}.
     */
    public CLCommandQueue putReleaseGLObject(CLGLObject glObject, CLEventList condition, CLEventList events) {
        this.putReleaseGLObjects(copy2NIO(ibA, glObject.getID()), condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueAcquireGLObjects}.
     */
    public CLCommandQueue putReleaseGLObjects(CLGLObject glObject1, CLGLObject glObject2, CLEventList condition, CLEventList events) {
        this.putReleaseGLObjects(copy2NIO(ibA, glObject1.getID(), glObject2.getID()), condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueAcquireGLObjects}.
     */
    public CLCommandQueue putReleaseGLObjects(CLGLObject glObject1, CLGLObject glObject2, CLGLObject glObject3, CLEventList condition, CLEventList events) {
        this.putReleaseGLObjects(copy2NIO(ibA, glObject1.getID(), glObject2.getID(), glObject3.getID()), condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueReleaseGLObjects}.
     */
    public CLCommandQueue putReleaseGLObjects(NativeSizeBuffer glObjectIDs, CLEventList condition, CLEventList events) {

        NativeSizeBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDsView;
            conditions   = condition.size;
        }

        CLGL xl = (CLGL) cl;

        int ret = xl.clEnqueueReleaseGLObjects(ID, glObjectIDs.remaining(), glObjectIDs,
                conditions, conditionIDs,
                events==null ? null : events.IDs);

        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not release " + glObjectIDs + "with " + toStr(condition, events));
        }

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    /**
     * Calls {@native clFinish}.
     */
    public CLCommandQueue finish() {
        int ret = cl.clFinish(ID);
        checkForError(ret, "can not finish command queue");
        return this;
    }

    /**
     * Calls {@native clFlush}.
     */
    public CLCommandQueue flush() {
        int ret = cl.clFlush(ID);
        checkForError(ret, "can not flush command queue");
        return this;
    }

    /**
     * Returns true only when {@link Mode#PROFILING_MODE} has been enabled.
     */
    public boolean isProfilingEnabled() {
        return (Mode.PROFILING_MODE.QUEUE_MODE & properties) != 0;
    }

    /**
     * Returns true only when {@link Mode#OUT_OF_ORDER_MODE} mode has been enabled.
     */
    public boolean isOutOfOrderModeEnabled() {
        return (Mode.OUT_OF_ORDER_MODE.QUEUE_MODE & properties) != 0;
    }

    @Override
    public synchronized void release() {
        super.release();
        int ret = cl.clReleaseCommandQueue(ID);
        context.onCommandQueueReleased(device, this);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not release "+this);
        }
    }

    private static NativeSizeBuffer copy2NIO(NativeSizeBuffer buffer, long a) {
        return buffer.put(2, a).position(2);
    }

    private static NativeSizeBuffer copy2NIO(NativeSizeBuffer buffer, long a, long b) {
        return buffer.position(1).put(a).put(b).position(1);
    }

    private static NativeSizeBuffer copy2NIO(NativeSizeBuffer buffer, long a, long b, long c) {
        return buffer.rewind().put(a).put(b).put(c).rewind();
    }

    private static String toStr(NativeSizeBuffer buffer) {
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

    private static String toStr(CLEventList condition, CLEventList events) {
        return "\ncond.: " + condition +" events: "+events;
    }

    private String toStr(Integer... values) {
        return Arrays.asList(values).toString();
    }

    private String bufferRectToString(String action, CLBuffer<?> buffer,
            long rowPitch, long slicePitch, long hostRowPitch, long hostSlicePitch,
            int originX, int originY, int originZ, int hostX, int hostY, int hostZ,
            int rangeX, int rangeY, int rangeZ, CLEventList condition, CLEventList events) {
        return "can not enqueue "+action+"-buffer-rect: " + buffer + "\n"
                + " with rowPitch: " + rowPitch + " slicePitch: " + slicePitch
                + " hostRowPitch: " + hostRowPitch + " hostSlicePitch: " + hostSlicePitch + "\n"
                + " origin: " + toStr(originX, originY, originZ) + " hostPos: " + toStr(hostX, hostY, hostZ)
                + " range: " + toStr(rangeX, rangeY, rangeZ) + toStr(condition, events);
    }

    /**
     * Returns the device of this command queue.
     */
    public CLDevice getDevice() {
        return device;
    }

    /**
     * Returns the command queue properties as EnumSet.
     */
    public EnumSet<Mode> getProperties() {
        return Mode.valuesOf(properties);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +" "+getProperties()+" on "+ getDevice();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CLCommandQueue other = (CLCommandQueue) obj;
        if (this.ID != other.ID) {
            return false;
        }
        if (this.context != other.context && (this.context == null || !this.context.equals(other.context))) {
            return false;
        }
        if (this.device != other.device && (this.device == null || !this.device.equals(other.device))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + (int) (this.ID ^ (this.ID >>> 32));
        hash = 89 * hash + (this.context != null ? this.context.hashCode() : 0);
        hash = 89 * hash + (this.device != null ? this.device.hashCode() : 0);
        return hash;
    }

    /**
     * Enumeration for the command-queue settings.
     */
    public enum Mode {
        /**
         * If set, the commands in the command-queue are
         * executed out-of-order. Otherwise, commands are executed in-order.
         */
        OUT_OF_ORDER_MODE(CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE),

        /**
         * Enables profiling of commands in the command-queue.
         * If set, the profiling of commands is enabled. Otherwise profiling of
         * commands is disabled. See {@link com.jogamp.opencl.CLEvent} for more information.
         */
        PROFILING_MODE(CL_QUEUE_PROFILING_ENABLE);

        /**
         * Value of wrapped OpenCL device type.
         */
        public final int QUEUE_MODE;

        private Mode(int value) {
            this.QUEUE_MODE = value;
        }

        public static Mode valueOf(int queueMode) {
            switch(queueMode) {
                case(CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE):
                    return OUT_OF_ORDER_MODE;
                case(CL_QUEUE_PROFILING_ENABLE):
                    return PROFILING_MODE;
            }
            return null;
        }

        public static EnumSet<Mode> valuesOf(long bitfield) {
            List<Mode> matching = new ArrayList<Mode>();
            Mode[] values = Mode.values();
            for (Mode value : values) {
                if((value.QUEUE_MODE & bitfield) != 0)
                    matching.add(value);
            }
            if(matching.isEmpty())
                return EnumSet.noneOf(Mode.class);
            else
                return EnumSet.copyOf(matching);
        }

    }
}
