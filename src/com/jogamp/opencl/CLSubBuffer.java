package com.jogamp.opencl;

import com.jogamp.opencl.CLMemory.Mem;
import java.nio.Buffer;

/**
 * A sub buffer of a CLBuffer.
 * @author Michael Bien
 */
public class CLSubBuffer<B extends Buffer> extends CLBuffer<B> {

    private CLBuffer<B> parent;
    private final int offset;

    CLSubBuffer(CLBuffer<B> parent, int origin, B directBuffer, long id, int flags) {
        super(parent.getContext(), directBuffer, id, flags);
        this.parent = parent;
        this.offset = origin;
    }

    /**
     * Throws an UnsupportedOperationException since creating sub buffers
     * from sub buffers is not allowed.
     */
    @Override
    public CLBuffer<B> createSubBuffer(int origin, int size, Mem... flags) {
        throw new UnsupportedOperationException("creating sub buffers from sub buffers is not allowed.");
    }

    @Override
    public void release() {
        parent.onReleaseSubBuffer(this);
        super.release();
    }

    /**
     * Returns the parent buffer this buffer was created from.
     */
    public CLBuffer<B> getParent() {
        return parent;
    }

    /**
     * Returns the offset of this sub buffer to its parent.
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Returns true.
     */
    @Override
    public boolean isSubBuffer() {
        return true;
    }
}
