package com.mbien.opencl;

import java.nio.Buffer;

import static com.mbien.opencl.CLException.*;

/**
 *
 * @author Michael Bien
 */
public class CLBuffer<B extends Buffer> extends CLMemory<B> {


    protected CLBuffer(CLContext context, B directBuffer, long id) {
        super(context, directBuffer, id);
    }

    static <B extends Buffer> CLBuffer<B> create(CLContext context, B directBuffer, int flags) {

        if(directBuffer != null && !directBuffer.isDirect())
            throw new IllegalArgumentException("buffer is not a direct buffer");

        B host_ptr = null;
        CL cl = context.cl;
        int[] result = new int[1];

        if(isHostPointerFlag(flags)) {
            host_ptr = directBuffer;
        }
        long id = cl.clCreateBuffer(context.ID, flags, sizeOfBufferElem(directBuffer)*directBuffer.capacity(), host_ptr, result, 0);
        checkForError(result[0], "can not create cl buffer");
        
        return new CLBuffer<B>(context, directBuffer, id);
    }
    
    @Override
    public <T extends Buffer> CLBuffer<T> cloneWith(T directBuffer) {
        return new CLBuffer<T>(context, directBuffer, ID);
    }

}
