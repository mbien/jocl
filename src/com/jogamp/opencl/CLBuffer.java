package com.jogamp.opencl;

import java.nio.Buffer;

import static com.jogamp.opencl.CLException.*;

/**
 *
 * @author Michael Bien
 */
public class CLBuffer<B extends Buffer> extends CLMemory<B> {

    protected CLBuffer(CLContext context, long id, int flags) {
        super(context, id, flags);
    }

    protected CLBuffer(CLContext context, B directBuffer, long id, int flags) {
        super(context, directBuffer, id, flags);
    }

    @SuppressWarnings("unchecked")
    static CLBuffer<?> create(CLContext context, int size, int flags) {

        CL cl = context.cl;
        int[] result = new int[1];

        if(isHostPointerFlag(flags)) {
            throw new IllegalArgumentException("no host pointer defined");
        }

        long id = cl.clCreateBuffer(context.ID, flags, size, null, result, 0);
        checkForError(result[0], "can not create cl buffer");

        return new CLBuffer(context, id, flags);
    }

    static <B extends Buffer> CLBuffer<B> create(CLContext context, B directBuffer, int flags) {

        if(!directBuffer.isDirect())
            throw new IllegalArgumentException("buffer is not direct");

        B host_ptr = null;
        CL cl = context.cl;
        int[] result = new int[1];

        if(isHostPointerFlag(flags)) {
            host_ptr = directBuffer;
        }
        long id = cl.clCreateBuffer(context.ID, flags, sizeOfBufferElem(directBuffer)*directBuffer.capacity(), host_ptr, result, 0);
        checkForError(result[0], "can not create cl buffer");
        
        return new CLBuffer<B>(context, directBuffer, id, flags);
    }
    
    @Override
    public <T extends Buffer> CLBuffer<T> cloneWith(T directBuffer) {
        return new CLBuffer<T>(context, directBuffer, ID, FLAGS);
    }

}
