package com.mbien.opencl;

import com.sun.gluegen.runtime.PointerBuffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.mbien.opencl.CLException.*;
import static com.mbien.opencl.CL.*;

/**
 * The command-queue can be used to queue a set of operations in order. Having multiple
 * command-queues allows applications to queue multiple independent commands without
 * requiring synchronization. Note that this should work as long as these objects are
 * not being shared.<br/>
 * Sharing of objects across multiple command-queues or using a CLCommandQueue
 * form multiple Threads will require the application to perform appropriate synchronization.
 * @author Michael Bien
 */
public class CLCommandQueue implements CLResource {

    public final long ID;

    private final CLContext context;
    private final CLDevice device;
    private final CL cl;
    private long properties;

    /*
     * Those direct memory buffers are used to move data between the JVM and OpenCL.
     */
    private final PointerBuffer bufferA;
    private final PointerBuffer bufferB;
    private final PointerBuffer bufferC;

    CLCommandQueue(CLContext context, CLDevice device, long properties) {
        this.context = context;
        this.cl = context.cl;
        this.device = device;
        this.properties = properties;

        this.bufferA = PointerBuffer.allocateDirect(3);
        this.bufferB = PointerBuffer.allocateDirect(3);
        this.bufferC = PointerBuffer.allocateDirect(3);

        int[] status = new int[1];
        this.ID = cl.clCreateCommandQueue(context.ID, device.ID, properties, status, 0);

        if(status[0] != CL_SUCCESS)
            throw new CLException(status[0], "can not create command queue on "+device);
    }

    public CLCommandQueue putWriteBuffer(CLBuffer<?> writeBuffer, boolean blockingRead) {
        return putWriteBuffer(writeBuffer, blockingRead, null);
    }

