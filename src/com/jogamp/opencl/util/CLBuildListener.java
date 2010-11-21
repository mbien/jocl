/*
 * Sunday, May 02 2010 20:38
 */

package com.jogamp.opencl.util;

import com.jogamp.opencl.CLProgram;

/**
 * A callback an application can register to be called when the program executable
 * has been built (successfully or unsuccessfully).<br/>
 * Note1: registering a build callback can make {@link com.jogamp.opencl.CL#clBuildProgram} non blocking (OpenCL implementation dependent).<br/>
 * Note2: the thread which calls this method is unspecified. The Application should ensure propper synchronization.
 * @author Michael Bien
 * @see com.jogamp.opencl.CL#clBuildProgram(long, int, com.jogamp.common.nio.PointerBuffer, java.lang.String, com.jogamp.opencl.impl.BuildProgramCallback) 
 */
public interface CLBuildListener {

    /**
     * Called when the program executable
     * has been built (successfully or unsuccessfully).
     */
    public void buildFinished(CLProgram program);

}
