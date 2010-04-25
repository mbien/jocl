package com.jogamp.opencl;

/**
 * A callback an application can register to be called when the program executable
 * has been built (successfully or unsuccessfully).<br/>
 * Note1: registering a build callback can make {@link CL#clBuildProgram} non blocking (OpenCL implementation dependent).<br/>
 * Note2: the thread which calls this method is unspecified. The Application should ensure propper synchronization.
 * @author Michael Bien
 * @see CL#clBuildProgram(long, int, com.jogamp.common.nio.PointerBuffer, java.lang.String, com.jogamp.opencl.BuildProgramCallback)
 */
public interface BuildProgramCallback {

    /**
     * Called when the program executable
     * has been built (successfully or unsuccessfully).
     */
    public void buildProgramCallback(long cl_program);
    
}
