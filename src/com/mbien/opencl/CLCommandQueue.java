package com.mbien.opencl;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import static com.mbien.opencl.CLException.*;

/**
 * The command-queue can be used to queue a set of operations in order. Having multiple
 * command-queues allows applications to queue multiple independent commands without
 * requiring synchronization. Note that this should work as long as these objects are
 * not being shared.<b/>
 * Sharing of objects across multiple command-queues will require the application to
 * perform appropriate synchronization.
 * @author Michael Bien
 */
public class CLCommandQueue {

    public final long ID;
    private final CLContext context;
    private final CLDevice device;
    private final CL cl;

    CLCommandQueue(CLContext context, CLDevice device, long properties) {
        this.context = context;
        this.cl = context.cl;
        this.device = device;

        int[] status = new int[1];
        this.ID = cl.clCreateCommandQueue(context.ID, device.ID, properties, status, 0);

        if(status[0] != CL.CL_SUCCESS)
            throw new CLException(status[0], "can not create command queue on "+device);
    }

    public CLCommandQueue putWriteBuffer(CLBuffer<?> writeBuffer, boolean blockingWrite) {

        int ret = cl.clEnqueueWriteBuffer(
                ID, writeBuffer.ID, blockingWrite ? CL.CL_TRUE : CL.CL_FALSE,
                0, writeBuffer.getSizeInBytes(), writeBuffer.buffer,
//                0, null, null); //TODO solve NPE in gluegen when PointerBuffer == null (fast dircet memory path)
                0, null, 0, null, 0); //TODO events

        if(ret != CL.CL_SUCCESS)
            throw new CLException(ret, "can not enqueue WriteBuffer: " + writeBuffer);

        return this;
    }

    public CLCommandQueue putReadBuffer(CLBuffer<?> readBuffer, boolean blockingRead) {

        int ret = cl.clEnqueueReadBuffer(
                ID, readBuffer.ID, blockingRead ? CL.CL_TRUE : CL.CL_FALSE,
                0, readBuffer.getSizeInBytes(), readBuffer.buffer,
//                0, null, null); //TODO solve NPE in gluegen when PointerBuffer == null (fast dircet memory path)
                0, null, 0, null, 0); //TODO events

        if(ret != CL.CL_SUCCESS)
            throw new CLException(ret, "can not enqueue ReadBuffer: " + readBuffer);
        
        return this;
    }

    public CLCommandQueue putReadBuffer(CLBuffer<?> readBuffer, Buffer buffer, boolean blockingRead) {

        int ret = cl.clEnqueueReadBuffer(
                ID, readBuffer.ID, blockingRead ? CL.CL_TRUE : CL.CL_FALSE,
                0, readBuffer.getSizeInBytes(), buffer,
//                0, null, null); //TODO solve NPE in gluegen when PointerBuffer == null (fast dircet memory path)
                0, null, 0, null, 0); //TODO events

        if(ret != CL.CL_SUCCESS)
            throw new CLException(ret, "can not enqueue ReadBuffer: " + readBuffer);

        return this;
    }

    public CLCommandQueue putBarrier() {
        int ret = cl.clEnqueueBarrier(ID);
        checkForError(ret, "can not enqueue Barrier");
        return this;
    }

    public CLCommandQueue putCopyBuffer(CLBuffer<?> src, CLBuffer<?> dest, long bytesToCopy) {
        int ret = cl.clEnqueueCopyBuffer(
                        ID, src.ID, dest.ID, src.buffer.position(), dest.buffer.position(), bytesToCopy,
//                      0, null, null); //TODO solve NPE in gluegen when PointerBuffer == null
                        0, null, 0, null, 0); //TODO events
        checkForError(ret, "can not copy Buffer");
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
    public CLCommandQueue putMarker() {

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

    public CLCommandQueue putWaitForEvents() {

        return this;
    }
*/

    public CLCommandQueue putNDRangeKernel(CLKernel kernel, int workDimension, long globalWorkOffset, long globalWorkSize, long localWorkSize) {
        return this.putNDRangeKernel(
                kernel, workDimension,
                globalWorkOffset==0 ? null : new long[] {globalWorkOffset},
                globalWorkSize  ==0 ? null : new long[] {globalWorkSize  },
                localWorkSize   ==0 ? null : new long[] {localWorkSize   }  );
    }

    public CLCommandQueue putNDRangeKernel(CLKernel kernel, int workDimension, long[] globalWorkOffset, long[] globalWorkSize, long[] localWorkSize) {

       int ret = cl.clEnqueueNDRangeKernel(
                ID, kernel.ID, workDimension,
                globalWorkOffset, 0,
                globalWorkSize, 0,
                localWorkSize, 0,
                0,
                null, 0,
                null, 0 );

        if(ret != CL.CL_SUCCESS)
            throw new CLException(ret, "can not enqueue NDRangeKernel: " + kernel);

        return this;
    }


    public CLCommandQueue putAcquireGLObject(long glObject) {
        CLGLI xl = (CLGLI) cl;
        int ret = xl.clEnqueueAcquireGLObjects(ID, 1, new long[] {glObject}, 0, 0, null, 0, null, 0);

        if(ret != CL.CL_SUCCESS)
            throw new CLException(ret, "can not aquire GLObject: " + glObject);

        return this;
    }

    public CLCommandQueue putReleaseGLObject(long glObject) {
        CLGLI xl = (CLGLI) cl;
        int ret = xl.clEnqueueReleaseGLObjects(ID, 1, new long[] {glObject}, 0, 0, null, 0, null, 0);

        if(ret != CL.CL_SUCCESS)
            throw new CLException(ret, "can not release GLObject: " + glObject);

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
        OUT_OF_ORDER_EXEC_MODE(CL.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE),
        /**
         * CL_DEVICE_TYPE_GPU
         */
        PROFILING_MODE(CL.CL_QUEUE_PROFILING_ENABLE);

        /**
         * Value of wrapped OpenCL device type.
         */
        public final int CL_QUEUE_MODE;

        private Mode(int CL_VALUE) {
            this.CL_QUEUE_MODE = CL_VALUE;
        }

        public static Mode valueOf(int queueMode) {
            switch(queueMode) {
                case(CL.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE):
                    return OUT_OF_ORDER_EXEC_MODE;
                case(CL.CL_QUEUE_PROFILING_ENABLE):
                    return PROFILING_MODE;
            }
            return null;
        }

        public static EnumSet<Mode> valuesOf(int bitfield) {
            List<Mode> matching = new ArrayList<Mode>();
            Mode[] values = Mode.values();
            for (Mode value : values) {
                if((value.CL_QUEUE_MODE & bitfield) != 0)
                    matching.add(value);
            }
            if(matching.isEmpty())
                return EnumSet.noneOf(Mode.class);
            else
                return EnumSet.copyOf(matching);
        }

    }
}
