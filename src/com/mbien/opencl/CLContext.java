package com.mbien.opencl;

import com.mbien.opencl.impl.CLImpl;

/**
 *
 * @author Michael Bien
 */
public class CLContext {

    private final static CL cl;

    static{
        System.loadLibrary("gluegen-rt");
        System.loadLibrary("jocl");
        cl = new CLImpl();
    }

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

}
