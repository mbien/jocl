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

    private final class CLTestTask implements CLTask<CLSimpleQueueContext, IntBuffer> {

        private final IntBuffer data;

        public CLTestTask(IntBuffer buffer) {
            this.data = buffer;
        }

        @Override
        public IntBuffer execute(CLSimpleQueueContext qc) {
            
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

    }

    private List<CLTestTask> createTasks(IntBuffer data, int taskCount, int slice) {
        List<CLTestTask> tasks = new ArrayList<CLTestTask>(taskCount);
        for (int i = 0; i < taskCount; i++) {
            IntBuffer subBuffer = Buffers.slice(data, i*slice, slice);
            assertEquals(slice, subBuffer.capacity());
            tasks.add(new CLTestTask(subBuffer));
        }
        return tasks;
    }

    @Test
    public void commandQueuePoolTest() throws InterruptedException, ExecutionException {

        CLMultiContext mc = CLMultiContext.create(CLPlatform.listCLPlatforms());

        try {

            CLSimpleContextFactory factory = CLQueueContextFactory.createSimple(programSource);
            CLCommandQueuePool<CLSimpleQueueContext> pool = CLCommandQueuePool.create(factory, mc);

            assertTrue(pool.getPoolSize() > 0);

            final int slice = 64;
            final int tasksPerQueue = 10;
            final int taskCount = pool.getPoolSize() * tasksPerQueue;
            
            IntBuffer data = Buffers.newDirectIntBuffer(slice*taskCount);
            List<CLTestTask> tasks = createTasks(data, taskCount, slice);

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
            factory = CLQueueContextFactory.createSimple(programSource.replaceAll("\\+\\+", "--"));
            pool.switchContext(factory);
            List<Future<IntBuffer>> results2 = pool.invokeAll(tasks);
            assertNotNull(results2);
            checkBuffer(2, data);

            // Note: we have to make sure that we don't resubmit old tasks at this point since
            // we wait only for completion of a subset of tasks.
            // submit any
            data = Buffers.newDirectIntBuffer(slice*taskCount);
            tasks = createTasks(data, taskCount, slice);

            IntBuffer ret1 = pool.invokeAny(tasks);
            assertNotNull(ret1);
            checkBuffer(-1, ret1);
            checkContains(-1, data);

            // completionservice take/any test
            data = Buffers.newDirectIntBuffer(slice*taskCount);
            tasks = createTasks(data, taskCount, slice);

            CLTaskCompletionService<CLSimpleQueueContext, IntBuffer> service = new CLTaskCompletionService(pool);
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
