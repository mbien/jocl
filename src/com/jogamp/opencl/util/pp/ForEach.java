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

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLProgram;
import com.jogamp.opencl.CLResource;
import com.jogamp.opencl.CLWork.CLWork1D;
import com.jogamp.opencl.util.CLUtil;
import com.jogamp.opencl.util.concurrent.CLQueueContext;
import com.jogamp.opencl.util.concurrent.CLQueueContext.CLResourceQueueContext;
import com.jogamp.opencl.util.concurrent.CLTask;
import java.nio.Buffer;

/**
 *
 * @author Michael Bien
 */
public class ForEach<B extends Buffer> implements CLResource {
    
    private final CLProgram program;
    private final CLWork1D foreach;

    public ForEach(CLContext context, Class<B> elementType, String body) {
        
        StringBuilder src = new StringBuilder(512+body.length());
        src.append("kernel void foreach(");
        arg(src, elementType, "a").append(", ");
        arg(src, elementType, "b").append(", ");
        src.append("uint length").append(") {\n");
        src.append("    uint globalID = get_global_id(0);\n");
        src.append("    if(globalID >= length) return;\n");
        src.append(body);
        src.append("\n}\n");
//        System.out.println(src);
        
        program = context.createProgram(src.toString()).build();
        foreach = CLWork1D.create1D(program.createCLKernel("foreach"));
    }
    
    public static <B extends Buffer> ForEach<B> create(CLContext context, String op, Class<B> elementType) {
        return new ForEach<B>(context, elementType, op);
    }
    
    public static <B extends Buffer> ForEach<B> create(CLCommandQueue queue, String op, Class<B> elementType) {
        return create(queue.getContext(), op, elementType);
    }

    public static <B extends Buffer> CLTask<CLResourceQueueContext<ForEach<B>>, B> createTask(B input, B output, String body, Class<B> elementType) {
        return new CLForEachTask<B>(input, output, body, elementType);
    }
    
    public B foreach(CLCommandQueue queue, B input, B output) {
        
        int length = input.capacity();
        
        int groupSize = (int)foreach.getKernel().getWorkGroupSize(queue.getDevice());
        int workItems = CLUtil.roundUp(length, groupSize);

        CLContext context = queue.getContext();
        
        CLBuffer<B> in = context.createBuffer(input);
        CLBuffer<B> out = context.createBuffer(output);
        
        foreach.getKernel().putArgs(in, out).putArg(length).rewind();
        foreach.setWorkSize(workItems, groupSize);
        
        queue.putWriteBuffer(in, false);
        queue.putWork(foreach);
        queue.putReadBuffer(out, true);
        
        in.release();
        out.release();
        
        return output;
    }

    private StringBuilder arg(StringBuilder builder, Class<B> elementType, String name) {
        String type = ArgType.valueOf(elementType).type();
        return builder.append("global").append(' ').append(type).append("* ").append(name);
    }

    @Override
    public void release() {
        program.release();
    }

    @Override
    public boolean isReleased() {
        return program.isReleased();
    }
    
    private static class CLForEachTask<B extends Buffer> extends CLTask<CLResourceQueueContext<ForEach<B>>, B> {

        private final B input;
        private final B output;
        private final Class<B> elementType;
        private final String body;

        private CLForEachTask(B input, B output, String body, Class<B> elementType) {
            this.input = input;
            this.output = output;
            this.elementType = elementType;
            this.body = body;
        }

        @Override
        public CLResourceQueueContext<ForEach<B>> createQueueContext(CLCommandQueue queue) {
            ForEach<B> foreach = ForEach.create(queue.getContext(), body, elementType);
            return new CLQueueContext.CLResourceQueueContext<ForEach<B>>(queue, foreach);
        }

        @Override
        public B execute(CLResourceQueueContext<ForEach<B>> context) {
            return context.resource.foreach(context.queue, input, output);
        }

        @Override
        public Object getContextKey() {
            return body;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()+"["+elementType+"]";
        }
    }
}
