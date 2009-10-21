package com.mbien.opencl;

import static com.mbien.opencl.CLException.*;

/**
 *
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

    public CLCommandQueue putWriteBuffer(CLBuffer writeBuffer, boolean blockingWrite) {

        int ret = cl.clEnqueueWriteBuffer(
                ID, writeBuffer.ID, blockingWrite ? CL.CL_TRUE : CL.CL_FALSE,
                0, writeBuffer.buffer.capacity(), writeBuffer.buffer,
//                0, null, null); //TODO solve NPE in gluegen when PointerBuffer == null (fast dircet memory path)
                0, null, 0, null, 0); //TODO events

        if(ret != CL.CL_SUCCESS)
            throw new CLException(ret, "can not enqueue WriteBuffer: " + writeBuffer);

        return this;
    }

    public CLCommandQueue putReadBuffer(CLBuffer readBuffer, boolean blockingRead) {

        int ret = cl.clEnqueueReadBuffer(
                ID, readBuffer.ID, blockingRead ? CL.CL_TRUE : CL.CL_FALSE,
                0, readBuffer.buffer.capacity(), readBuffer.buffer,
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

    public CLCommandQueue putCopyBuffer(CLBuffer src, CLBuffer dest, long bytesToCopy) {
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
                ID, kernel.ID, 1,
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


}
