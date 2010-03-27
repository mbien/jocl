package com.mbien.opencl;

import com.mbien.opencl.gl.CLGLI;
import com.jogamp.gluegen.runtime.PointerBuffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.mbien.opencl.CLException.*;
import static com.mbien.opencl.CL.*;
import static com.mbien.opencl.util.CLUtil.*;

/**
 * The command queue is used to queue a set of operations for a specific {@link CLDevice}.
 * Having multiple command-queues allows applications to queue multiple independent commands without
 * requiring synchronization. Note that this should work as long as these objects are
 * not being shared.<br/>
 * Sharing of objects across multiple queues or using a CLCommandQueue
 * form multiple Threads will require the application to perform appropriate synchronization.
 * @see CLDevice#createCommandQueue(com.mbien.opencl.CLCommandQueue.Mode...)
 * @author Michael Bien
 */
public class CLCommandQueue extends CLObject implements CLResource {

    private final CLDevice device;
    private long properties;

    /*
     * Those direct memory buffers are used to move data between the JVM and OpenCL.
     */
    private final PointerBuffer bufferA;
    private final PointerBuffer bufferB;
    private final PointerBuffer bufferC;

    private CLCommandQueue(CLContext context, long id, CLDevice device, long properties) {
        super(context, id);

        this.device = device;
        this.properties = properties;

        this.bufferA = PointerBuffer.allocateDirect(3);
        this.bufferB = PointerBuffer.allocateDirect(3);
        this.bufferC = PointerBuffer.allocateDirect(3);

    }

