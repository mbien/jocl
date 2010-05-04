package com.jogamp.opencl;

import java.nio.ByteBuffer;

/**
 * Experimental: the api may change in future, feedback appreciated.<br/>
 * Note: the thread which calls {@link #onError onError} is unspecified. The Application must ensure propper synchronization.
 * @author Michael Bien
 * @see CLContext#addCLErrorHandler(com.jogamp.opencl.CLErrorHandler)
 * @see CLContext#removeCLErrorHandler(com.jogamp.opencl.CLErrorHandler)
 */
public interface CLErrorHandler {

    public void onError(String errinfo, ByteBuffer private_info, long cb);

}
