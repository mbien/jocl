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

import com.jogamp.common.nio.NativeSizeBuffer;
import com.jogamp.common.os.Platform;
import com.jogamp.opencl.util.CLUtil;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import static com.jogamp.common.nio.Buffers.*;
import static com.jogamp.opencl.CLException.*;

/**
 * Internal utility for common OpenCL clGetFooInfo calls.
 * Threadsafe.
 * @author Michael Bien
 */
abstract class CLInfoAccessor {

    private static final int BB_SIZE = 512;

    protected final static ThreadLocal<ByteBuffer> localBB = new ThreadLocal<ByteBuffer>() {

        @Override
        protected ByteBuffer initialValue() {
            return newDirectByteBuffer(BB_SIZE);
        }

    };
    protected final static ThreadLocal<NativeSizeBuffer> localNSB = new ThreadLocal<NativeSizeBuffer>() {

        @Override
        protected NativeSizeBuffer initialValue() {
            return NativeSizeBuffer.allocateDirect(1);
        }

    };

    public final long getLong(int key) {

        ByteBuffer buffer = getBB(8).putLong(0, 0);
        int ret = getInfo(key, 8, buffer, null);
        checkForError(ret, "error while asking for info value");

        return buffer.getLong(0);
    }

    public final String getString(int key) {
        
        NativeSizeBuffer sizeBuffer = getNSB();
        int ret = getInfo(key, 0, null, sizeBuffer);
        checkForError(ret, "error while asking for info string");

        int clSize = (int)sizeBuffer.get(0);
        ByteBuffer buffer = getBB(clSize);

        ret = getInfo(key, buffer.capacity(), buffer, null);
        checkForError(ret, "error while asking for info string");

        byte[] array = new byte[clSize];
        buffer.get(array).rewind();

        return CLUtil.clString2JavaString(array, clSize);

    }

    public final int[] getInts(int key, int n) {

        ByteBuffer buffer = getBB(n * (Platform.is32Bit()?4:8));
        int ret = getInfo(key, buffer.capacity(), buffer, null);
        checkForError(ret, "error while asking for info value");

        int[] array = new int[n];
        for(int i = 0; i < array.length; i++) {
            if(Platform.is32Bit()) {
                array[i] = buffer.getInt();
            }else{
                array[i] = (int)buffer.getLong();
            }
        }
        buffer.rewind();

        return array;
    }

    protected ByteBuffer getBB(int minCapacity) {
        if(minCapacity > BB_SIZE) {
            return newDirectByteBuffer(minCapacity);
        }else{
            return localBB.get();
        }
    }

    protected NativeSizeBuffer getNSB() {
        return localNSB.get();
    }

    protected abstract int getInfo(int name, long valueSize, Buffer value, NativeSizeBuffer valueSizeRet);


}
