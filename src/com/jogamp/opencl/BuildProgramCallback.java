package com.jogamp.opencl;

/**
 * @author Michael Bien
 */
// TODO implement callbacks
public interface BuildProgramCallback {
    
    public void buildProgramCallback(long cl_program, Object user_data);
}
