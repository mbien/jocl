package com.mbien.opencl;

/**
 * Releasable OpenCL resource.
 * @author Michael Bien
 */
public interface CLResource {

    /**
     * Releases the OpenCL resource.
     */
    public void release();

}
