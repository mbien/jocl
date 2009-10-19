package com.mbien.opencl;

import java.nio.ByteBuffer;
import static com.mbien.opencl.CLException.*;

/**
 *
 * @author Michael Bien
 */
public class CLBuffer {

    public final ByteBuffer buffer;
    public final long ID;
    
    private final CLContext context;
    private final CL cl;

    CLBuffer(CLContext context, int flags, ByteBuffer directBuffer) {
        
        if(!directBuffer.isDirect())
            throw new IllegalArgumentException("buffer is not a direct buffer");

        this.buffer = directBuffer;
        this.context = context;
        this.cl = context.cl;

        int[] intArray = new int[1];

        this.ID = cl.clCreateBuffer(context.ID, flags, directBuffer.capacity(), null, intArray, 0);

        checkForError(intArray[0], "can not create cl buffer");
        
    }

    public void release() {
        int ret = cl.clReleaseMemObject(ID);
        context.bufferReleased(this);
        checkForError(ret, "can not release mem object");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CLBuffer other = (CLBuffer) obj;
        if (this.buffer != other.buffer && (this.buffer == null || !this.buffer.equals(other.buffer))) {
            return false;
        }
        if (this.context.ID != other.context.ID) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + (this.buffer != null ? this.buffer.hashCode() : 0);
        hash = 29 * hash + (int) (this.context.ID ^ (this.context.ID >>> 32));
        return hash;
    }




}
