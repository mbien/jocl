/*
 * Created on Friday, February 26 2010
 */
package com.mbien.opencl.gl;

import com.mbien.opencl.CLMemory.GLObjectType;

/**
 *
 * @author Michael Bien
 */
interface CLGLObject {

    /**
     * Returns the OpenGL object id of this shared object.
     */
    public int getGLObjectID();

    /**
     * Returns the OpenGL buffer type of this shared object.
     */
    public GLObjectType getGLObjectType();

}
