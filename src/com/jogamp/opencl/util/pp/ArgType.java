/*
 * Copyright (c) 2011, Michael Bien
 * All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

/*
 * Created on Sunday, September 18 2011 22:22
 */
package com.jogamp.opencl.util.pp;

import java.nio.Buffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import static com.jogamp.common.nio.Buffers.*;

/**
 *
 * @author Michael Bien
 */
 enum ArgType {
     
//  BYTE(SIZEOF_BYTE),
    SHORT(SIZEOF_SHORT),
    INT(SIZEOF_INT),
    LONG(SIZEOF_LONG),
    FLOAT(SIZEOF_FLOAT),
    DOUBLE(SIZEOF_DOUBLE);
    
    public final int SIZE;

    private ArgType(int size) {
        this.SIZE = size;
    }

    public String type() {
        return name().toLowerCase();
    }

    public String vectorType(int elements) {
        return type() + (elements == 0 ? "" : elements);
    }

    public static <B extends Buffer> ArgType valueOf(Class<B> elementType) {
        if (elementType.equals(ShortBuffer.class)) {
            return ArgType.SHORT;
        } else if (elementType.equals(IntBuffer.class)) {
            return ArgType.INT;
        } else if (elementType.equals(LongBuffer.class)) {
            return ArgType.LONG;
        } else if (elementType.equals(FloatBuffer.class)) {
            return ArgType.FLOAT;
        } else if (elementType.equals(DoubleBuffer.class)) {
            return ArgType.DOUBLE;
//        }else if(elementType.equals(ByteBuffer.class)) {
//            ELEMENT_SIZE = SIZEOF_BYTE;
        } else {
            throw new IllegalArgumentException("unsupported buffer type " + elementType);
        }
    }
    
}
