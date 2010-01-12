package com.mbien.opencl;

import com.sun.gluegen.runtime.PointerBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.mbien.opencl.CLException.*;
import static com.mbien.opencl.CL.*;

/**
 * The command-queue can be used to queue a set of operations in order. Having multiple
 * command-queues allows applications to queue multiple independent commands without
 * requiring synchronization. Note that this should work as long as these objects are
 * not being shared.<b/>
 * Sharing of objects across multiple command-queues or using a CLCommandQueue
 * form multiple Threads will require the application to perform appropriate synchronization.<b/>
 * @author Michael Bien
 */
public class CLCommandQueue implements CLResource {

    public final long ID;
    private final CLContext context;
    private final CLDevice device;
    private final CL cl;

    /*
     * Those direct memory buffers are used to pass data between the JVM and OpenCL.
     */
    private final PointerBuffer bufferA;
    private final PointerBuffer bufferB;
    private final PointerBuffer bufferC;

    CLCommandQueue(CLContext context, CLDevice device, long properties) {
        this.context = context;
        this.cl = context.cl;
        this.device = device;

        this.bufferA = PointerBuffer.allocateDirect(2);
        this.bufferB = PointerBuffer.allocateDirect(2);
        this.bufferC = PointerBuffer.allocateDirect(2);

        int[] status = new int[1];
        this.ID = cl.clCreateCommandQueue(context.ID, device.ID, properties, status, 0);

        if(status[0] != CL_SUCCESS)
            throw new CLException(status[0], "can not create command queue on "+device);
    }

    public CLCommandQueue putWriteBuffer(CLBuffer<?> writeBuffer, boolean blockingRead) {
        return putWriteBuffer(writeBuffer, null, blockingRead);
    }

    public CLCommandQueue putWriteBuffer(CLBuffer<?> writeBuffer, CLEventList events, boolean blockingWrite) {
        PointerBuffer pb = PointerBuffer.allocateDirect(2);

        int ret = cl.clEnqueueWriteBuffer(
                ID, writeBuffer.ID, blockingWrite ? CL_TRUE : CL_FALSE,
                0, writeBuffer.getSizeInBytes(), writeBuffer.buffer,
                0, null, events==null ? null : events.IDs);

        if(ret != CL_SUCCESS)
            throw new CLException(ret, "can not enqueue WriteBuffer: " + writeBuffer);

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }

    public CLCommandQueue putReadBuffer(CLBuffer<?> readBuffer, boolean blockingRead) {
        putReadBuffer(readBuffer, null, blockingRead);
        return this;
    }

