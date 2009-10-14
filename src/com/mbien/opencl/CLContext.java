package com.mbien.opencl;

import com.mbien.opencl.impl.CLImpl;
import com.sun.gluegen.runtime.PointerBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Michael Bien
 */
public final class CLContext {

    private final static CL cl;
    public final long contextID;

    static{
        System.loadLibrary("gluegen-rt");
        System.loadLibrary("jocl");
        cl = new CLImpl();
    }

    private CLContext(long contextID) {
        this.contextID = contextID;
    }

    /**
     * Creates a default context on all available devices.
     */
    public static CLContext create() {
        IntBuffer ib = IntBuffer.allocate(1);
        long context = cl.clCreateContextFromType(null, 0, CL.CL_DEVICE_TYPE_ALL, null, null, null, 0);

//        int errorCode = ib.get();
//        if(errorCode != CL.CL_SUCCESS)
//            throw new CLException(errorCode, "can not create CL context");

        return new CLContext(context);
    }

    /**
     * Creates a default context on the specified device types.
     */
    public static CLContext create(CLDevice.Type... deviceTypes) {

        int type = deviceTypes[0].CL_TYPE;
        for (int i = 1; i < deviceTypes.length; i++) {
            type |= deviceTypes[i].CL_TYPE;
        }

        long ctxID = cl.clCreateContextFromType(null, 0, type, null, null, null, 0);
        return new CLContext(ctxID);
    }

    /**
     * Releases the context and all resources.
     */
    public void release() {
        int ret = cl.clReleaseContext(contextID);
        if(CL.CL_SUCCESS != ret)
            throw new CLException(ret, "error releasing context");
    }

    /**
     * Gets the device with maximal FLOPS from this context.
     */
    /*
    public CLDevice getMaxFlopsDevice() {

        long[] longBuffer = new long[1];
//        ByteBuffer bb = ByteBuffer.allocate(8);
//        bb.order(ByteOrder.nativeOrder());

        int ret = cl.clGetContextInfo(contextID, CL.CL_CONTEXT_DEVICES, 0, null, longBuffer, 0);
        if(CL.CL_SUCCESS != ret)
            throw new CLException(ret, "can not receive context info");

        System.out.println("#devices: "+longBuffer[0]);

        long[] deviceIDs = new long[(int)longBuffer[0]];
        ret = cl.clGetContextInfo(contextID, CL.CL_CONTEXT_DEVICES, 0, null, deviceIDs, 0);

        if(CL.CL_SUCCESS != ret)
            throw new CLException(ret, "can not receive context info");

        for (int i = 0; i < deviceIDs.length; i++) {
            long l = deviceIDs[i];
            System.out.println("device id"+l);
        }

            // get the list of GPU devices associated with context
//        ciErrNum = clGetContextInfo(cxGPUContext, CL_CONTEXT_DEVICES, 0, NULL, &dataBytes);
//        cl_device_id *cdDevices = (cl_device_id *)malloc(dataBytes);
//        ciErrNum |= clGetContextInfo(cxGPUContext, CL_CONTEXT_DEVICES, dataBytes, cdDevices, NULL);
//        shrCheckError(ciErrNum, CL_SUCCESS);

        return null;
    }

    public CLDevice[] getCLDevices() {

    }
*/


    /**
     * Lists all available OpenCL implementaitons.
     * @throws CLException if something went wrong initializing OpenCL
     */
    public static CLPlatform[] listCLPlatforms() {

        int[] intBuffer = new int[1];
        // find all available OpenCL platforms
        int ret = cl.clGetPlatformIDs(0, null, 0, intBuffer, 0);
        if(CL.CL_SUCCESS != ret)
            throw new CLException(ret, "can not enumerate platforms");

        // receive platform ids
        long[] platformId = new long[intBuffer[0]];
        ret = cl.clGetPlatformIDs(platformId.length, platformId, 0, null, 0);
        if(CL.CL_SUCCESS != ret)
            throw new CLException(ret, "can not enumerate platforms");

        CLPlatform[] platforms = new CLPlatform[platformId.length];

        for (int i = 0; i < platformId.length; i++)
            platforms[i] = new CLPlatform(cl, platformId[i]);

        return platforms;
    }

    /**
     * Returns the low level binding interface to the OpenCL APIs.
     */
    public static CL getLowLevelBinding() {
        return cl;
    }

}
