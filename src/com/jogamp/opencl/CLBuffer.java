package com.jogamp.opencl;

import com.jogamp.common.nio.Buffers;
import java.util.List;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.opencl.CLMemory.Mem;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Collections;

import static com.jogamp.opencl.CLException.*;

/**
 * OpenCL buffer object wrapping an optional NIO buffer.
 * @author Michael Bien
 */
public class CLBuffer<B extends Buffer> extends CLMemory<B> {

    private List<CLSubBuffer<B>> childs;

    protected CLBuffer(CLContext context, long size, long id, int flags) {
        this(context, null, size, id, flags);
    }

    protected CLBuffer(CLContext context, B directBuffer, long size, long id, int flags) {
        super(context, directBuffer, size, id, flags);
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

        return new CLBuffer(context, size, id, flags);
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
        int size = sizeOfBufferElem(directBuffer) * directBuffer.capacity();
        long id = cl.clCreateBuffer(context.ID, flags, size, host_ptr, result, 0);
        checkForError(result[0], "can not create cl buffer");
        
        return new CLBuffer<B>(context, directBuffer, size, id, flags);
    }

    /**
     * Creates a sub buffer with the specified region from this buffer.
     * If this buffer contains a NIO buffer, the sub buffer will also contain a slice
     * matching the specified region of the parent buffer. The region is specified
     * by the offset and size in buffer elements or bytes if this buffer does not
     * contain any NIO buffer.
     * @param offset The offset in buffer elements.
     * @param size The size in buffer elements.
     */
    public CLSubBuffer<B> createSubBuffer(int offset, int size, Mem... flags) {

        B slice = null;
        if(buffer != null) {
            slice = (B)Buffers.slice(buffer, offset, size);
            int elemSize = Buffers.sizeOfBufferElem(buffer);
            offset *= elemSize;
            size *= elemSize;
        }

        PointerBuffer info = PointerBuffer.allocateDirect(2);
        info.put(offset).put(size).rewind();
        int bitset = Mem.flagsToInt(flags);
        
        int[] err = new int[1];
        long subID = cl.clCreateSubBuffer(ID, bitset, CL.CL_BUFFER_CREATE_TYPE_REGION, info.getBuffer(), err, 0);
        checkForError(err[0], "can not create sub buffer");

        CLSubBuffer<B> clSubBuffer = new CLSubBuffer<B>(this, offset, size, slice, subID, bitset);
        if(childs == null) {
            childs = new ArrayList<CLSubBuffer<B>>();
        }
        childs.add(clSubBuffer);
        return clSubBuffer;
    }

    @Override
    public void release() {
        if(childs != null) {
            while(!childs.isEmpty()) {
                childs.get(0).release();
            }
        }
        super.release();
    }

    void onReleaseSubBuffer(CLSubBuffer sub) {
        childs.remove(sub);
    }

    /**
     * Returns the list of subbuffers.
     */
    public List<CLSubBuffer<B>> getSubBuffers() {
        if(childs == null) {
            return Collections.EMPTY_LIST;
        }else{
            return Collections.unmodifiableList(childs);
        }
    }

    /**
     * Returns true if this is a sub buffer.
     */
    public boolean isSubBuffer() {
        return false;
    }
    
    @Override
    public <T extends Buffer> CLBuffer<T> cloneWith(T directBuffer) {
        return new CLBuffer<T>(context, directBuffer, size, ID, FLAGS);
    }

}
