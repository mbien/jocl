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

import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.util.pp.Scan.Op;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.TestUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.System.*;


/**
 *
 * @author Michael Bien
 */
public class ScanTest {
    

    @Test
    public void testSmallScan() {
        CLContext context = CLContext.create(getDevice());

        try{
            CLCommandQueue queue = context.getMaxFlopsDevice().createCommandQueue();
            
            int[][] in      = new int[][]{ { 4, 0, 5, 5, 0, 5, 5, 1    },   //even
                                           { 4, 0, 5, 5, 0, 5, 5, 1, 3 }  };//odd

            int[] inclusive = new int[]    { 4, 4, 9,14,14,19,24,27,28 };
            int[] exclusive = new int[]    { 0, 4, 4, 9,14,14,19,24,27 };

            for (int i = 0; i < in.length; i++) {

                IntBuffer input = Buffers.newDirectIntBuffer(in[i]);
                IntBuffer output = Buffers.newDirectIntBuffer(input.capacity());

                out.println((input.capacity()%2==0?"even":"odd") + " array lenght");

                Scan<IntBuffer> scan = Scan.create(context, Op.ADD, input.getClass());
                scan.scan(queue, input, output);

                while(output.hasRemaining()) {
                    int value = output.get();
//                    System.out.println(value);
                    assertEquals(exclusive[output.position()-1], value);
                }

                scan.release();

            }
        }finally{
            context.release();
        }
    }

    @Test
    public void testSmallScanSizeLimit() {

        CLContext context = CLContext.create(getDevice());

        try{
            CLCommandQueue queue = context.getMaxFlopsDevice().createCommandQueue();

            float[] exclusive = new float[queue.getDevice().getMaxWorkGroupSize()*2];

            FloatBuffer input = Buffers.newDirectFloatBuffer(exclusive.length);
            FloatBuffer output = Buffers.newDirectFloatBuffer(input.capacity());

            TestUtils.fillBuffer(input, 42);
            long time = nanoTime();
            for (int i = 1; i < exclusive.length; i++) {
                exclusive[i] = exclusive[i-1]+input.get(i-1);
            }
            out.println("delta "+(nanoTime()-time));

            Scan<FloatBuffer> scan = Scan.create(context, Op.ADD, input.getClass());
            time = nanoTime();
            scan.scan(queue, input, output);
            out.println("delta "+(nanoTime()-time));

            while(output.hasRemaining()) {
                float value = output.get();
                assertEquals("@"+(output.position()-1),exclusive[output.position()-1], value, 0.1f);
            }

            scan.release();

        }finally{
            context.release();
        }
    }

    private CLDevice getDevice() {
        return CLPlatform.listCLPlatforms()[0].getMaxFlopsDevice();
    }

}
