package com.jogamp.opencl;

/**
 * Releasable OpenCL resource.
 * @author Michael Bien
 */
public interface CLResource extends Disposable<CLException> {

    /**
     * Releases the OpenCL resource.
     */
    public void release();

    /**
     * Calls {@link #release()};
     * @see #release()
     */
    @Override public void close();

}
