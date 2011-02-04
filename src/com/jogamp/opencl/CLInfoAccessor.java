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
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.opencl.util.CLUtil;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import static com.jogamp.opencl.CLException.*;

/**
 * Internal utility for common OpenCL clGetFooInfo calls.
 * Threadsafe.
 * @author Michael Bien
 */
abstract class CLInfoAccessor {

    protected final static ThreadLocal<ByteBuffer> localBB = new ThreadLocal<ByteBuffer>() {

        @Override
        protected ByteBuffer initialValue() {
            return Buffers.newDirectByteBuffer(512);
        }

    };
    protected final static ThreadLocal<PointerBuffer> localPB = new ThreadLocal<PointerBuffer>() {

        @Override
        protected PointerBuffer initialValue() {
            return PointerBuffer.allocateDirect(1);
        }

    };

    public final long getLong(int key) {

        ByteBuffer buffer = localBB.get();
        int ret = getInfo(key, 8, buffer, null);
        checkForError(ret, "error while asking for info value");

        return buffer.getLong(0);
    }

    public final String getString(int key) {
        
        ByteBuffer buffer = localBB.get();
        PointerBuffer sizeBuffer = localPB.get();
        int ret = getInfo(key, buffer.capacity(), buffer, sizeBuffer);
        checkForError(ret, "error while asking for info string");

        int clSize = (int)sizeBuffer.get(0);
        byte[] array = new byte[clSize];
        buffer.get(array).rewind();

        return CLUtil.clString2JavaString(array, clSize);

    }

    protected abstract int getInfo(int name, long valueSize, Buffer value, PointerBuffer valueSizeRet);


}
