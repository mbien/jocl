/*
 * Created on Friday, February 26 2010
 */
package com.jogamp.opencl.gl;

import com.jogamp.opencl.CLMemory.GLObjectType;
import javax.media.opengl.GLContext;

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
    
    /**
     * Returns the OpenCL context of this shared object.
     */
    public CLGLContext getContext();

    /**
     * Returns the OpenGL context of this shared object.
     */
    public GLContext getGLContext();

}
