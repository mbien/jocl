/*
 * Created on Tuesday, July 06 2010 00:46
 */

package com.jogamp.opencl.impl;

/**
 * A callback for a specific command execution status.
 * @author Michael Bien
 */
public interface CLEventCallback {

    public void eventStateChanged(long event, int status);

}
