package com.mbien.opencl;

import java.nio.Buffer;

import static com.mbien.opencl.CLException.*;

/**
 *
 * @author Michael Bien
 */
public final class CLBuffer<B extends Buffer> extends CLMemory<B> {


    private CLBuffer(CLContext context, B directBuffer, long id) {
        super(context, directBuffer, id);
    }

    static <B extends Buffer> CLBuffer<B> create(CLContext context, B directBuffer, int flags) {
        return create(context, directBuffer, flags, 0);
    }
    
    static <B extends Buffer> CLBuffer<B> create(CLContext context, B directBuffer, int flags, int glBuffer) {

        if(directBuffer != null && !directBuffer.isDirect())
            throw new IllegalArgumentException("buffer is not a direct buffer");

        CL cl = context.cl;
        long id;

        int[] result = new int[1];

        if(glBuffer == 0) {
            B host_ptr = null;
            if(isHostPointerFlag(flags)) {
                host_ptr = directBuffer;
            }
            id = cl.clCreateBuffer(context.ID, flags, sizeOfBufferElem(directBuffer)*directBuffer.capacity(), host_ptr, result, 0);
        }else{
            if(isHostPointerFlag(flags)) {
                throw new IllegalArgumentException(
                        "CL_MEM_COPY_HOST_PTR or CL_MEM_USE_HOST_PTR can not be used with OpenGL Buffers.");
            }
            CLGLI clgli = (CLGLI)cl;
            id = clgli.clCreateFromGLBuffer(context.ID, flags, glBuffer, result, 0);
        }
        checkForError(result[0], "can not create cl buffer");

        return new CLBuffer<B>(context, directBuffer, id);
    }

    @Override
    public <T extends Buffer> CLBuffer<T> cloneWith(T directBuffer) {
        return new CLBuffer<T>(context, directBuffer, ID);
    }

}
