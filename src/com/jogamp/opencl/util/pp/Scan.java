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
 * Created on Tuesday, September 20 2011 22:26
 */
package com.jogamp.opencl.util.pp;

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

import static com.jogamp.opencl.CLMemory.Mem.*;

/**
 * Prototype, not ready for general use.
 * @author Michael Bien
 */
/*public */class Scan<B extends Buffer> implements CLResource {

    private static final String SOURCES;

    private final Op OPERATION;
    private final ArgType ELEMENT;

    private final CLProgram program;
    private final CLWork1D smallScan;

    static{
        try {
            StringBuilder sb = new StringBuilder(2048);
            CLUtil.readStream(Scan.class.getResourceAsStream("scan.cl"), sb);
            SOURCES = sb.toString();
        } catch (IOException ex) {
            throw new RuntimeException("can not initialize Reduction.", ex);
        }
    }

    private <B extends Buffer> Scan(CLContext context, Op op, Class<B> elementType) {

        if(!op.equals(Op.ADD)) {
            throw new IllegalArgumentException("only add is supported for now");
        }

        this.ELEMENT = ArgType.valueOf(elementType);
        this.OPERATION = op;

        this.program = context.createProgram(SOURCES);

        CLProgramConfiguration config = program.prepare();
        config.withDefine("OP_"+op.name())
              .withDefine("TYPE", ELEMENT.vectorType(1));
        if(ELEMENT.equals(ArgType.DOUBLE)) {
            config.withDefine("DOUBLE_FP");
        }
        config.build();

        smallScan = CLWork1D.create1D(program.createCLKernel("smallScan"));
    }

    public static <B extends Buffer> Scan<B> create(CLContext context, Op op, Class<? extends B> elementType) {
        return new Scan<B>(context, op, elementType);
    }

    public static <B extends Buffer> Scan<B> create(CLCommandQueue queue, Op op, Class<? extends B> elementType) {
        return create(queue.getContext(), op, elementType);
    }

    public static <B extends Buffer> CLTask<CLResourceQueueContext<Scan<B>>, B> createTask(B input, B output, Op op, Class<? extends B> elementType) {
        return new CLScanTask<B>(input, output, op, elementType);
    }

    public B scan(CLCommandQueue queue, B input, B output) {

        int length = input.capacity();

        int maxSize = (int)smallScan.getKernel().getWorkGroupSize(queue.getDevice());
        if(length > maxSize*2) {
            throw new IllegalArgumentException("buffer was to large for the given hardware");
        }

        int workSize = (length+length%2)/2; // half, rounded up
        int sharedBufferSize = maxSize * ELEMENT.SIZE*2;

        CLContext context = queue.getContext();

        CLBuffer<B> in  = context.createBuffer(input, READ_ONLY);
        CLBuffer<B> out = context.createBuffer(output, WRITE_ONLY);

        smallScan.getKernel().putArg(in).putArg(out).putArgSize(sharedBufferSize).putArg(length).rewind();
        smallScan.setWorkSize(workSize, workSize);

        queue.putWriteBuffer(in, false);
        queue.putWork(smallScan);
        queue.putReadBuffer(out, true);

        in.release();
        out.release();

        return output;
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
        return getClass().getSimpleName()+"["+OPERATION+", "+ELEMENT+"]";
    }

    public enum Op {ADD, MUL, MIN, MAX}

    private static class CLScanTask<B extends Buffer> extends CLTask<CLResourceQueueContext<Scan<B>>, B> {

        private final static int TYPE_ID = 2;

        private final B input;
        private final B output;
        private final Op op;
        private final Class<? extends B> elementType;
        private final Integer KEY;

        private CLScanTask(B input, B output, Op op, Class<? extends B> elementType) {
            this.input = input;
            this.output = output;
            this.op = op;
            this.elementType = elementType;
            this.KEY = TYPE_ID + op.ordinal()*10 + 1000*ArgType.valueOf(elementType).ordinal();
        }

        @Override
        public CLResourceQueueContext<Scan<B>> createQueueContext(CLCommandQueue queue) {
            Scan<B> reduction = Scan.create(queue, op, elementType);
            return new CLQueueContext.CLResourceQueueContext<Scan<B>>(queue, reduction);
        }

        @Override
        public B execute(CLResourceQueueContext<Scan<B>> context) {
            return context.resource.scan(context.queue, input, output);
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
