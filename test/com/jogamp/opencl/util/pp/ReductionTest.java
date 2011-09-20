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
 * Created on Tuesday, September 13 2011 04:22
 */
package com.jogamp.opencl.util.pp;

import com.jogamp.opencl.util.pp.Reduction.Op;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLPlatform;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author mbien
 */
public class ReductionTest {
    
    public static final int SIZE = 1024*4*2+4+4;
    
    public static final float EPSILON = 0.00001f;
    public static final float SUM_EPSILON = 1000;

    @Test
    public void intReducerTest() {

        CLContext context = CLContext.create(device());
        System.out.println(context);

        try{
            CLCommandQueue queue = context.getMaxFlopsDevice().createCommandQueue();

            IntBuffer input = Buffers.newDirectIntBuffer(SIZE);
            Random rnd = new Random(42);
            int expected_min = Integer.MAX_VALUE;
            int expected_max = Integer.MIN_VALUE;
            int expected_sum = 0;
            while(input.hasRemaining()) {
                int v = rnd.nextInt();
                if(expected_min > v) expected_min = v;
                if(expected_max < v) expected_max = v;
                expected_sum+=v;
                input.put(v);
            }
            input.rewind();

            IntBuffer output = Buffers.newDirectIntBuffer(1);

            Reduction<IntBuffer> max = Reduction.create(context, Op.MAX, IntBuffer.class);
            max.reduce(queue, input, output);
            output.rewind();

            max.release();
            assertTrue(max.isReleased());

            assertEquals(expected_max, output.get(0));

            Reduction<IntBuffer> min = Reduction.create(context, Op.MIN, IntBuffer.class);
            min.reduce(queue, input, output);
            output.rewind();

            min.release();
            assertTrue(min.isReleased());

            assertEquals(expected_min, output.get(0));

            Reduction<IntBuffer> sum = Reduction.create(context, Op.ADD, IntBuffer.class);
            sum.reduce(queue, input, output);
            output.rewind();

            sum.release();
            assertTrue(sum.isReleased());

            assertEquals(expected_sum, output.get(0));

        }finally{
            context.release();
        }
    }

    @Test
    public void floatReducerTest() {

        CLContext context = CLContext.create(device());
        System.out.println(context);

        try{
            CLCommandQueue queue = context.getMaxFlopsDevice().createCommandQueue();

            FloatBuffer input = Buffers.newDirectFloatBuffer(SIZE);
            Random rnd = new Random(42);
            float expected_min = Float.MAX_VALUE;
            float expected_max = Float.MIN_VALUE;
            float expected_sum = 0;
            while(input.hasRemaining()) {
                float v = rnd.nextFloat();
                if(expected_min > v) expected_min = v;
                if(expected_max < v) expected_max = v;
                expected_sum+=v;
                input.put(v);
            }
            input.rewind();

            FloatBuffer output = Buffers.newDirectFloatBuffer(1);

            Reduction<FloatBuffer> max = Reduction.create(context, Op.MAX, FloatBuffer.class);
            max.reduce(queue, input, output);
            output.rewind();

            max.release();
            assertTrue(max.isReleased());

            assertEquals(expected_max, output.get(0), EPSILON);

            Reduction<FloatBuffer> min = Reduction.create(context, Op.MIN, FloatBuffer.class);
            min.reduce(queue, input, output);
            output.rewind();

            min.release();
            assertTrue(min.isReleased());

            assertEquals(expected_min, output.get(0), EPSILON);

            Reduction<FloatBuffer> sum = Reduction.create(context, Op.ADD, FloatBuffer.class);
            sum.reduce(queue, input, output);
            output.rewind();

            sum.release();
            assertTrue(sum.isReleased());

            assertEquals(expected_sum, output.get(0), Math.ulp(expected_sum)*SUM_EPSILON);

        }finally{
            context.release();
        }
    }

    @Test
    public void doubleReducerTest() {

        CLContext context = CLContext.create(device());
        System.out.println(context);

        try{
            CLCommandQueue queue = context.getMaxFlopsDevice().createCommandQueue();

            DoubleBuffer input = Buffers.newDirectDoubleBuffer(SIZE);
            Random rnd = new Random(42);
            double expected_min = Double.MAX_VALUE;
            double expected_max = Double.MIN_VALUE;
            double expected_sum = 0;
            while(input.hasRemaining()) {
                double v = rnd.nextDouble();
                if(expected_min > v) expected_min = v;
                if(expected_max < v) expected_max = v;
                expected_sum+=v;
                input.put(v);
            }
            input.rewind();

            DoubleBuffer output = Buffers.newDirectDoubleBuffer(1);

            Reduction<DoubleBuffer> max = Reduction.create(context, Op.MAX, DoubleBuffer.class);
            max.reduce(queue, input, output);
            output.rewind();

            max.release();
            assertTrue(max.isReleased());

            assertEquals(expected_max, output.get(0), EPSILON);

            Reduction<DoubleBuffer> min = Reduction.create(context, Op.MIN, DoubleBuffer.class);
            min.reduce(queue, input, output);
            output.rewind();

            min.release();
            assertTrue(min.isReleased());

            assertEquals(expected_min, output.get(0), EPSILON);

            Reduction<DoubleBuffer> sum = Reduction.create(context, Op.ADD, DoubleBuffer.class);
            sum.reduce(queue, input, output);
            output.rewind();

            sum.release();
            assertTrue(sum.isReleased());

            assertEquals(expected_sum, output.get(0), Math.ulp(expected_sum)*SUM_EPSILON);

        }finally{
            context.release();
        }
    }

    private CLDevice device() {
        return CLPlatform.listCLPlatforms()[0].getMaxFlopsDevice();
    }
}
