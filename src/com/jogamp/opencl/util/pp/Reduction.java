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
 * Created on Thursday, September 08 2011 21:22
 */
package com.jogamp.opencl.util.pp;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLProgram;
import com.jogamp.opencl.CLResource;
import com.jogamp.opencl.CLWork.CLWork1D;
import com.jogamp.opencl.util.CLUtil;
import com.jogamp.opencl.util.concurrent.CLQueueContext;
import com.jogamp.opencl.util.concurrent.CLTask;
import com.jogamp.opencl.util.concurrent.CLQueueContext.CLResourceQueueContext;
import com.jogamp.opencl.util.CLProgramConfiguration;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static com.jogamp.opencl.CLMemory.Mem.*;
import static java.lang.Math.*;

/**
 *
 * @author Michael Bien
 */
public class Reduction<B extends Buffer> implements CLResource {

    private static final String SOURCES;

    private final Op OPERATION;
    private final ArgType ELEMENT;

    private final int VECTOR_SIZE;
    private final CLProgram program;
    private final CLWork1D reduction;

    static{
        try {
            StringBuilder sb = new StringBuilder(2048);
            CLUtil.readStream(Reduction.class.getResourceAsStream("reduce.cl"), sb);
            SOURCES = sb.toString();
        } catch (IOException ex) {
            throw new RuntimeException("can not initialize Reduction.", ex);
        }
    }

    private <B extends Buffer> Reduction(CLContext context, Op op, Class<B> elementType) {

        this.ELEMENT = ArgType.valueOf(elementType);
        this.OPERATION = op;
        this.VECTOR_SIZE = 4;

        this.program = context.createProgram(SOURCES);

        CLProgramConfiguration config = program.prepare();
        config.withDefine("OP_"+op.name())
              .withDefine("TYPE", ELEMENT.vectorType(VECTOR_SIZE));
        if(ELEMENT.equals(ArgType.DOUBLE)) {
            config.withDefine("DOUBLE_FP");
        }
        config.build();

        reduction = CLWork1D.create1D(program.createCLKernel("reduce"));
    }

    public static <B extends Buffer> Reduction<B> create(CLContext context, Op op, Class<? extends B> elementType) {
        return new Reduction<B>(context, op, elementType);
    }

    public static <B extends Buffer> Reduction<B> create(CLCommandQueue queue, Op op, Class<? extends B> elementType) {
        return create(queue.getContext(), op, elementType);
    }

    public static <B extends Buffer> CLTask<CLResourceQueueContext<Reduction<B>>, B> createTask(B input, B output, Op op, Class<? extends B> elementType) {
        return new CLReductionTask<B>(input, output, op, elementType);
    }

    public B reduce(CLCommandQueue queue, B input, B output) {

        int length = input.capacity();

        //TODO thats temporary...
        if(length%(VECTOR_SIZE*2) != 0) {
            throw new IllegalArgumentException("input buffer must be evenly devideable through "+VECTOR_SIZE*2);
        }

        int groupSize = (int)reduction.getKernel().getWorkGroupSize(queue.getDevice());
        int realSize  = length / VECTOR_SIZE;
        int workItems = CLUtil.roundUp(realSize, groupSize*2) / 2;

        int groups = workItems / groupSize;
        int sharedBufferSize = groupSize * ELEMENT.SIZE*VECTOR_SIZE;

        int outputSize = groups * ELEMENT.SIZE*VECTOR_SIZE;

        CLContext context = queue.getContext();

        CLBuffer<B> in = context.createBuffer(input, READ_ONLY);
        CLBuffer<ByteBuffer> out = context.createByteBuffer(outputSize, WRITE_ONLY);

        reduction.getKernel().putArgs(in, out).putArgSize(sharedBufferSize).putArg(realSize/2).rewind();
        reduction.setWorkSize(workItems, groupSize);

//        System.out.println(groups);
//        System.out.println(reduction);

        queue.putWriteBuffer(in, false);
        queue.putWork(reduction);
        queue.putReadBuffer(out, true);

        in.release();
        out.release();

        if(OPERATION.equals(Op.MAX)) {
            finishMax(output, out.getBuffer());
        }else if(OPERATION.equals(Op.MIN)) {
            finishMin(output, out.getBuffer());
        }else if(OPERATION.equals(Op.ADD)) {
            finishAdd(output, out.getBuffer());
        }else if(OPERATION.equals(Op.MUL)) {
            finishMul(output, out.getBuffer());
        }else{
            throw new RuntimeException();
        }

        return output;
    }

