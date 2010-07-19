package com.jogamp.opencl;

import com.jogamp.opencl.util.MultiQueueBarrier;
import com.jogamp.opencl.CLCommandQueue.Mode;
import com.jogamp.opencl.CLMemory.Mem;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.System.*;
import static com.jogamp.opencl.TestUtils.*;
import static com.jogamp.opencl.CLEvent.*;
import static com.jogamp.opencl.CLVersion.*;
import static com.jogamp.common.nio.Buffers.*;

/**
 *
 * @author Michael Bien
 */
public class CLCommandQueueTest {

    private final int groupSize = 256;

    @Test
    public void enumsTest() {

        //CLCommandQueueEnums
        EnumSet<Mode> queueMode = Mode.valuesOf(CL.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE | CL.CL_QUEUE_PROFILING_ENABLE);
        assertTrue(queueMode.contains(Mode.OUT_OF_ORDER_MODE));
        assertTrue(queueMode.contains(Mode.PROFILING_MODE));

        assertNotNull(Mode.valuesOf(0));
        assertEquals(0, Mode.valuesOf(0).size());
        for (Mode mode : Mode.values()) {
            assertEquals(mode, Mode.valueOf(mode.QUEUE_MODE));
        }

        // CLEvent enums
        for (ProfilingCommand cmd : ProfilingCommand.values()) {
            assertEquals(cmd, ProfilingCommand.valueOf(cmd.COMMAND));
        }

        for (CommandType type : CommandType.values()) {
            assertEquals(type, CommandType.valueOf(type.TYPE));
        }

        for (ExecutionStatus status : ExecutionStatus.values()) {
            assertEquals(status, ExecutionStatus.valueOf(status.STATUS));
        }

    }

    @Test
    public void eventsTest() throws IOException {

        out.println(" - - - event synchronization test - - - ");

        final int elements = roundUp(groupSize, ONE_MB / SIZEOF_INT * 5); // 5MB per buffer

        CLContext context = CLContext.create();

        CLBuffer<ByteBuffer> clBufferA = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
        CLBuffer<ByteBuffer> clBufferB = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
        CLBuffer<ByteBuffer> clBufferC = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
        CLBuffer<ByteBuffer> clBufferD = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);

        fillBuffer(clBufferA.buffer, 12345);
        fillBuffer(clBufferB.buffer, 67890);

        CLProgram program = context.createProgram(getClass().getResourceAsStream("testkernels.cl")).build();
        CLKernel vectorAddKernel = program.createCLKernel("VectorAddGM").setArg(3, elements);
        CLCommandQueue queue = context.getDevices()[0].createCommandQueue();

        out.println(queue);

        final CLEventList events = new CLEventList(2);

        out.println(events);

        assertEquals(0, events.size());

        queue.putWriteBuffer(clBufferA, false, events) // write A
             .putWriteBuffer(clBufferB, false, events);// write B

        out.println(events);

        assertEquals(2, events.size());
        queue.putWaitForEvents(events, true);

        events.release();
        assertEquals(0, events.size());

        vectorAddKernel.setArgs(clBufferA, clBufferB, clBufferC); // C = A+B
        queue.put1DRangeKernel(vectorAddKernel, 0, elements, groupSize, events);

        vectorAddKernel.setArgs(clBufferA, clBufferB, clBufferD); // D = A+B
        queue.put1DRangeKernel(vectorAddKernel, 0, elements, groupSize, events);

        assertEquals(2, events.size());
        queue.putWaitForEvent(events, 0, false)
             .putWaitForEvent(events, 1, true);

        queue.putReadBuffer(clBufferC, false)
             .putReadBuffer(clBufferD, true);

        events.release();

        checkIfEqual(clBufferC.buffer, clBufferD.buffer, elements);


        context.release();


