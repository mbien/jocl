package com.mbien.opencl;

import static com.mbien.opencl.CLException.*;

/**
 *
 * @author Michael Bien
 */
public class CLCommandQueue {

    public final long ID;
    private final CLContext context;
    private final CL cl;

    CLCommandQueue(CLContext context, CLDevice device, long properties) {
        this.context = context;
        this.cl = context.cl;

        int[] status = new int[1];
        this.ID = cl.clCreateCommandQueue(context.ID, device.ID, properties, status, 0);

        if(status[0] != CL.CL_SUCCESS)
            throw new CLException(status[0], "can not create command queue on "+device);
    }

    public CLCommandQueue putWriteBuffer(CLBuffer writeBuffer, boolean blockingWrite) {

        int ret = cl.clEnqueueWriteBuffer(
                ID, writeBuffer.ID, blockingWrite ? CL.CL_TRUE : CL.CL_FALSE,
                0, writeBuffer.buffer.capacity(), writeBuffer.buffer,
                0, null, 0,
                null, 0 );

        if(ret != CL.CL_SUCCESS)
            throw new CLException(ret, "can not enqueue WriteBuffer: " + writeBuffer);

        return this;
    }

    public CLCommandQueue putReadBuffer(CLBuffer readBuffer, boolean blockingRead) {

        int ret = cl.clEnqueueReadBuffer(
                ID, readBuffer.ID, blockingRead ? CL.CL_TRUE : CL.CL_FALSE,
                0, readBuffer.buffer.capacity(), readBuffer.buffer,
                0, null, 0,
                null, 0 );

        if(ret != CL.CL_SUCCESS)
            throw new CLException(ret, "can not enqueue ReadBuffer: " + readBuffer);
        
        return this;
    }

    public CLCommandQueue putNDRangeKernel(CLKernel kernel, int workDimension, long[] globalWorkOffset, long[] globalWorkSize, long[] localWorkSize) {

       int ret = cl.clEnqueueNDRangeKernel(
                ID, kernel.ID, 1,
                null, 0,
                globalWorkSize, 0,
                localWorkSize, 0,
                0,
                null, 0,
                null, 0 );

        if(ret != CL.CL_SUCCESS)
            throw new CLException(ret, "can not enqueue NDRangeKernel: " + kernel);

        return this;
    }

    public void release() {
        int ret = cl.clReleaseCommandQueue(ID);
        checkForError(ret, "can not release command queue");
    }


}
