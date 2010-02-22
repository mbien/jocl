package com.mbien.opencl;

/**
 * Common superclass for all OpenCL objects.
 * @author Michael Bien
 */
abstract class CLObject {

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
        return "CLObject [id: " + ID
                      + " context: " + context+"]";
    }

}
