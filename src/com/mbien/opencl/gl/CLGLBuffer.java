package com.mbien.opencl.gl;

import com.mbien.opencl.CL;
import com.mbien.opencl.CLBuffer;
import com.mbien.opencl.CLContext;
import com.mbien.opencl.CLGLI;
import com.mbien.opencl.CLMemory.GLObjectType;
import java.nio.Buffer;


/**
 * Shared buffer between OpenGL and OpenCL contexts.
 * @author Michael Bien
 */
public final class CLGLBuffer<B extends Buffer> extends CLBuffer<B> implements CLGLObject {


    /**
     * The OpenGL object handle.
     */
    public final int GLID;

    private CLGLBuffer(CLContext context, B directBuffer, long id, int glObject) {
        super(context, directBuffer, id);
        this.GLID = glObject;
    }


    static <B extends Buffer> CLGLBuffer<B> create(CLContext context, B directBuffer, int flags, int glObject) {
        checkBuffer(directBuffer, flags);
        
        CL cl = getCL(context);
        int[] result = new int[1];
        CLGLI clgli = (CLGLI)cl;
        
        long id = clgli.clCreateFromGLBuffer(context.ID, flags, glObject, result, 0);

        return new CLGLBuffer<B>(context, directBuffer, id, glObject);
    }

    static <B extends Buffer> void checkBuffer(B directBuffer, int flags) throws IllegalArgumentException {
        if (directBuffer != null && !directBuffer.isDirect()) {
            throw new IllegalArgumentException("buffer is not a direct buffer");
        }
        if (isHostPointerFlag(flags)) {
            throw new IllegalArgumentException("CL_MEM_COPY_HOST_PTR or CL_MEM_USE_HOST_PTR can not be used with OpenGL Buffers.");
        }
    }

    public int getGLObjectID() {
        return GLID;
    }

    public GLObjectType getGLObjectType() {
        return GLObjectType.GL_OBJECT_BUFFER;
    }

    @Override
    public <T extends Buffer> CLGLBuffer<T> cloneWith(T directBuffer) {
        return new CLGLBuffer<T>(context, directBuffer, ID, GLID);
    }

    @Override
    public String toString() {
        return "CLGLBuffer [id: " + ID+" glID: "+GLID+"]";
    }

}
