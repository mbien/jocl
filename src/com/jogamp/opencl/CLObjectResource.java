/*
 * Created on Saturday, June 18 2011 02:36
 */
package com.jogamp.opencl;

import com.jogamp.common.AutoCloseable;

/**
 * Releasable resource with an CL object ID.
 * @author Michael Bien
 */
abstract class CLObjectResource extends CLObject implements CLResource, AutoCloseable {

    private boolean released;

    public CLObjectResource(long ID) {
        super(ID);
    }

    public CLObjectResource(CLContext context, long ID) {
        super(context, ID);
    }

    public void release() {
        if(released) {
            throw new RuntimeException(getClass().getSimpleName()+" was already released.");
        }else{
            released = true;
        }
    }

    /**
     * Implementation detail.
     * TODO remove as soon we have extension methods.
     * @deprecated This method is not intended to be called from client code.
     * @see java.lang.AutoCloseable
     */
    @Deprecated
    @Override
    public final void close() {
        if(this instanceof CLResource) {
            ((CLResource)this).release();
        }
    }

    public boolean isReleased() {
        return released;
    }
    

}
