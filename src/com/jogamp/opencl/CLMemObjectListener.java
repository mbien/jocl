/*
 * Created on Tuesday, July 06 2010 00:46
 */

package com.jogamp.opencl;

/**
 * A callback which is invoked by the OpenCL implementation when the memory
 * object is deleted and its resources freed.
 * @author Michael Bien
 */
public interface CLMemObjectListener {

    /**
     * 
     */
    public void memoryDeallocated(CLMemory<?> mem);

}
