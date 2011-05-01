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
import com.jogamp.opencl.CLMemory.Mem;
import java.nio.Buffer;

/**
 * A sub buffer of a CLBuffer.
 * @author Michael Bien
 */
public class CLSubBuffer<B extends Buffer> extends CLBuffer<B> {

    private CLBuffer<B> parent;
    private final int offset;

    CLSubBuffer(CLBuffer<B> parent, int origin, int size, B directBuffer, long id, int flags) {
        super(parent.getContext(), directBuffer, size, id, flags);
        this.parent = parent;
        this.offset = origin;
    }

    /**
     * Throws an UnsupportedOperationException since creating sub buffers
     * from sub buffers is not allowed as of OpenCL 1.1.
     */
    @Override
    public CLSubBuffer<B> createSubBuffer(int origin, int size, Mem... flags) {
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
     * Returns the offset of this sub buffer to its parent in buffer elements.
     */
    public int getOffset() {
        int elemSize = buffer==null ? 1 : Buffers.sizeOfBufferElem(buffer);
        return offset/elemSize;
    }

    /**
     * Returns the offset of this sub buffer to its parent in bytes.
     */
    public int getCLOffset() {
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
