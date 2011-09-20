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
 * Created on Tuesday, September 20 2011 01:26
 */
package com.jogamp.opencl.util.pp;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import java.nio.IntBuffer;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 *
 * @author mbien
 */
public class ForEachTest {
    

    @Test
    public void testForEach() {
        CLContext context = CLContext.create();
        try{
            CLCommandQueue queue = context.getMaxFlopsDevice().createCommandQueue();
            
            IntBuffer input = Buffers.newDirectIntBuffer(new int[]{ 1, 2, 3, 4, 5, 6, 7, 8});
            IntBuffer output = Buffers.newDirectIntBuffer(input.capacity());
            
            String op = "b[globalID] = a[globalID]+1;";
            ForEach<IntBuffer> foreach = ForEach.create(context, op, IntBuffer.class);
            foreach.foreach(queue, input, output);
            
            while(output.hasRemaining()) {
                assertEquals(input.get()+1, output.get());
            }
            
            foreach.release();
        }finally{
            context.release();
        }
    }


}