    private <B extends Buffer> void finishMax(B output, ByteBuffer buffer) {
        if(output instanceof ByteBuffer) {
            byte max = Byte.MIN_VALUE;
            while(buffer.hasRemaining()) {
                max = (byte) max(max, buffer.get());
            }
            ((ByteBuffer)output).put(max);
        }else if(output instanceof ShortBuffer) {
            short max = Short.MIN_VALUE;
            while(buffer.hasRemaining()) {
                max = (short) max(max, buffer.getShort());
            }
            ((ShortBuffer)output).put(max);
        }else if(output instanceof IntBuffer) {
            int max = Integer.MIN_VALUE;
            while(buffer.hasRemaining()) {
                max = max(max, buffer.getInt());
            }
            ((IntBuffer)output).put(max);
        }else if(output instanceof LongBuffer) {
            long max = Long.MIN_VALUE;
            while(buffer.hasRemaining()) {
                max = max(max, buffer.getLong());
            }
            ((LongBuffer)output).put(max);
        }else if(output instanceof FloatBuffer) {
            float max = Float.MIN_VALUE;
            while(buffer.hasRemaining()) {
                max = max(max, buffer.getFloat());
            }
            ((FloatBuffer)output).put(max);
        }else if(output instanceof DoubleBuffer) {
            double max = Double.MIN_VALUE;
            while(buffer.hasRemaining()) {
                max = max(max, buffer.getDouble());
            }
            ((DoubleBuffer)output).put(max);
        }
        buffer.rewind();
    }

    private <B extends Buffer> void finishMin(B output, ByteBuffer buffer) {
        if(output instanceof ByteBuffer) {
            byte min = Byte.MAX_VALUE;
            while(buffer.hasRemaining()) {
                min = (byte) min(min, buffer.get());
            }
            ((ByteBuffer)output).put(min);
        }else if(output instanceof ShortBuffer) {
            short min = Short.MAX_VALUE;
            while(buffer.hasRemaining()) {
                min = (short) min(min, buffer.getShort());
            }
            ((ShortBuffer)output).put(min);
        }else if(output instanceof IntBuffer) {
            int min = Integer.MAX_VALUE;
            while(buffer.hasRemaining()) {
                min = min(min, buffer.getInt());
            }
            ((IntBuffer)output).put(min);
        }else if(output instanceof LongBuffer) {
            long min = Long.MAX_VALUE;
            while(buffer.hasRemaining()) {
                min = min(min, buffer.getLong());
            }
            ((LongBuffer)output).put(min);
        }else if(output instanceof FloatBuffer) {
            float min = Float.MAX_VALUE;
            while(buffer.hasRemaining()) {
                min = min(min, buffer.getFloat());
            }
            ((FloatBuffer)output).put(min);
        }else if(output instanceof DoubleBuffer) {
            double min = Double.MAX_VALUE;
            while(buffer.hasRemaining()) {
                min = min(min, buffer.getDouble());
            }
            ((DoubleBuffer)output).put(min);
        }
        buffer.rewind();
    }