    static CLCommandQueue create(CLContext context, CLDevice device, long properties) {
        int[] status = new int[1];
        long id = context.cl.clCreateCommandQueue(context.ID, device.ID, properties, status, 0);

        if(status[0] != CL_SUCCESS)
            throw newException(status[0], "can not create command queue on "+device);

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

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        int ret = cl.clEnqueueWriteBuffer(
                ID, writeBuffer.ID, clBoolean(blockingWrite),
                0, writeBuffer.getSize(), writeBuffer.buffer,
                conditions, conditionIDs, events==null ? null : events.IDs);

        if(ret != CL_SUCCESS)
            throw newException(ret, "can not enqueue WriteBuffer: " + writeBuffer);

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

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        int ret = cl.clEnqueueReadBuffer(
                ID, readBuffer.ID, clBoolean(blockingRead),
                0, readBuffer.getSize(), readBuffer.buffer,
                conditions, conditionIDs, events==null ? null : events.IDs);

        if(ret != CL_SUCCESS)
            throw newException(ret, "can not enqueue ReadBuffer: " + readBuffer);

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

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        int ret = cl.clEnqueueCopyBuffer(
                        ID, src.ID, dest.ID, srcOffset, destOffset, bytesToCopy,
                        conditions, conditionIDs, events==null ? null : events.IDs);

        checkForError(ret, "can not copy Buffer");

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

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        // spec: CL_INVALID_VALUE if image is a 2D image object and origin[2] is not equal to 0
        // or region[2] is not equal to 1 or slice_pitch is not equal to 0.
        copy2NIO(bufferA, originX, originY, 0);
        copy2NIO(bufferB, rangeX, rangeY, 1);

        int ret = cl.clEnqueueWriteImage(ID, writeImage.ID, clBoolean(blockingWrite),
                                         bufferA, bufferB, inputRowPitch, 0, writeImage.buffer,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        checkForError(ret, "can not write Image");

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

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        copy2NIO(bufferA, originX, originY, originZ);
        copy2NIO(bufferB, rangeX, rangeY, rangeZ);

        int ret = cl.clEnqueueWriteImage(ID, writeImage.ID, clBoolean(blockingWrite),
                                         bufferA, bufferB, inputRowPitch, inputSlicePitch, writeImage.buffer,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        checkForError(ret, "can not write Image");

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

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        // spec: CL_INVALID_VALUE if image is a 2D image object and origin[2] is not equal to 0
        // or region[2] is not equal to 1 or slice_pitch is not equal to 0.
        copy2NIO(bufferA, originX, originY, 0);
        copy2NIO(bufferB, rangeX, rangeY, 1);

        int ret = cl.clEnqueueReadImage(ID, readImage.ID, clBoolean(blockingRead),
                                         bufferA, bufferB, inputRowPitch, 0, readImage.buffer,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        checkForError(ret, "can not read Image");

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

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        copy2NIO(bufferA, originX, originY, originZ);
        copy2NIO(bufferB, rangeX, rangeY, rangeZ);

        int ret = cl.clEnqueueReadImage(ID, readImage.ID, clBoolean(blockingRead),
                                        bufferA, bufferB, inputRowPitch, inputSlicePitch, readImage.buffer,
                                        conditions, conditionIDs, events==null ? null : events.IDs);
        checkForError(ret, "can not read Image");

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

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        //spec: CL_INVALID_VALUE if src_image is a 2D image object and origin[2] or dst_origin[2] is not equal to 0
        // or region[2] is not equal to 1.
        copy2NIO(bufferA, srcOriginX, srcOriginY, 0);
        copy2NIO(bufferB, dstOriginX, dstOriginY, 0);
        copy2NIO(bufferC, rangeX, rangeY, 1);

        int ret = cl.clEnqueueCopyImage(ID, srcImage.ID, dstImage.ID, bufferA, bufferB, bufferC,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        checkForError(ret, "can not copy Image");

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
    public CLCommandQueue putCopyImage(CLImage3d<?> srcImage, CLImage3d<?> dstImage,
                                        int srcOriginX, int srcOriginY, int srcOriginZ,
                                        int dstOriginX, int dstOriginY, int dstOriginZ,
                                        int rangeX, int rangeY, int rangeZ, CLEventList condition, CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        copy2NIO(bufferA, srcOriginX, srcOriginY, srcOriginZ);
        copy2NIO(bufferB, dstOriginX, dstOriginY, dstOriginZ);
        copy2NIO(bufferC, rangeX, rangeY, rangeZ);

        int ret = cl.clEnqueueCopyImage(ID, srcImage.ID, dstImage.ID, bufferA, bufferB, bufferC,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        checkForError(ret, "can not copy Image");

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

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        // spec: CL_INVALID_VALUE if dst_image is a 2D image object and dst_origin[2] is not equal to 0
        // or region[2] is not equal to 1.
        copy2NIO(bufferA, dstOriginX, dstOriginY, 0);
        copy2NIO(bufferB, rangeX, rangeY, 1);

        int ret = cl.clEnqueueCopyBufferToImage(ID, srcBuffer.ID, dstImage.ID,
                                         srcOffset, bufferA, bufferB,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        checkForError(ret, "can not copy buffer to image2d");

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

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        copy2NIO(bufferA, dstOriginX, dstOriginY, dstOriginZ);
        copy2NIO(bufferB, rangeX, rangeY, rangeZ);

        int ret = cl.clEnqueueCopyBufferToImage(ID, srcBuffer.ID, dstImage.ID,
                                         srcOffset, bufferA, bufferB,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        checkForError(ret, "can not copy buffer to image3d");

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

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        // spec: CL_INVALID_VALUE if src_image is a 2D image object and src_origin[2] is not equal to 0
        // or region[2] is not equal to 1.
        copy2NIO(bufferA, srcOriginX, srcOriginY, 0);
        copy2NIO(bufferB, rangeX, rangeY, 1);

        int ret = cl.clEnqueueCopyImageToBuffer(ID, dstBuffer.ID, srcImage.ID,
                                         bufferA, bufferB, dstOffset,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        checkForError(ret, "can not copy buffer to image2d");

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
        
        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        copy2NIO(bufferA, srcOriginX, srcOriginY, srcOriginZ);
        copy2NIO(bufferB, rangeX, rangeY, rangeZ);

        int ret = cl.clEnqueueCopyImageToBuffer(ID, dstBuffer.ID, srcImage.ID,
                                         bufferA, bufferB, dstOffset,
                                         conditions, conditionIDs, events==null ? null : events.IDs);
        checkForError(ret, "can not copy buffer to image3d");

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

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        IntBuffer error = bufferA.position(0).getBuffer().asIntBuffer();
        ByteBuffer mappedBuffer = cl.clEnqueueMapBuffer(ID, buffer.ID, clBoolean(blockingMap),
                                         flag.FLAGS, offset, length,
                                         conditions, conditionIDs, events==null ? null : events.IDs, error);
        checkForError(error.get(), "can not map buffer");

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
    public ByteBuffer putMapImage(CLImage2d<?> buffer, CLMemory.Map flag,
                                    int offsetX, int offsetY,
                                    int rangeX, int rangeY, boolean blockingMap, CLEventList condition, CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        IntBuffer error = bufferA.position(0).getBuffer().asIntBuffer();

        // spec: CL_INVALID_VALUE if image is a 2D image object and origin[2] is not equal to 0 or region[2] is not equal to 1
        copy2NIO(bufferB, offsetX, offsetY, 0);
        copy2NIO(bufferC, rangeX, rangeY, 1);

        ByteBuffer mappedImage = cl.clEnqueueMapImage(ID, buffer.ID, clBoolean(blockingMap),
                                         flag.FLAGS, bufferB, bufferC, null, null,
                                         conditions, conditionIDs, events==null ? null : events.IDs, error);
        checkForError(error.get(), "can not map image2d");

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
    public ByteBuffer putMapImage(CLImage3d<?> buffer, CLMemory.Map flag,
                                    int offsetX, int offsetY, int offsetZ,
                                    int rangeX, int rangeY, int rangeZ, boolean blockingMap, CLEventList condition, CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        IntBuffer error = bufferA.position(0).getBuffer().asIntBuffer();
        copy2NIO(bufferB, offsetX, offsetY, offsetZ);
        copy2NIO(bufferC, rangeX, rangeY, rangeZ);
        ByteBuffer mappedImage = cl.clEnqueueMapImage(ID, buffer.ID, clBoolean(blockingMap),
                                         flag.FLAGS, bufferB, bufferC, null, null,
                                         conditions, conditionIDs, events==null ? null : events.IDs, error);
        checkForError(error.get(), "can not map image3d");

        if(events != null) {
            events.createEvent(context);
        }

        return mappedImage;
    }

    /**
     * Calls {@native clEnqueueUnmapMemObject}.
     */
    public CLCommandQueue putUnmapMemory(CLMemory<?> memory) {
        return putUnmapMemory(memory, null, null);
    }

    /**
     * Calls {@native clEnqueueUnmapMemObject}.
     */
    public CLCommandQueue putUnmapMemory(CLMemory<?> memory, CLEventList events) {
        return putUnmapMemory(memory, null, events);
    }

    /**
     * Calls {@native clEnqueueUnmapMemObject}.
     */
    public CLCommandQueue putUnmapMemory(CLMemory<?> memory, CLEventList condition, CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        int ret = cl.clEnqueueUnmapMemObject(ID, memory.ID, memory.getBuffer(),
                                        conditions, conditionIDs, events==null ? null : events.IDs);
        checkForError(ret, "can not unmap memory");

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    /**
     * Calls {@native clEnqueueMarker}.
     */
    public CLCommandQueue putMarker(CLEventList events) {
        int ret = cl.clEnqueueMarker(CL_INT_MIN, events.IDs);
        checkForError(ret, "can not enqueue marker");
        return this;
    }

    /**
     * Calls {@native clWaitForEvents} if blockingWait equals true otherwise {@native clEnqueueWaitForEvents}.
     */
    public CLCommandQueue putWaitForEvent(CLEventList list, int index, boolean blockingWait) {
        int marker = list.IDs.position()-1;
        list.IDs.position(index);
        int ret = blockingWait ? cl.clWaitForEvents(1, list.IDs)
                               : cl.clEnqueueWaitForEvents(ID, 1, list.IDs);
        list.IDs.position(marker);
        checkForError(ret, "error while waiting for events");
        return this;
    }

    /**
     * Calls {@native clWaitForEvents} if blockingWait equals true otherwise {@native clEnqueueWaitForEvents}.
     */
    public CLCommandQueue putWaitForEvents(CLEventList list, boolean blockingWait) {
        list.IDs.rewind();
        int ret = blockingWait ? cl.clWaitForEvents(list.size, list.IDs)
                               : cl.clEnqueueWaitForEvents(ID, list.size, list.IDs);
        checkForError(ret, "error while waiting for events");
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
        int ret = cl.clEnqueueTask(ID, kernel.ID, 0, null, null);
        checkForError(ret, "can not enqueue Task");
        return this;
    }

    /**
     * <p>Calls {@native clEnqueueTask}.</p>
     * @see #putTask(com.mbien.opencl.CLKernel)
     */
    public CLCommandQueue putTask(CLKernel kernel, CLEventList events) {
        putTask(kernel, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueTask}.
     * @see #putTask(com.mbien.opencl.CLKernel)
     */
    public CLCommandQueue putTask(CLKernel kernel, CLEventList condition, CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        int ret = cl.clEnqueueTask(ID, kernel.ID, conditions, conditionIDs, events==null ? null : events.IDs);
        checkForError(ret, "can not enqueue Task");
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
        PointerBuffer globWO = null;
        PointerBuffer globWS = null;
        PointerBuffer locWS = null;

        if(globalWorkOffset != 0) {
            globWO = copy2NIO(bufferA, globalWorkOffset);
        }
        if(globalWorkSize != 0) {
            globWS = copy2NIO(bufferB, globalWorkSize);
        }
        if(localWorkSize != 0) {
            locWS = copy2NIO(bufferC, localWorkSize);
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
        PointerBuffer globalWorkOffset = null;
        PointerBuffer globalWorkSize = null;
        PointerBuffer localWorkSize = null;

        if(globalWorkOffsetX != 0 && globalWorkOffsetY != 0) {
            globalWorkOffset = copy2NIO(bufferA, globalWorkOffsetX, globalWorkOffsetY);
        }
        if(globalWorkSizeX != 0 && globalWorkSizeY != 0) {
            globalWorkSize = copy2NIO(bufferB, globalWorkSizeX, globalWorkSizeY);
        }
        if(localWorkSizeX != 0 && localWorkSizeY !=0) {
            localWorkSize = copy2NIO(bufferC, localWorkSizeX, localWorkSizeY);
        }
        this.putNDRangeKernel(kernel, 2, globalWorkOffset, globalWorkSize, localWorkSize, condition, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue putNDRangeKernel(CLKernel kernel, int workDimension, PointerBuffer globalWorkOffset, PointerBuffer globalWorkSize, PointerBuffer localWorkSize) {
        this.putNDRangeKernel(kernel, workDimension, globalWorkOffset, globalWorkSize, localWorkSize, null, null);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue putNDRangeKernel(CLKernel kernel, int workDimension, PointerBuffer globalWorkOffset, PointerBuffer globalWorkSize, PointerBuffer localWorkSize, CLEventList events) {
        this.putNDRangeKernel(kernel, workDimension, globalWorkOffset, globalWorkSize, localWorkSize, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueNDRangeKernel}.
     */
    public CLCommandQueue putNDRangeKernel(CLKernel kernel, int workDimension, PointerBuffer globalWorkOffset,
            PointerBuffer globalWorkSize, PointerBuffer localWorkSize, CLEventList condition, CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        int ret = cl.clEnqueueNDRangeKernel(
                ID, kernel.ID, workDimension,
                globalWorkOffset,
                globalWorkSize, 
                localWorkSize, 
                conditions, conditionIDs,
                events==null ? null : events.IDs);

        if(ret != CL_SUCCESS)
            throw newException(ret, "can not enqueue NDRangeKernel: " + kernel);

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    /**
     * Calls {@native clEnqueueAcquireGLObjects}.
     */
    public CLCommandQueue putAcquireGLObject(long glObject) {
        this.putAcquireGLObject(glObject, null, null);
        return this;
    }

    /**
     * Calls {@native clEnqueueAcquireGLObjects}.
     */
    public CLCommandQueue putAcquireGLObject(long glObject, CLEventList events) {
        this.putAcquireGLObject(glObject, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueAcquireGLObjects}.
     */
    public CLCommandQueue putAcquireGLObject(long glObject, CLEventList condition, CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        CLGLI xl = (CLGLI) cl;

        PointerBuffer glObj = copy2NIO(bufferA, glObject);
        
        int ret = xl.clEnqueueAcquireGLObjects(ID, 1, glObj,
                    conditions, conditionIDs,
                    events==null ? null : events.IDs);

        if(ret != CL_SUCCESS)
            throw newException(ret, "can not aquire GLObject: " + glObject);

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    /**
     * Calls {@native clEnqueueReleaseGLObjects}.
     */
    public CLCommandQueue putReleaseGLObject(long glObject) {
        this.putReleaseGLObject(glObject, null);
        return this;
    }

    /**
     * Calls {@native clEnqueueReleaseGLObjects}.
     */
    public CLCommandQueue putReleaseGLObject(long glObject, CLEventList events) {
        this.putReleaseGLObject(glObject, null, events);
        return this;
    }

    /**
     * Calls {@native clEnqueueReleaseGLObjects}.
     */
    public CLCommandQueue putReleaseGLObject(long glObject, CLEventList condition, CLEventList events) {

        PointerBuffer conditionIDs = null;
        int conditions = 0;
        if(condition != null) {
            conditionIDs = condition.IDs;
            conditions   = condition.size;
        }

        CLGLI xl = (CLGLI) cl;

        PointerBuffer glObj = copy2NIO(bufferA, glObject);

        int ret = xl.clEnqueueReleaseGLObjects(ID, 1, glObj,
                conditions, conditionIDs,
                events==null ? null : events.IDs);

        if(ret != CL_SUCCESS)
            throw newException(ret, "can not release GLObject: " + glObject);

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

    public void release() {
        int ret = cl.clReleaseCommandQueue(ID);
        context.onCommandQueueReleased(device, this);
        checkForError(ret, "can not release command queue");
    }

    public void close() {
        release();
    }

    private static PointerBuffer copy2NIO(PointerBuffer buffer, long a) {
        return buffer.put(2, a).position(2);
    }

    private static PointerBuffer copy2NIO(PointerBuffer buffer, long a, long b) {
        return buffer.position(1).put(a).put(b).position(1);
    }

    private static PointerBuffer copy2NIO(PointerBuffer buffer, long a, long b, long c) {
        return buffer.rewind().put(a).put(b).put(c).rewind();
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

    /**
     * Setting properties after a command queue has been created can be implementation specific,
     * please refer to the specification ({@native clSetCommandQueueProperty}) or vendor documentation.
     */
    public void setProperty(Mode property, boolean enabled) {
        int ret = cl.clSetCommandQueueProperty(ID, property.QUEUE_MODE, clBoolean(enabled), null);
        if(ret != CL_SUCCESS) {
            checkForError(ret, "can not set command queue property: " + property);
        }
        if(enabled) {
            properties |=  property.QUEUE_MODE;
        }else{
            properties &= ~property.QUEUE_MODE;
        }
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
         * commands is disabled. See {@link com.mbien.opencl.CLEvent} for more information.
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
