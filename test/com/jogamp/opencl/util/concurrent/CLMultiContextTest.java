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
 * Created on Tuesday, May 03 2011
 */
package com.jogamp.opencl.util.concurrent;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.util.concurrent.CLQueueContext.CLSingleProgramQueueContext;
import java.nio.IntBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.rules.Timeout;
import com.jogamp.opencl.util.CLMultiContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.System.*;

/**
 *
 * @author Michael Bien
 */
public class CLMultiContextTest {

    @Rule
    public MethodRule methodTimeout= new Timeout(10000);

    @Test
    public void createMultiContextTest() {

        CLMultiContext mc = CLMultiContext.create(CLPlatform.listCLPlatforms());

        try{
            List<CLContext> contexts = mc.getContexts();
            List<CLDevice> devices = mc.getDevices();

            assertFalse(contexts.isEmpty());
            assertFalse(devices.isEmpty());

            for (CLContext context : contexts) {
                out.println(context);
            }
            for (CLDevice device : devices) {
                out.println(device);
            }

        }finally{
            mc.release();
        }

    }

    private final static String programSource =
          "kernel void compute(global int* array, int numElements) { \n"
        + "    int index = get_global_id(0);                         \n"
        + "    if (index >= numElements)  {                          \n"
        + "        return;                                           \n"
        + "    }                                                     \n"
        + "    array[index]++;                                       \n"
        + "}                                                         \n";

    private final class CLTestTask extends CLTask<CLSingleProgramQueueContext, IntBuffer> {

        private final IntBuffer data;
        private final String source;

        public CLTestTask(String source, IntBuffer buffer) {
            this.data = buffer;
            this.source = source;
        }

        @Override
        public CLSingleProgramQueueContext createQueueContext(CLCommandQueue queue) {
            return new CLSingleProgramQueueContext(queue, source);
        }

        @Override
        public IntBuffer execute(CLSingleProgramQueueContext qc) {
            
            CLCommandQueue queue = qc.getQueue();
            CLContext context = qc.getCLContext();
            CLKernel kernel = qc.getKernel("compute");

//            System.out.println(Thread.currentThread().getName()+" / "+queue);
            assertFalse(qc.isReleased());
            assertFalse(queue.isReleased());
            assertFalse(context.isReleased());
            assertFalse(kernel.isReleased());

            CLBuffer<IntBuffer> buffer = null;
            try{

                buffer = context.createBuffer(data);
                int gws = buffer.getCLCapacity();

                kernel.putArg(buffer).putArg(gws).rewind();

                queue.putWriteBuffer(buffer, false);
                queue.put1DRangeKernel(kernel, 0, gws, 0);
                queue.putReadBuffer(buffer, true);
            }finally{
                if(buffer != null) {
                    buffer.release();
                }
            }

            return data;
        }

        @Override
        public Object getContextKey() {
            return source.hashCode();
        }

    }

    private List<CLTestTask> createTasks(String source, IntBuffer data, int taskCount, int slice) {
        List<CLTestTask> tasks = new ArrayList<CLTestTask>(taskCount);
        for (int i = 0; i < taskCount; i++) {
            IntBuffer subBuffer = Buffers.slice(data, i*slice, slice);
            assertEquals(slice, subBuffer.capacity());
            tasks.add(new CLTestTask(source, subBuffer));
        }
        return tasks;
    }

    @Test
    public void commandQueuePoolTest() throws InterruptedException, ExecutionException {

        CLMultiContext mc = CLMultiContext.create(CLPlatform.listCLPlatforms());

        try {

            CLCommandQueuePool pool = CLCommandQueuePool.create(mc);

            assertTrue(pool.getPoolSize() > 0);

            final int slice = 64;
            final int tasksPerQueue = 10;
            final int taskCount = pool.getPoolSize() * tasksPerQueue;
            
            IntBuffer data = Buffers.newDirectIntBuffer(slice*taskCount);
            List<CLTestTask> tasks = createTasks(programSource, data, taskCount, slice);

            out.println("invoking "+tasks.size()+" tasks on "+pool.getPoolSize()+" queues");

            // blocking invoke
            List<Future<IntBuffer>> results = pool.invokeAll(tasks);
            assertNotNull(results);
            checkBuffer(1, data);

            // submit blocking emediatly
            for (CLTestTask task : tasks) {
                IntBuffer ret = pool.submit(task).get();
                assertNotNull(ret);
                checkBuffer(2, ret);
            }
            checkBuffer(2, data);

            // submitAll using futures
            List<Future<IntBuffer>> futures = pool.submitAll(tasks);
            for (Future<IntBuffer> future : futures) {
                IntBuffer ret = future.get();
                assertNotNull(ret);
                checkBuffer(3, ret);
            }
            checkBuffer(3, data);

            // switching contexts using different program
            final String decrementProgramSource = programSource.replaceAll("\\+\\+", "--");
            tasks = createTasks(decrementProgramSource, data, taskCount, slice);
            List<Future<IntBuffer>> results2 = pool.invokeAll(tasks);
            assertNotNull(results2);
            checkBuffer(2, data);

            // Note: we have to make sure that we don't resubmit old tasks at this point since
            // we wait only for completion of a subset of tasks.
            // submit any
            data = Buffers.newDirectIntBuffer(slice*taskCount);
            tasks = createTasks(decrementProgramSource, data, taskCount, slice);

            IntBuffer ret1 = pool.invokeAny(tasks);
            assertNotNull(ret1);
            checkBuffer(-1, ret1);
            checkContains(-1, data);

            // completionservice take/any test
            data = Buffers.newDirectIntBuffer(slice*taskCount);
            tasks = createTasks(decrementProgramSource, data, taskCount, slice);

            CLTaskCompletionService<IntBuffer> service = new CLTaskCompletionService(pool);
            for (CLTestTask task : tasks) {
                service.submit(task);
            }
            IntBuffer ret2 = service.take().get();
            assertNotNull(ret2);
            checkBuffer(-1, ret2);
            checkContains(-1, data);

            pool.release();
        }finally{
            mc.release();
        }
    }

    private void checkBuffer(int expected, IntBuffer data) {
        while(data.hasRemaining()) {
            assertEquals(expected, data.get());
        }
        data.rewind();
    }

    private void checkContains(int expected, IntBuffer data) {
        while(data.hasRemaining()) {
            if(expected == data.get()){
                data.rewind();
                return;
            }
        }
        fail();
    }

//    @Test
    public void loadTest() throws InterruptedException, ExecutionException {
        for (int i = 0; i < 40; i++) {
            commandQueuePoolTest();
        }
    }

}