    private <B extends Buffer> void finishAdd(B output, ByteBuffer buffer) {
        if(output instanceof ByteBuffer) {
            long result = 0;
            while(buffer.hasRemaining()) {
                result += buffer.get();
            }
            ((ByteBuffer)output).put((byte)result);
        }else if(output instanceof ShortBuffer) {
            long result = 0;
            while(buffer.hasRemaining()) {
                result += buffer.getShort();
            }
            ((ShortBuffer)output).put((short)result);
        }else if(output instanceof IntBuffer) {
            long result = 0;
            while(buffer.hasRemaining()) {
                result += buffer.getInt();
            }
            ((IntBuffer)output).put((int)result);
        }else if(output instanceof LongBuffer) {
            long result = 0;
            while(buffer.hasRemaining()) {
                result += buffer.getLong();
            }
            ((LongBuffer)output).put(result);
        }else if(output instanceof FloatBuffer) {
            double result = 0;
            while(buffer.hasRemaining()) {
                result += buffer.getFloat();
            }
            ((FloatBuffer)output).put((float)result);
        }else if(output instanceof DoubleBuffer) {
            double result = 0;
            while(buffer.hasRemaining()) {
                result += buffer.getDouble();
            }
            ((DoubleBuffer)output).put(result);
        }
        buffer.rewind();
    }

    private <B extends Buffer> void finishMul(B output, ByteBuffer buffer) {
        if(output instanceof ByteBuffer) {
            long result = buffer.get();
            while(buffer.hasRemaining()) {
                result *= buffer.get();
            }
            ((ByteBuffer)output).put((byte)result);
        }else if(output instanceof ShortBuffer) {
            long result = buffer.getShort();
            while(buffer.hasRemaining()) {
                result *= buffer.getShort();
            }
            ((ShortBuffer)output).put((short)result);
        }else if(output instanceof IntBuffer) {
            long result = buffer.getInt();
            while(buffer.hasRemaining()) {
                result *= buffer.getInt();
            }
            ((IntBuffer)output).put((int)result);
        }else if(output instanceof LongBuffer) {
            long result = buffer.getLong();
            while(buffer.hasRemaining()) {
                result *= buffer.getLong();
            }
            ((LongBuffer)output).put(result);
        }else if(output instanceof FloatBuffer) {
            double result = buffer.getFloat();
            while(buffer.hasRemaining()) {
                result *= buffer.getFloat();
            }
            ((FloatBuffer)output).put((float)result);
        }else if(output instanceof DoubleBuffer) {
            double result = buffer.getDouble();
            while(buffer.hasRemaining()) {
                result *= buffer.getDouble();
            }
            ((DoubleBuffer)output).put(result);
        }
        buffer.rewind();
    }

    @Override
    public void release() {
        program.release();
    }

    @Override
    public boolean isReleased() {
        return program == null || program.isReleased();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"["+OPERATION+", "+ELEMENT+VECTOR_SIZE+"]";
    }


    public enum Op {ADD, MUL, MIN, MAX}

    private static class CLReductionTask<B extends Buffer> extends CLTask<CLResourceQueueContext<Reduction<B>>, B> {

        private final static int TYPE_ID = 1;

        private final B input;
        private final B output;
        private final Op op;
        private final Class<? extends B> elementType;
        private final Integer KEY;

        private CLReductionTask(B input, B output, Op op, Class<? extends B> elementType) {
            this.input = input;
            this.output = output;
            this.op = op;
            this.elementType = elementType;
            this.KEY = TYPE_ID + op.ordinal()*10 + 1000*ArgType.valueOf(elementType).ordinal();
        }

        @Override
        public CLResourceQueueContext<Reduction<B>> createQueueContext(CLCommandQueue queue) {
            Reduction<B> reduction = Reduction.create(queue, op, elementType);
            return new CLQueueContext.CLResourceQueueContext<Reduction<B>>(queue, reduction);
        }

        @Override
        public B execute(CLResourceQueueContext<Reduction<B>> context) {
            return context.resource.reduce(context.queue, input, output);
        }

        @Override
        public Object getContextKey() {
            return KEY;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()+"["+op+", "+elementType+", "+KEY+"]";
        }
    }

}
