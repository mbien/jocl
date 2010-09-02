/*
 * Created on Thursday, September 02 2010 22:01
 */
package com.jogamp.opencl;

/**
 * A callback for a specific command execution status.
 * @author Michael Bien
 * @see CLEvent#registerCallback(com.jogamp.opencl.CLEventListener) 
 */
public interface CLEventListener {

    public void eventStateChanged(CLEvent event, int status);

}