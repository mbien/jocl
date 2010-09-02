/*
 * Created on Thursday, September 02 2010 23:09
 */

package com.jogamp.opencl.impl;

/**
 * A callback which is invoked by the OpenCL implementation when the memory
 * object is deleted and its resources freed.
 * @author Michael Bien
 */
public interface CLMemObjectDestructorCallback {

    /**
     * 
     */
    public void memoryDeallocated(long memObjID);

}
