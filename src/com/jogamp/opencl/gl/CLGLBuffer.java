/*
 * Copyright 2009 - 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package com.jogamp.opencl.gl;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLException;

import java.nio.Buffer;
import javax.media.opengl.GLContext;


/**
 * Shared buffer between OpenGL and OpenCL contexts.
 * @author Michael Bien
 */
public final class CLGLBuffer<B extends Buffer> extends CLBuffer<B> implements CLGLObject {


    /**
     * The OpenGL object handle.
     */
    public final int GLID;

    private CLGLBuffer(CLContext context, B directBuffer, long id, int glObject, long size, int flags) {
        super(context, directBuffer, size, id, flags);
        this.GLID = glObject;
    }


    static <B extends Buffer> CLGLBuffer<B> create(CLContext context, B directBuffer, long size, int flags, int glObject) {
        checkBuffer(directBuffer, flags);
        
        CLGLI clgli = (CLGLI)getCL(context);
        
        int[] result = new int[1];
        long id = clgli.clCreateFromGLBuffer(context.ID, flags, glObject, result, 0);
        CLException.checkForError(result[0], "can not create CLGLObject from #"+glObject);

        return new CLGLBuffer<B>(context, directBuffer, id, glObject, size, flags);
    }

    static <B extends Buffer> void checkBuffer(B directBuffer, int flags) throws IllegalArgumentException {
        if (directBuffer != null && !directBuffer.isDirect()) {
            throw new IllegalArgumentException("buffer is not a direct buffer");
        }
        if (isHostPointerFlag(flags)) {
            throw new IllegalArgumentException("CL_MEM_COPY_HOST_PTR or CL_MEM_USE_HOST_PTR can not be used with OpenGL Buffers.");
        }
    }

    /**
     * Updates the size of this CLGLBuffer by querying OpenGL.
     * This method may only be called if this memory object has been acquired by calling
     * {@link CLCommandQueue#putAcquireGLObject(com.jogamp.opencl.gl.CLGLObject)}.
     */
    public void updateSize() {
        size = getSizeImpl(cl, ID);
        initCLCapacity();
    }

    public int getGLObjectID() {
        return GLID;
    }

    public GLObjectType getGLObjectType() {
        return GLObjectType.GL_OBJECT_BUFFER;
    }

    @Override
    public CLGLContext getContext() {
        return (CLGLContext) super.getContext();
    }

    public GLContext getGLContext() {
        return getContext().getGLContext();
    }

    @Override
    public <T extends Buffer> CLGLBuffer<T> cloneWith(T directBuffer) {
        return new CLGLBuffer<T>(context, directBuffer, ID, GLID, size, FLAGS);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+" [id: " + ID+" glID: "+GLID+"]";
    }

}
