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
 * Created on Tuesday, August 02 2011 22:53
 */
package com.jogamp.opencl.util.concurrent;

import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.util.CLMultiContext;
import com.jogamp.opencl.util.concurrent.CLQueueContext.CLSingleProgramQueueContext;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.System.*;

/**
 *
 * @author Michael Bien
 */
public class CLForkJoinTest {

    private class LogicTest extends CLRecursiveTask<CLQueueContext.CLSingleProgramQueueContext, Integer> {

        private final int size;

        public LogicTest(int size) {
            this.size = size;
        }

        @Override
        public CLSingleProgramQueueContext createQueueContext(CLCommandQueue queue) {
            return new CLSingleProgramQueueContext(queue, "kernel void noop(void){}\n");
        }

        @Override
        public Integer execute(CLSingleProgramQueueContext context) {

            assertNotNull(context);
            assertTrue(context instanceof CLSingleProgramQueueContext);

//            out.println(Thread.currentThread());

            if(size > 8) {
                
                LogicTest task2 = new LogicTest(size/2);
                task2.fork();

                LogicTest task1 = new LogicTest(size/2);

                return task1.compute() + task2.join();
            }else{
                return size;
            }

        }

    }

    @Test
    public void forkJoinTest() throws InterruptedException, ExecutionException {

        CLMultiContext mc = CLMultiContext.create(CLPlatform.listCLPlatforms());

        try{

            CLForkJoinPool pool = CLForkJoinPool.create(mc);

            final int size = 64;
            LogicTest task = new LogicTest(size);
            Future<Integer> future = pool.submit(task);
            assertNotNull(future);
            assertEquals(size, (int)future.get());

            pool.release();

        }finally{
            mc.release();
        }

    }


}