    public CLCommandQueue putWriteBuffer(CLBuffer<?> writeBuffer, boolean blockingWrite, CLEventList events) {

        int ret = cl.clEnqueueWriteBuffer(
                ID, writeBuffer.ID, blockingWrite ? CL_TRUE : CL_FALSE,
                0, writeBuffer.getSize(), writeBuffer.buffer,
                0, null, events==null ? null : events.IDs);

        if(ret != CL_SUCCESS)
            throw new CLException(ret, "can not enqueue WriteBuffer: " + writeBuffer);

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    public CLCommandQueue putReadBuffer(CLBuffer<?> readBuffer, boolean blockingRead) {
        putReadBuffer(readBuffer, blockingRead, null);
        return this;
    }

    public CLCommandQueue putReadBuffer(CLBuffer<?> readBuffer, boolean blockingRead, CLEventList events) {

        int ret = cl.clEnqueueReadBuffer(
                ID, readBuffer.ID, blockingRead ? CL_TRUE : CL_FALSE,
                0, readBuffer.getSize(), readBuffer.buffer,
                0, null, events==null ? null : events.IDs);

        if(ret != CL_SUCCESS)
            throw new CLException(ret, "can not enqueue ReadBuffer: " + readBuffer);

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }
/*
    public CLCommandQueue putReadBuffer(CLBuffer<?> readBuffer, Buffer buffer, boolean blockingRead) {

        int ret = cl.clEnqueueReadBuffer(
                ID, readBuffer.ID, blockingRead ? CL_TRUE : CL_FALSE,
                0, readBuffer.getSizeInBytes(), buffer,
//                0, null, null); //TODO solve NPE in gluegen when PointerBuffer == null (fast dircet memory path)
                0, null, 0, null, 0); //TODO events

        if(ret != CL_SUCCESS)
            throw new CLException(ret, "can not enqueue ReadBuffer: " + readBuffer);

        return this;
    }
*/

    public CLCommandQueue putCopyBuffer(CLBuffer<?> src, CLBuffer<?> dest) {
        return putCopyBuffer(src, dest, 0, 0, src.getCLSize(), null);
    }

    public CLCommandQueue putCopyBuffer(CLBuffer<?> src, CLBuffer<?> dest, long bytesToCopy) {
        return putCopyBuffer(src, dest, 0, 0, bytesToCopy, null);
    }

    public CLCommandQueue putCopyBuffer(CLBuffer<?> src, CLBuffer<?> dest, int srcOffset, int destOffset, long bytesToCopy, CLEventList events) {

        int ret = cl.clEnqueueCopyBuffer(
                        ID, src.ID, dest.ID, srcOffset, destOffset, bytesToCopy,
                        0, null, events==null ? null : events.IDs);

        checkForError(ret, "can not copy Buffer");

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    //2D
    public CLCommandQueue putWriteImage(CLImage2d<?> writeImage, boolean blockingWrite) {
        return putWriteImage(writeImage, 0, 0, 0, writeImage.width, writeImage.height, blockingWrite, null);
    }

    public CLCommandQueue putWriteImage(CLImage2d<?> writeImage, boolean blockingWrite, CLEventList events) {
        return putWriteImage(writeImage, 0, 0, 0, writeImage.width, writeImage.height, blockingWrite, events);
    }

    public CLCommandQueue putWriteImage(CLImage2d<?> writeImage, int inputRowPitch,
            int originX, int originY, int rangeX, int rangeY, boolean blockingWrite) {
        return putWriteImage(writeImage, inputRowPitch, originX, originY, rangeX, rangeY, blockingWrite, null);
    }

    public CLCommandQueue putWriteImage(CLImage2d<?> writeImage, int inputRowPitch,
            int originX, int originY, int rangeX, int rangeY, boolean blockingWrite, CLEventList events) {

        // spec: CL_INVALID_VALUE if image is a 2D image object and origin[2] is not equal to 0
        // or region[2] is not equal to 1 or slice_pitch is not equal to 0.
        copy2NIO(bufferA, originX, originY, 0);
        copy2NIO(bufferB, rangeX, rangeY, 1);

        int ret = cl.clEnqueueWriteImage(ID, writeImage.ID, blockingWrite ? CL_TRUE : CL_FALSE,
                                         bufferA, bufferB, inputRowPitch, 0, writeImage.buffer,
                                         0, null, events==null ? null : events.IDs);
        checkForError(ret, "can not write Image");

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //3D
    public CLCommandQueue putWriteImage(CLImage3d<?> writeImage, boolean blockingWrite) {
        return putWriteImage(writeImage, 0, 0, 0, 0, 0, writeImage.width, writeImage.height, writeImage.depth, blockingWrite, null);
    }

    public CLCommandQueue putWriteImage(CLImage3d<?> writeImage, boolean blockingWrite, CLEventList events) {
        return putWriteImage(writeImage, 0, 0, 0, 0, 0, writeImage.width, writeImage.height, writeImage.depth, blockingWrite, events);
    }

    public CLCommandQueue putWriteImage(CLImage3d<?> writeImage, int inputRowPitch, int inputSlicePitch,
            int originX, int originY, int originZ, int rangeX, int rangeY, int rangeZ, boolean blockingWrite) {
        return putWriteImage(writeImage, inputRowPitch, inputSlicePitch, originX, originY, originZ, rangeX, rangeY, rangeZ, blockingWrite, null);
    }

    public CLCommandQueue putWriteImage(CLImage3d<?> writeImage, int inputRowPitch, int inputSlicePitch,
            int originX, int originY, int originZ, int rangeX, int rangeY, int rangeZ, boolean blockingWrite, CLEventList events) {

        copy2NIO(bufferA, originX, originY, originZ);
        copy2NIO(bufferB, rangeX, rangeY, rangeZ);

        int ret = cl.clEnqueueWriteImage(ID, writeImage.ID, blockingWrite ? CL_TRUE : CL_FALSE,
                                         bufferA, bufferB, inputRowPitch, inputSlicePitch, writeImage.buffer,
                                         0, null, events==null ? null : events.IDs);
        checkForError(ret, "can not write Image");

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //2D
    public CLCommandQueue putReadImage(CLImage2d<?> readImage, boolean blockingRead) {
        return putReadImage(readImage, 0, 0, 0, readImage.width, readImage.height, blockingRead, null);
    }

    public CLCommandQueue putReadImage(CLImage2d<?> readImage, boolean blockingRead, CLEventList events) {
        return putReadImage(readImage, 0, 0, 0, readImage.width, readImage.height, blockingRead, events);
    }

    public CLCommandQueue putReadImage(CLImage2d<?> readImage, int inputRowPitch,
            int originX, int originY, int rangeX, int rangeY, boolean blockingRead) {
        return putReadImage(readImage, inputRowPitch, originX, originY, rangeX, rangeY, blockingRead, null);
    }

    public CLCommandQueue putReadImage(CLImage2d<?> readImage, int inputRowPitch,
            int originX, int originY, int rangeX, int rangeY, boolean blockingRead, CLEventList events) {

        // spec: CL_INVALID_VALUE if image is a 2D image object and origin[2] is not equal to 0
        // or region[2] is not equal to 1 or slice_pitch is not equal to 0.
        copy2NIO(bufferA, originX, originY, 0);
        copy2NIO(bufferB, rangeX, rangeY, 1);

        int ret = cl.clEnqueueReadImage(ID, readImage.ID, blockingRead ? CL_TRUE : CL_FALSE,
                                         bufferA, bufferB, inputRowPitch, 0, readImage.buffer,
                                         0, null, events==null ? null : events.IDs);
        checkForError(ret, "can not read Image");

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //3D
    public CLCommandQueue putReadImage(CLImage3d<?> readImage, boolean blockingRead) {
        return putReadImage(readImage, 0, 0, 0, 0, 0, readImage.width, readImage.height, readImage.depth, blockingRead, null);
    }

    public CLCommandQueue putReadImage(CLImage3d<?> readImage, boolean blockingRead, CLEventList events) {
        return putReadImage(readImage, 0, 0, 0, 0, 0, readImage.width, readImage.height, readImage.depth, blockingRead, events);
    }

    public CLCommandQueue putReadImage(CLImage3d<?> readImage, int inputRowPitch, int inputSlicePitch,
            int originX, int originY, int originZ, int rangeX, int rangeY, int rangeZ, boolean blockingRead) {
        return putReadImage(readImage, inputRowPitch, inputSlicePitch, originX, originY, originZ, rangeX, rangeY, rangeZ, blockingRead, null);
    }

    public CLCommandQueue putReadImage(CLImage3d<?> readImage, int inputRowPitch, int inputSlicePitch,
            int originX, int originY, int originZ, int rangeX, int rangeY, int rangeZ, boolean blockingRead, CLEventList events) {

        copy2NIO(bufferA, originX, originY, originZ);
        copy2NIO(bufferB, rangeX, rangeY, rangeZ);

        int ret = cl.clEnqueueReadImage(ID, readImage.ID, blockingRead ? CL_TRUE : CL_FALSE,
                                         bufferA, bufferB, inputRowPitch, inputSlicePitch, readImage.buffer,
                                         0, null, events==null ? null : events.IDs);
        checkForError(ret, "can not read Image");

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //2D
    public CLCommandQueue putCopyImage(CLImage2d<?> srcImage, CLImage2d<?> dstImage) {
        return putCopyImage(srcImage, dstImage, null);
    }

    public CLCommandQueue putCopyImage(CLImage2d<?> srcImage, CLImage2d<?> dstImage, CLEventList events) {
        return putCopyImage(srcImage, dstImage, 0, 0, 0, 0, srcImage.width, srcImage.height, events);
    }

    public CLCommandQueue putCopyImage(CLImage2d<?> srcImage, CLImage2d<?> dstImage,
                                        int srcOriginX, int srcOriginY,
                                        int dstOriginX, int dstOriginY,
                                        int rangeX, int rangeY) {
        return putCopyImage(srcImage, dstImage, srcOriginX, srcOriginY, dstOriginX, dstOriginY, rangeX, rangeY, null);
    }

    public CLCommandQueue putCopyImage(CLImage2d<?> srcImage, CLImage2d<?> dstImage,
                                        int srcOriginX, int srcOriginY,
                                        int dstOriginX, int dstOriginY,
                                        int rangeX, int rangeY, CLEventList events) {

        //spec: CL_INVALID_VALUE if src_image is a 2D image object and origin[2] or dst_origin[2] is not equal to 0
        // or region[2] is not equal to 1.
        copy2NIO(bufferA, srcOriginX, srcOriginY, 0);
        copy2NIO(bufferB, dstOriginX, dstOriginY, 0);
        copy2NIO(bufferC, rangeX, rangeY, 1);

        int ret = cl.clEnqueueCopyImage(ID, srcImage.ID, dstImage.ID, bufferA, bufferB, bufferC,
                                         0, null, events==null ? null : events.IDs);
        checkForError(ret, "can not copy Image");

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //3D
    public CLCommandQueue putCopyImage(CLImage3d<?> srcImage, CLImage3d<?> dstImage) {
        return putCopyImage(srcImage, dstImage, null);
    }

    public CLCommandQueue putCopyImage(CLImage3d<?> srcImage, CLImage3d<?> dstImage, CLEventList events) {
        return putCopyImage(srcImage, dstImage, 0, 0, 0, 0, 0, 0, srcImage.width, srcImage.height, srcImage.depth, events);
    }

    public CLCommandQueue putCopyImage(CLImage3d<?> srcImage, CLImage3d<?> dstImage,
                                        int srcOriginX, int srcOriginY, int srcOriginZ,
                                        int dstOriginX, int dstOriginY, int dstOriginZ,
                                        int rangeX, int rangeY, int rangeZ) {
        return putCopyImage(srcImage, dstImage, srcOriginX, srcOriginY, srcOriginZ,
                                                dstOriginX, dstOriginY, dstOriginZ,
                                                rangeX, rangeY, rangeZ, null);
    }

    public CLCommandQueue putCopyImage(CLImage3d<?> srcImage, CLImage3d<?> dstImage,
                                        int srcOriginX, int srcOriginY, int srcOriginZ,
                                        int dstOriginX, int dstOriginY, int dstOriginZ,
                                        int rangeX, int rangeY, int rangeZ, CLEventList events) {

        copy2NIO(bufferA, srcOriginX, srcOriginY, srcOriginZ);
        copy2NIO(bufferB, dstOriginX, dstOriginY, dstOriginZ);
        copy2NIO(bufferC, rangeX, rangeY, rangeZ);

        int ret = cl.clEnqueueCopyImage(ID, srcImage.ID, dstImage.ID, bufferA, bufferB, bufferC,
                                         0, null, events==null ? null : events.IDs);
        checkForError(ret, "can not copy Image");

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }
    
    //2D
    public CLCommandQueue putCopyBufferToImage(CLBuffer<?> srcBuffer, CLImage2d<?> dstImage) {
        return putCopyBufferToImage(srcBuffer, dstImage, null);
    }
    
    public CLCommandQueue putCopyBufferToImage(CLBuffer<?> srcBuffer, CLImage2d<?> dstImage, CLEventList events) {
        return putCopyBufferToImage(srcBuffer, dstImage, 0, 0, 0, dstImage.width, dstImage.height, events);
    }
        
    public CLCommandQueue putCopyBufferToImage(CLBuffer<?> srcBuffer, CLImage2d<?> dstImage,
                                        long srcOffset, int dstOriginX, int dstOriginY,
                                        int rangeX, int rangeY) {
        return putCopyBufferToImage(srcBuffer, dstImage, 
                srcOffset, dstOriginX, dstOriginY, rangeX, rangeY, null);
    }
    
    public CLCommandQueue putCopyBufferToImage(CLBuffer<?> srcBuffer, CLImage2d<?> dstImage,
                                        long srcOffset, int dstOriginX, int dstOriginY,
                                        int rangeX, int rangeY, CLEventList events) {

        // spec: CL_INVALID_VALUE if dst_image is a 2D image object and dst_origin[2] is not equal to 0
        // or region[2] is not equal to 1.
        copy2NIO(bufferA, dstOriginX, dstOriginY, 0);
        copy2NIO(bufferB, rangeX, rangeY, 1);

        int ret = cl.clEnqueueCopyBufferToImage(ID, srcBuffer.ID, dstImage.ID,
                                         srcOffset, bufferA, bufferB,
                                         0, null, events==null ? null : events.IDs);
        checkForError(ret, "can not copy buffer to image2d");

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }
    
    //3D
    public CLCommandQueue putCopyBufferToImage(CLBuffer<?> srcBuffer, CLImage3d<?> dstImage) {
        return putCopyBufferToImage(srcBuffer, dstImage, null);
    }
    
    public CLCommandQueue putCopyBufferToImage(CLBuffer<?> srcBuffer, CLImage3d<?> dstImage, CLEventList events) {
        return putCopyBufferToImage(srcBuffer, dstImage, 0, 0, 0, 0, dstImage.width, dstImage.height, dstImage.depth, events);
    }
        
    public CLCommandQueue putCopyBufferToImage(CLBuffer<?> srcBuffer, CLImage3d<?> dstImage,
                                        long srcOffset, int dstOriginX, int dstOriginY, int dstOriginZ,
                                        int rangeX, int rangeY, int rangeZ) {
        return putCopyBufferToImage(srcBuffer, dstImage, 
                srcOffset, dstOriginX, dstOriginY, dstOriginZ, rangeX, rangeY, rangeZ, null);
        
    }
    
    public CLCommandQueue putCopyBufferToImage(CLBuffer<?> srcBuffer, CLImage3d<?> dstImage,
                                        long srcOffset, int dstOriginX, int dstOriginY, int dstOriginZ,
                                        int rangeX, int rangeY, int rangeZ, CLEventList events) {

        copy2NIO(bufferA, dstOriginX, dstOriginY, dstOriginZ);
        copy2NIO(bufferB, rangeX, rangeY, rangeZ);

        int ret = cl.clEnqueueCopyBufferToImage(ID, srcBuffer.ID, dstImage.ID,
                                         srcOffset, bufferA, bufferB,
                                         0, null, events==null ? null : events.IDs);
        checkForError(ret, "can not copy buffer to image3d");

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    //2D
    public CLCommandQueue putCopyImageToBuffer(CLImage2d<?> srcImage, CLBuffer<?> dstBuffer) {
        return putCopyImageToBuffer(srcImage, dstBuffer, null);
    }
    
    public CLCommandQueue putCopyImageToBuffer(CLImage2d<?> srcImage, CLBuffer<?> dstBuffer, CLEventList events) {
        return putCopyImageToBuffer(srcImage, dstBuffer, 0, 0, srcImage.width, srcImage.height, 0, events);
    }
        
    public CLCommandQueue putCopyImageToBuffer(CLImage2d<?> srcImage, CLBuffer<?> dstBuffer,
                                        int srcOriginX, int srcOriginY,
                                        int rangeX, int rangeY, long dstOffset) {
        return putCopyImageToBuffer(srcImage, dstBuffer, 
                srcOriginX, srcOriginY, rangeX, rangeY, dstOffset, null);
    }
    
    public CLCommandQueue putCopyImageToBuffer(CLImage2d<?> srcImage, CLBuffer<?> dstBuffer,
                                        int srcOriginX, int srcOriginY,
                                        int rangeX, int rangeY, long dstOffset, CLEventList events) {

        // spec: CL_INVALID_VALUE if src_image is a 2D image object and src_origin[2] is not equal to 0
        // or region[2] is not equal to 1.
        copy2NIO(bufferA, srcOriginX, srcOriginY, 0);
        copy2NIO(bufferB, rangeX, rangeY, 1);

        int ret = cl.clEnqueueCopyImageToBuffer(ID, dstBuffer.ID, srcImage.ID,
                                         bufferA, bufferB, dstOffset,
                                         0, null, events==null ? null : events.IDs);
        checkForError(ret, "can not copy buffer to image2d");

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }
    
    //3D
    public CLCommandQueue putCopyImageToBuffer(CLImage3d<?> srcImage, CLBuffer<?> dstBuffer) {
        return putCopyImageToBuffer(srcImage, dstBuffer, 0, 0, 0, srcImage.width, srcImage.height, srcImage.depth, 0, null);
    }
    
    public CLCommandQueue putCopyImageToBuffer(CLImage3d<?> srcImage, CLBuffer<?> dstBuffer, CLEventList events) {
        return putCopyImageToBuffer(srcImage, dstBuffer, 0, 0, 0, srcImage.width, srcImage.height, srcImage.depth, 0, events);
    }
        
    public CLCommandQueue putCopyImageToBuffer(CLImage3d<?> srcImage, CLBuffer<?> dstBuffer,
                                        int srcOriginX, int srcOriginY, int srcOriginZ,
                                        int rangeX, int rangeY, int rangeZ, long dstOffset) {
        return putCopyImageToBuffer(srcImage, dstBuffer, 
                srcOriginX, srcOriginY, srcOriginZ, rangeX, rangeY, rangeZ, dstOffset, null);
        
    }
    
    public CLCommandQueue putCopyImageToBuffer(CLImage3d<?> srcImage, CLBuffer<?> dstBuffer,
                                        int srcOriginX, int srcOriginY, int srcOriginZ, 
                                        int rangeX, int rangeY, int rangeZ, long dstOffset, CLEventList events) {

        copy2NIO(bufferA, srcOriginX, srcOriginY, srcOriginZ);
        copy2NIO(bufferB, rangeX, rangeY, rangeZ);

        int ret = cl.clEnqueueCopyImageToBuffer(ID, dstBuffer.ID, srcImage.ID,
                                         bufferA, bufferB, dstOffset,
                                         0, null, events==null ? null : events.IDs);
        checkForError(ret, "can not copy buffer to image3d");

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    public ByteBuffer putMapBuffer(CLBuffer<?> buffer, CLMemory.Map flag, boolean blockingMap) {
        return putMapBuffer(buffer, flag, blockingMap, null);
    }

    public ByteBuffer putMapBuffer(CLBuffer<?> buffer, CLMemory.Map flag, boolean blockingMap, CLEventList events) {
        return putMapBuffer(buffer, flag, 0, buffer.getCLSize(), blockingMap, events);
    }

    public ByteBuffer putMapBuffer(CLBuffer<?> buffer, CLMemory.Map flag, long offset, long length, boolean blockingMap) {
        return putMapBuffer(buffer, flag, offset, length, blockingMap, null);
    }

    public ByteBuffer putMapBuffer(CLBuffer<?> buffer, CLMemory.Map flag, long offset, long length, boolean blockingMap, CLEventList events) {
        IntBuffer error = bufferA.position(0).getBuffer().asIntBuffer();
        ByteBuffer mappedBuffer = cl.clEnqueueMapBuffer(ID, buffer.ID, blockingMap ? CL_TRUE : CL_FALSE,
                                         flag.FLAGS, offset, length,
                                         0, null, events==null ? null : events.IDs, error);
        checkForError(error.get(), "can not map buffer");

        if(events != null) {
            events.createEvent(context);
        }

        return mappedBuffer;
    }
/* TODO finish putMapImage
    // 2D
    public ByteBuffer putMapImage(CLImage2d<?> image, CLMemory.Map flag, boolean blockingMap) {
        return putMapImage(image, flag, blockingMap, null);
    }

    public ByteBuffer putMapImage(CLImage2d<?> image, CLMemory.Map flag, boolean blockingMap, CLEventList events) {
        return putMapImage(image, flag, 0, 0, image.width, image.height, blockingMap, events);
    }

    public ByteBuffer putMapImage(CLImage2d<?> buffer, CLMemory.Map flag, int offsetX, int offsetY,
                                    int rangeX, int rangeY, boolean blockingMap) {
        return putMapImage(buffer, flag, offsetX, offsetY, rangeX, rangeY, blockingMap, null);
    }

    public ByteBuffer putMapImage(CLImage2d<?> buffer, CLMemory.Map flag,
                                    int offsetX, int offsetY,
                                    int rangeX, int rangeY, boolean blockingMap, CLEventList events) {
        IntBuffer error = bufferA.position(0).getBuffer().asIntBuffer();

        // spec: CL_INVALID_VALUE if image is a 2D image object and origin[2] is not equal to 0 or region[2] is not equal to 1
        copy2NIO(bufferB, offsetX, offsetY, 0);
        copy2NIO(bufferC, rangeX, rangeY, 1);

        ByteBuffer mappedImage = cl.clEnqueueMapImage(ID, buffer.ID, blockingMap ? CL_TRUE : CL_FALSE,
                                         flag.FLAGS, bufferB, bufferC, null, null,
                                         0, null, events==null ? null : events.IDs, error);
        checkForError(error.get(), "can not map image2d");

        if(events != null) {
            events.createEvent(context);
        }

        return mappedImage;
    }

    // 3D
    public ByteBuffer putMapImage(CLImage3d<?> image, CLMemory.Map flag, boolean blockingMap) {
        return putMapImage(image, flag, blockingMap, null);
    }

    public ByteBuffer putMapImage(CLImage3d<?> image, CLMemory.Map flag, boolean blockingMap, CLEventList events) {
        return putMapImage(image, flag, 0, 0, 0, image.width, image.height, image.depth, blockingMap, events);
    }

    public ByteBuffer putMapImage(CLImage3d<?> image, CLMemory.Map flag,
                                    int offsetX, int offsetY, int offsetZ,
                                    int rangeX, int rangeY, int rangeZ, boolean blockingMap) {
        return putMapImage(image, flag, offsetX, offsetY, offsetZ, rangeX, rangeY, rangeZ, blockingMap, null);
    }

    public ByteBuffer putMapImage(CLImage3d<?> buffer, CLMemory.Map flag,
                                    int offsetX, int offsetY, int offsetZ,
                                    int rangeX, int rangeY, int rangeZ, boolean blockingMap, CLEventList events) {
        IntBuffer error = bufferA.position(0).getBuffer().asIntBuffer();
        copy2NIO(bufferB, offsetX, offsetY, offsetZ);
        copy2NIO(bufferC, rangeX, rangeY, rangeZ);
        ByteBuffer mappedImage = cl.clEnqueueMapImage(ID, buffer.ID, blockingMap ? CL_TRUE : CL_FALSE,
                                         flag.FLAGS, bufferB, bufferC, null, null,
                                         0, null, events==null ? null : events.IDs, error);
        checkForError(error.get(), "can not map image3d");

        if(events != null) {
            events.createEvent(context);
        }

        return mappedImage;
    }
*/
    public CLCommandQueue putUnmapMemory(CLMemory<?> memory) {
        return putUnmapMemory(memory, null);
    }

    public CLCommandQueue putUnmapMemory(CLMemory<?> memory, CLEventList events) {
        int ret = cl.clEnqueueUnmapMemObject(ID, memory.ID, memory.getBuffer(),
                                        0, null, events==null ? null : events.IDs);
        checkForError(ret, "can not unmap memory");

        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    public CLCommandQueue putMarker(CLEventList events) {
        int ret = cl.clEnqueueMarker(CL_INT_MIN, events.IDs);
        checkForError(ret, "can not enqueue marker");
        return this;
    }

    public CLCommandQueue putWaitForEvent(CLEventList list, int index, boolean blockingWait) {
        int marker = list.IDs.position()-1;
        list.IDs.position(index);
        int ret = blockingWait ? cl.clWaitForEvents(1, list.IDs)
                               : cl.clEnqueueWaitForEvents(ID, 1, list.IDs);
        list.IDs.position(marker);
        checkForError(ret, "error while waiting for events");
        return this;
    }

    public CLCommandQueue putWaitForEvents(CLEventList list, boolean blockingWait) {
        list.IDs.rewind();
        int ret = blockingWait ? cl.clWaitForEvents(list.size, list.IDs)
                               : cl.clEnqueueWaitForEvents(ID, list.size, list.IDs);
        checkForError(ret, "error while waiting for events");
        return this;
    }

    public CLCommandQueue putBarrier() {
        int ret = cl.clEnqueueBarrier(ID);
        checkForError(ret, "can not enqueue Barrier");
        return this;
    }

    /**
     * {@link #putTask} equivalent to calling
     * {@link #put1DRangeKernel(CLKernel kernel, long globalWorkOffset, long globalWorkSize, long localWorkSize)}
     * with globalWorkOffset = null, globalWorkSize set to 1, and localWorkSize set to 1.
     */
    public CLCommandQueue putTask(CLKernel kernel) {
        int ret = cl.clEnqueueTask(ID, kernel.ID, 0, null, null);
        checkForError(ret, "can not enqueue Task");
        return this;
    }

    /**
     * @see #putTask(com.mbien.opencl.CLKernel)
     */
    public CLCommandQueue putTask(CLKernel kernel, CLEventList events) {
        int ret = cl.clEnqueueTask(ID, kernel.ID, 0, null, events==null ? null : events.IDs);
        checkForError(ret, "can not enqueue Task");
        if(events != null) {
            events.createEvent(context);
        }
        return this;
    }

    public CLCommandQueue put1DRangeKernel(CLKernel kernel, long globalWorkOffset, long globalWorkSize, long localWorkSize) {
        this.put1DRangeKernel(kernel, globalWorkOffset, globalWorkSize, localWorkSize, null);
        return this;
    }

    public CLCommandQueue put1DRangeKernel(CLKernel kernel, long globalWorkOffset, long globalWorkSize, long localWorkSize, CLEventList events) {
        PointerBuffer globWO = null;
        PointerBuffer globWS = null;
        PointerBuffer locWS = null;

        if(globalWorkOffset != 0) {
            globWO = copy2NIO(bufferA, globalWorkOffset);
        }
        if(globalWorkSize != 0) {
            globWS = copy2NIO(bufferB, globalWorkSize);
        }
        if(globalWorkSize != 0) {
            locWS = copy2NIO(bufferC, localWorkSize);
        }

        this.putNDRangeKernel(kernel, 1, globWO, globWS, locWS, events);
        return this;
    }

    public CLCommandQueue put2DRangeKernel(CLKernel kernel, long globalWorkOffsetX, long globalWorkOffsetY,
                                                            long globalWorkSizeX, long globalWorkSizeY,
                                                            long localWorkSizeX, long localWorkSizeY) {
        this.put2DRangeKernel(kernel,
                globalWorkOffsetX, globalWorkOffsetY,
                globalWorkSizeX, globalWorkSizeY,
                localWorkSizeX, localWorkSizeY, null);

        return this;
    }

    public CLCommandQueue put2DRangeKernel(CLKernel kernel, long globalWorkOffsetX, long globalWorkOffsetY,
                                                            long globalWorkSizeX, long globalWorkSizeY,
                                                            long localWorkSizeX, long localWorkSizeY, CLEventList events) {
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
        this.putNDRangeKernel(kernel, 2, globalWorkOffset, globalWorkSize, localWorkSize, events);
        return this;
    }

    public CLCommandQueue putNDRangeKernel(CLKernel kernel, int workDimension, PointerBuffer globalWorkOffset, PointerBuffer globalWorkSize, PointerBuffer localWorkSize) {
        this.putNDRangeKernel(kernel, workDimension, globalWorkOffset, globalWorkSize, localWorkSize, null);
        return this;
    }

    public CLCommandQueue putNDRangeKernel(CLKernel kernel, int workDimension, PointerBuffer globalWorkOffset, PointerBuffer globalWorkSize, PointerBuffer localWorkSize, CLEventList events) {

       int ret = cl.clEnqueueNDRangeKernel(
                ID, kernel.ID, workDimension,
                globalWorkOffset,
                globalWorkSize, 
                localWorkSize, 
                0, null,
                events==null ? null : events.IDs);

        if(ret != CL_SUCCESS)
            throw new CLException(ret, "can not enqueue NDRangeKernel: " + kernel);

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    public CLCommandQueue putAcquireGLObject(long glObject) {
        this.putAcquireGLObject(glObject, null);
        return this;
    }

    public CLCommandQueue putAcquireGLObject(long glObject, CLEventList events) {
        CLGLI xl = (CLGLI) cl;

        PointerBuffer glObj = copy2NIO(bufferA, glObject);
        
        int ret = xl.clEnqueueAcquireGLObjects(ID, 1, glObj, 0, null,
                    events==null ? null : events.IDs);

        if(ret != CL_SUCCESS)
            throw new CLException(ret, "can not aquire GLObject: " + glObject);

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    public CLCommandQueue putReleaseGLObject(long glObject) {
        this.putReleaseGLObject(glObject, null);
        return this;
    }

    public CLCommandQueue putReleaseGLObject(long glObject, CLEventList events) {
        CLGLI xl = (CLGLI) cl;

        PointerBuffer glObj = copy2NIO(bufferA, glObject);

        int ret = xl.clEnqueueReleaseGLObjects(ID, 1, glObj, 0, null,
                events==null ? null : events.IDs);

        if(ret != CL_SUCCESS)
            throw new CLException(ret, "can not release GLObject: " + glObject);

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    public CLCommandQueue finish() {
        int ret = cl.clFinish(ID);
        checkForError(ret, "can not finish command queue");
        return this;
    }

    /**
     * Returns true only when {@link Mode#PROFILING_MODE} has been enabled.
     */
    public boolean isProfilingEnabled() {
        return (Mode.PROFILING_MODE.QUEUE_MODE & properties) != 0;
    }

    /**
     * Returns true only when {@link Mode#OUT_OF_ORDER_EXEC_MODE} mode has been enabled.
     */
    public boolean isOutOfOrderModeEnabled() {
        return (Mode.OUT_OF_ORDER_EXEC_MODE.QUEUE_MODE & properties) != 0;
    }

    public void release() {
        int ret = cl.clReleaseCommandQueue(ID);
        context.onCommandQueueReleased(device, this);
        checkForError(ret, "can not release command queue");
    }

    private final static PointerBuffer copy2NIO(PointerBuffer buffer, long a) {
        return buffer.put(2, a).position(2);
    }

    private final static PointerBuffer copy2NIO(PointerBuffer buffer, long a, long b) {
        return buffer.position(1).put(a).put(b).position(1);
    }

    private final static PointerBuffer copy2NIO(PointerBuffer buffer, long a, long b, long c) {
        return buffer.rewind().put(a).put(b).put(c).rewind();
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
        OUT_OF_ORDER_EXEC_MODE(CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE),

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
                    return OUT_OF_ORDER_EXEC_MODE;
                case(CL_QUEUE_PROFILING_ENABLE):
                    return PROFILING_MODE;
            }
            return null;
        }

        public static EnumSet<Mode> valuesOf(int bitfield) {
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
