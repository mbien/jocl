package com.jogamp.opencl;

import com.jogamp.common.nio.Buffers;
import java.util.List;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.opencl.CLMemory.Mem;
import java.nio.Buffer;
import java.util.ArrayList;

import static com.jogamp.opencl.CLException.*;

/**
 * OpenCL buffer object.
 * @author Michael Bien
 */
public class CLBuffer<B extends Buffer> extends CLMemory<B> {

    private List<CLBuffer<B>> childs;

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

    /**
     * Creates a sub buffer with the specified region from this buffer.
     */
    public CLBuffer<B> createSubBuffer(int origin, int size, Mem... flags) {
        PointerBuffer info = PointerBuffer.allocateDirect(2);
        info.put(origin).put(size).rewind();
        int bitset = Mem.flagsToInt(flags);
        
        int[] err = new int[1];
        long subID = cl.clCreateSubBuffer(ID, bitset, CL.CL_BUFFER_CREATE_TYPE_REGION, info.getBuffer(), err, 0);
        checkForError(err[0], "can not create sub buffer");

        B slice = null;
        if(buffer != null) {
            slice = (B)Buffers.slice(buffer, origin, size);
        }

        CLSubBuffer<B> clSubBuffer = new CLSubBuffer<B>(this, origin, slice, subID, bitset);
        if(childs == null) {
            childs = new ArrayList<CLBuffer<B>>();
        }
        childs.add(clSubBuffer);
        return clSubBuffer;
    }

    @Override
    public void release() {
        if(childs != null) {
            for (CLBuffer<B> child : childs) {
                child.release();
            }
        }
        super.release();
    }

    void onReleaseSubBuffer(CLSubBuffer sub) {
        childs.remove(sub);
    }

    /**
     * Returns true if this is a sub buffer.
     */
    public boolean isSubBuffer() {
        return false;
    }
    
    @Override
    public <T extends Buffer> CLBuffer<T> cloneWith(T directBuffer) {
        return new CLBuffer<T>(context, directBuffer, ID, FLAGS);
    }

}