        out.println("results are valid");

    }
    @Test
    public void profilingEventsTest() throws IOException {

        out.println(" - - - event synchronization test - - - ");

        final int elements = roundUp(groupSize, ONE_MB / SIZEOF_INT * 5); // 5MB per buffer

        CLContext context = CLContext.create();

        CLBuffer<ByteBuffer> clBufferA = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
        CLBuffer<ByteBuffer> clBufferB = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
        CLBuffer<ByteBuffer> clBufferC = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);

        fillBuffer(clBufferA.buffer, 12345);
        fillBuffer(clBufferB.buffer, 67890);

        CLProgram program = context.createProgram(getClass().getResourceAsStream("testkernels.cl")).build();
        CLKernel vectorAddKernel = program.createCLKernel("VectorAddGM").setArg(3, elements);
        CLCommandQueue queue = context.getDevices()[0].createCommandQueue(Mode.PROFILING_MODE);

        out.println(queue);

        queue.putWriteBuffer(clBufferA, true) // write A
             .putWriteBuffer(clBufferB, true);// write B

        final CLEventList events = new CLEventList(1);

        assertEquals(0, events.size());

        vectorAddKernel.setArgs(clBufferA, clBufferB, clBufferC); // C = A+B
        queue.put1DRangeKernel(vectorAddKernel, 0, elements, groupSize, events);

        assertEquals(1, events.size());
        CLEvent probe = events.getEvent(0);
        out.println(probe);

        queue.putWaitForEvents(events, true);
        assertEquals(CLEvent.ExecutionStatus.COMPLETE, probe.getStatus());

        out.println(probe);
        long time = probe.getProfilingInfo(CLEvent.ProfilingCommand.END)
                  - probe.getProfilingInfo(CLEvent.ProfilingCommand.START);
        out.println("time: "+time);
        assertTrue(time > 0);

        events.release();
        context.release();

    }

    @Test
    public void customEventsTest() throws IOException, InterruptedException {
        out.println(" - - - user events test - - - ");

        final int elements = roundUp(groupSize, ONE_MB / SIZEOF_INT * 5); // 5MB per buffer

        // 5MB per buffer
        CLPlatform[] platforms = CLPlatform.listCLPlatforms();
        CLPlatform theChosenOne = platforms[0];
        for (CLPlatform platform : platforms) {
            if(platform.isAtLeast(CL_1_1)) {
                theChosenOne = platform;
                break;
            }
        }

        final CLContext context = CLContext.create(theChosenOne);

        // we expect an UOE if CL 1.1 is not supported
        if(!theChosenOne.isAtLeast(CL_1_1)) {
            try{
                CLUserEvent.create(context);
                fail("");
            }catch(UnsupportedOperationException ex) {
                out.println("test dissabled, required CLVersion: "+CL_1_1+" available: "+theChosenOne.getVersion());
                return;
            }
        }

        try{

            CLBuffer<ByteBuffer> clBufferA = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
            CLBuffer<ByteBuffer> clBufferB = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
            CLBuffer<ByteBuffer> clBufferC = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);

            fillBuffer(clBufferA.buffer, 12345);
            fillBuffer(clBufferB.buffer, 67890);

            CLProgram program = context.createProgram(getClass().getResourceAsStream("testkernels.cl")).build();
            CLKernel vectorAddKernel = program.createCLKernel("VectorAddGM").setArg(3, elements);
            CLCommandQueue queue = context.getDevices()[0].createCommandQueue();

            queue.putWriteBuffer(clBufferA, true) // write A
                 .putWriteBuffer(clBufferB, true);// write B

            vectorAddKernel.setArgs(clBufferA, clBufferB, clBufferC); // C = A+B

            // the interesting part...

            CLUserEvent condition = CLUserEvent.create(context);
            assertEquals(CommandType.USER, condition.getType());
            assertEquals(ExecutionStatus.SUBMITTED, condition.getStatus());
            out.println(condition);

            final CLEventList conditions = new CLEventList(condition);
            final CLEventList events     = new CLEventList(1);
            assertEquals(1, conditions.size());
            assertEquals(1, conditions.capacity());
            assertEquals(0, events.size());
            assertEquals(1, events.capacity());

            queue.put1DRangeKernel(vectorAddKernel, 0, elements, groupSize, conditions, events);
            assertEquals(1, events.size());

            Thread.sleep(1000);
            final CLEvent status = events.getEvent(0);

            assertEquals(ExecutionStatus.QUEUED, status.getStatus());
            condition.setComplete();
            assertTrue(condition.isComplete());

            queue.finish();
            assertTrue(status.isComplete());

        }finally{
            context.release();
        }

    }

    @Test
    public void concurrencyTest() throws IOException, InterruptedException {

        out.println(" - - - QueueBarrier test - - - ");

        final int elements = ONE_MB / SIZEOF_INT * 10; // 20MB per buffer

        CLContext context = CLContext.create();

        CLDevice[] devices = context.getDevices();

        // ignore this test if we can't test in parallel
        if (devices.length < 2) {
            out.println("aborting test... need at least 2 devices");
            context.release();
            return;
        }

        final CLBuffer<ByteBuffer> clBufferC = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
        final CLBuffer<ByteBuffer> clBufferD = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);

        final CLBuffer<ByteBuffer> clBufferA1 = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
        final CLBuffer<ByteBuffer> clBufferB1 = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
        final CLBuffer<ByteBuffer> clBufferA2 = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);
        final CLBuffer<ByteBuffer> clBufferB2 = context.createByteBuffer(elements * SIZEOF_INT, Mem.READ_ONLY);

        CLProgram program = context.createProgram(getClass().getResourceAsStream("testkernels.cl")).build();

        //two independent kernel instances
        final CLKernel vectorAddKernel1 = program.createCLKernel("VectorAddGM").setArg(3, elements);
        final CLKernel vectorAddKernel2 = program.createCLKernel("VectorAddGM").setArg(3, elements);

        final CLCommandQueue queue1 = devices[0].createCommandQueue();
        final CLCommandQueue queue2 = devices[1].createCommandQueue();

        out.println(queue1);
        out.println(queue2);

        fillBuffer(clBufferC.buffer, 12345);


        final MultiQueueBarrier barrier = new MultiQueueBarrier(2);

        Thread thread1 = new Thread("C") {

            @Override
            public void run() {

                fillBuffer(clBufferA1.buffer, 12345);
                fillBuffer(clBufferB1.buffer, 67890);

//                System.out.println("C buffer");
                queue1.putWriteBuffer(clBufferA1, false)  // write A
                      .putWriteBuffer(clBufferB1, false); // write B

//                System.out.println("C args");
                vectorAddKernel1.setArgs(clBufferA1, clBufferB1, clBufferC); // C = A+B

//                System.out.println("C kernels");
                CLEventList events1 = new CLEventList(2);
                queue1.put1DRangeKernel(vectorAddKernel1, 0, elements, groupSize, events1)
                      .putReadBuffer(clBufferC, false, events1);

                barrier.waitFor(queue1, events1);

            }
        };

        Thread thread2 = new Thread("D") {

            @Override
            public void run() {

                fillBuffer(clBufferA2.buffer, 12345);
                fillBuffer(clBufferB2.buffer, 67890);

//                System.out.println("D buffer");
                queue2.putWriteBuffer(clBufferA2, false)  // write A
                      .putWriteBuffer(clBufferB2, false); // write B

//                System.out.println("D args");
                vectorAddKernel2.setArgs(clBufferA2, clBufferB2, clBufferD); // D = A+B

//                System.out.println("D kernels");
                CLEventList events2 = new CLEventList(2);
                queue2.put1DRangeKernel(vectorAddKernel2, 0, elements, groupSize, events2)
                      .putReadBuffer(clBufferD, false, events2);

                barrier.waitFor(queue2, events2);

            }
        };

        out.println("starting threads");
        thread1.start();
        thread2.start();
        barrier.await();
        out.println("done");

        checkIfEqual(clBufferC.buffer, clBufferD.buffer, elements);

        context.release();

        out.println("results are valid");

    }
}
