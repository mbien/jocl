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

package com.jogamp.opencl;

import com.jogamp.common.nio.Buffers;
import java.util.List;
import com.jogamp.common.nio.NativeSizeBuffer;
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
        int size = Buffers.sizeOfBufferElem(directBuffer) * directBuffer.capacity();
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
            slice = Buffers.slice(buffer, offset, size);
            int elemSize = Buffers.sizeOfBufferElem(buffer);
            offset *= elemSize;
            size *= elemSize;
        }

        NativeSizeBuffer info = NativeSizeBuffer.allocateDirect(2);
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
    @SuppressWarnings("unchecked")
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
