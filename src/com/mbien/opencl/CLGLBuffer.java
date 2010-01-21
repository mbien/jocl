package com.mbien.opencl;

import java.nio.Buffer;


import static com.mbien.opencl.CLGLI.*;

/**
 * Shared buffer between OpenGL and OpenCL contexts.
 * @author Michael Bien
 */
public final class CLGLBuffer<B extends Buffer> extends CLBuffer<B> {

    private CLGLBuffer(CLContext context, B directBuffer, long id) {
        super(context, directBuffer, id);
    }


    static <B extends Buffer> CLGLBuffer<B> create(CLContext context, B directBuffer, int flags, int glBuffer) {
        if(directBuffer != null && !directBuffer.isDirect())
            throw new IllegalArgumentException("buffer is not a direct buffer");

        if(isHostPointerFlag(flags)) {
            throw new IllegalArgumentException(
                    "CL_MEM_COPY_HOST_PTR or CL_MEM_USE_HOST_PTR can not be used with OpenGL Buffers.");
        }
        
        CL cl = context.cl;
        int[] result = new int[1];
        CLGLI clgli = (CLGLI)cl;
        
        long id = clgli.clCreateFromGLBuffer(context.ID, flags, glBuffer, result, 0);

        return new CLGLBuffer<B>(context, directBuffer, id);
    }

    @Override
    public <T extends Buffer> CLGLBuffer<T> cloneWith(T directBuffer) {
        return new CLGLBuffer<T>(context, directBuffer, ID);
    }

    /**
     * Returns the OpenGL buffer type of this shared buffer.
     */
    public GLObjectType getGLObjectType() {
        int[] array = new int[1];
        int ret = ((CLGLI)cl).clGetGLObjectInfo(ID, array, 0, null, 0);
        CLException.checkForError(ret, "error while asking for gl object info");
        return GLObjectType.valueOf(array[0]);
    }

    /**
     * Returns the OpenGL object id of this shared buffer.
     */
    public int getGLObjectID() {
        int[] array = new int[1];
        int ret = ((CLGLI)cl).clGetGLObjectInfo(ID, null, 0, array, 0);
        CLException.checkForError(ret, "error while asking for gl object info");
        return array[0];
    }

    public enum GLObjectType {

        GL_OBJECT_BUFFER(CL_GL_OBJECT_BUFFER),
        GL_OBJECT_TEXTURE2D(CL_GL_OBJECT_TEXTURE2D),
        GL_OBJECT_TEXTURE3D(CL_GL_OBJECT_TEXTURE3D),
        GL_OBJECT_RENDERBUFFER(CL_GL_OBJECT_RENDERBUFFER);

        public final int TYPE;

        private GLObjectType(int type) {
            this.TYPE = type;
        }

        public static GLObjectType valueOf(int type) {
            if(type == CL_GL_OBJECT_BUFFER)
                return GL_OBJECT_BUFFER;
            else if(type == CL_GL_OBJECT_TEXTURE2D)
                return GL_OBJECT_TEXTURE2D;
            else if(type == CL_GL_OBJECT_TEXTURE3D)
                return GL_OBJECT_TEXTURE3D;
            else if(type == CL_GL_OBJECT_RENDERBUFFER)
                return GL_OBJECT_RENDERBUFFER;
            return null;
        }
    }

}
