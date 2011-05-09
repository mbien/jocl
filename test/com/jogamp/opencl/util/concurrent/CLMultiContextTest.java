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
import com.jogamp.opencl.util.concurrent.CLQueueContext.CLSimpleQueueContext;
import com.jogamp.opencl.util.concurrent.CLQueueContextFactory.CLSimpleContextFactory;
import java.nio.IntBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.rules.Timeout;
import com.jogamp.opencl.util.CLMultiContext;
import java.nio.Buffer;
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

    private final class CLTestTask implements CLTask<CLSimpleQueueContext, Buffer> {

        private final Buffer data;

        public CLTestTask(Buffer buffer) {
            this.data = buffer;
        }

        public Buffer execute(CLSimpleQueueContext qc) {
            
            CLCommandQueue queue = qc.getQueue();
            CLContext context = qc.getCLContext();
            CLKernel kernel = qc.getKernel("compute");

            CLBuffer<Buffer> buffer = null;
            try{
                buffer = context.createBuffer(data);
                int gws = buffer.getCLCapacity();

                kernel.putArg(buffer).putArg(gws).rewind();

                queue.putWriteBuffer(buffer, true);
                queue.put1DRangeKernel(kernel, 0, gws, 0);
                queue.putReadBuffer(buffer, true);
            }finally{
                if(buffer != null) {
                    buffer.release();
                }
            }

            return data;
        }

    }

    @Test
    public void commandQueuePoolTest() throws InterruptedException, ExecutionException {

        CLMultiContext mc = CLMultiContext.create(CLPlatform.listCLPlatforms());

        try {

            CLSimpleContextFactory factory = CLQueueContextFactory.createSimple(programSource);
            CLCommandQueuePool<CLSimpleQueueContext> pool = CLCommandQueuePool.create(factory, mc);

            assertTrue(pool.getSize() > 0);

            final int slice = 64;
            final int tasksPerQueue = 10;
            final int taskCount = pool.getSize() * tasksPerQueue;
            
            IntBuffer data = Buffers.newDirectIntBuffer(slice*taskCount);

            List<CLTestTask> tasks = new ArrayList<CLTestTask>(taskCount);

            for (int i = 0; i < taskCount; i++) {
                IntBuffer subBuffer = Buffers.slice(data, i*slice, slice);
                assertEquals(slice, subBuffer.capacity());
                tasks.add(new CLTestTask(subBuffer));
            }

            out.println("invoking "+tasks.size()+" tasks on "+pool.getSize()+" queues");

            // blocking invoke
            pool.invokeAll(tasks);
            checkBuffer(1, data);

            // submit blocking emediatly
            for (CLTestTask task : tasks) {
                pool.submit(task).get();
            }
            checkBuffer(2, data);

            // submitAll using futures
            List<Future<Buffer>> futures = pool.submitAll(tasks);
            for (Future<Buffer> future : futures) {
                future.get();
            }
            checkBuffer(3, data);

            // switching contexts using different program
            factory = CLQueueContextFactory.createSimple(programSource.replaceAll("\\+\\+", "--"));
            pool.switchContext(factory);
            pool.invokeAll(tasks);
            checkBuffer(2, data);

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

}