    public CLCommandQueue putReadBuffer(CLBuffer<?> readBuffer, CLEventList events, boolean blockingRead) {

        int ret = cl.clEnqueueReadBuffer(
                ID, readBuffer.ID, blockingRead ? CL_TRUE : CL_FALSE,
                0, readBuffer.getSizeInBytes(), readBuffer.buffer,
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

    public CLCommandQueue putCopyBuffer(CLBuffer<?> src, CLBuffer<?> dest, long bytesToCopy) {
        return putCopyBuffer(src, dest, bytesToCopy, null);
    }

    public CLCommandQueue putCopyBuffer(CLBuffer<?> src, CLBuffer<?> dest, long bytesToCopy, CLEventList events) {

        int ret = cl.clEnqueueCopyBuffer(
                        ID, src.ID, dest.ID, src.buffer.position(), dest.buffer.position(), bytesToCopy,
                        0, null, events==null ? null : events.IDs);

        checkForError(ret, "can not copy Buffer");

        if(events != null) {
            events.createEvent(context);
        }

        return this;
    }
    
    //TODO implement remaining methods
    /*
    public CLCommandQueue putCopyImage() {

        return this;
    }
    public CLCommandQueue putCopyBufferToImage() {

        return this;
    }
    public CLCommandQueue putCopyImageToBuffer() {

        return this;
    }

    public CLCommandQueue putWriteImage() {

        return this;
    }

    public CLCommandQueue putReadImage() {

        return this;
    }

    public CLCommandQueue putTask() {

        return this;
    }

    public CLBuffer putMapBuffer() {

        return null;
    }

    public CLCommandQueue putMapImage() {

        return this;
    }

    public CLCommandQueue putUnmapMemObject() {

        return this;
    }
*/
    public CLCommandQueue putMarker(CLEventList events) {
        int ret = cl.clEnqueueMarker(CL_INT_MIN, events.IDs);
        checkForError(ret, "can not enqueue marker");
        return this;
    }

    public CLCommandQueue putWaitForEvent(CLEventList list, int index) {
        int marker = list.IDs.position()-1;
        list.IDs.position(index);
        int ret = cl.clWaitForEvents(1, list.IDs);
        list.IDs.position(marker);
        checkForError(ret, "error while waiting for events");
        return this;
    }

    public CLCommandQueue putWaitForEvents(CLEventList list) {
        list.IDs.rewind();
        int ret = cl.clWaitForEvents(list.size, list.IDs);
        checkForError(ret, "error while waiting for events");
        return this;
    }

    public CLCommandQueue putBarrier() {
        int ret = cl.clEnqueueBarrier(ID);
        checkForError(ret, "can not enqueue Barrier");
        return this;
    }

    public CLCommandQueue put1DRangeKernel(CLKernel kernel, long globalWorkOffset, long globalWorkSize, long localWorkSize) {
        this.put1DRangeKernel(kernel, null, globalWorkOffset, globalWorkSize, localWorkSize);
        return this;
    }

    public CLCommandQueue put1DRangeKernel(CLKernel kernel, CLEventList events, long globalWorkOffset, long globalWorkSize, long localWorkSize) {
        PointerBuffer globWO = null;
        PointerBuffer globWS = null;
        PointerBuffer locWS = null;

        if(globalWorkOffset != 0) {
            globWO = bufferA.put(1, globalWorkOffset).position(1);
        }
        if(globalWorkSize != 0) {
            globWS = bufferB.put(1, globalWorkSize).position(1);
        }
        if(globalWorkSize != 0) {
            locWS = bufferC.put(1, localWorkSize).position(1);
        }

        this.putNDRangeKernel(kernel, events, 1, globWO, globWS, locWS);
        return this;
    }

    public CLCommandQueue put2DRangeKernel(CLKernel kernel, long globalWorkOffsetX, long globalWorkOffsetY,
                                                            long globalWorkSizeX, long globalWorkSizeY,
                                                            long localWorkSizeX, long localWorkSizeY) {
        this.put2DRangeKernel(kernel, null,
                globalWorkOffsetX, globalWorkOffsetY,
                globalWorkSizeX, globalWorkSizeY,
                localWorkSizeX, localWorkSizeY);

        return this;
    }

    public CLCommandQueue put2DRangeKernel(CLKernel kernel, CLEventList events,
                                                            long globalWorkOffsetX, long globalWorkOffsetY,
                                                            long globalWorkSizeX, long globalWorkSizeY,
                                                            long localWorkSizeX, long localWorkSizeY) {
        PointerBuffer globalWorkOffset = null;
        PointerBuffer globalWorkSize = null;
        PointerBuffer localWorkSize = null;

        if(globalWorkOffsetX != 0 && globalWorkOffsetY != 0) {
            globalWorkOffset = bufferA.put(globalWorkOffsetX).put(globalWorkOffsetY).rewind();
        }
        if(globalWorkSizeX != 0 && globalWorkSizeY != 0) {
            globalWorkSize = bufferB.put(globalWorkSizeX).put(globalWorkSizeY).rewind();
        }
        if(localWorkSizeX != 0 && localWorkSizeY !=0) {
            localWorkSize = bufferC.put(localWorkSizeX).put(localWorkSizeY).rewind();
        }
        this.putNDRangeKernel(kernel, 2, globalWorkOffset, globalWorkSize, localWorkSize);
        return this;
    }

    public CLCommandQueue putNDRangeKernel(CLKernel kernel, int workDimension, PointerBuffer globalWorkOffset, PointerBuffer globalWorkSize, PointerBuffer localWorkSize) {
        this.putNDRangeKernel(kernel, null, workDimension, globalWorkOffset, globalWorkSize, localWorkSize);
        return this;
    }

    public CLCommandQueue putNDRangeKernel(CLKernel kernel, CLEventList events, int workDimension, PointerBuffer globalWorkOffset, PointerBuffer globalWorkSize, PointerBuffer localWorkSize) {

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

        PointerBuffer glObj = bufferA.put(1, glObject).position(1);
        
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

        PointerBuffer glObj = bufferA.put(1, glObject).position(1);

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

    public void release() {
        int ret = cl.clReleaseCommandQueue(ID);
        context.onCommandQueueReleased(device, this);
        checkForError(ret, "can not release command queue");
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
         * CL_DEVICE_TYPE_CPU
         */
        OUT_OF_ORDER_EXEC_MODE(CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE),
        /**
         * CL_DEVICE_TYPE_GPU
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
