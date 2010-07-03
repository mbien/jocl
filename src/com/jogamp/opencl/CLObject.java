package com.jogamp.opencl;

/**
 * Common superclass for all OpenCL objects.
 * @author Michael Bien
 */
abstract class CLObject implements AutoCloseable {

    /**
     * The OpenCL object handle.
     */
    public final long ID;
    
    protected CLContext context;

    protected final CL cl;

    CLObject(CL cl, long ID) {
        this.cl = cl;
        this.context = null;
        this.ID = ID;
    }

    CLObject(CLContext context, long ID) {
        this.cl = context.cl;
        this.context = context;
        this.ID = ID;
    }

    /**
     * Implementation detail.
     * TODO remove as soon we have extension methods.
     * @deprecated This method is not intended to be called from client code.
     * @see java.lang.AutoCloseable
     */
    @Deprecated
    public final void close() {
        if(this instanceof CLResource) {
            ((CLResource)this).release();
        }
    }

    /**
     * Returns the context for this OpenCL object.
     */
    public CLContext getContext() {
        return context;
    }

    /**
     * Returns the platform for this OpenCL object.
     */
    public CLPlatform getPlatform() {
        return context.getPlatform();
    }

    /**
     * Returns the OpenCL object handle
     */
    public long getID() {
        return ID;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [id: " + ID
                                          + " context: " + context+"]";
    }

}
