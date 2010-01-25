package com.mbien.opencl;

import com.mbien.opencl.CLMemory.Mem;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.System.*;
import static com.mbien.opencl.TestUtils.*;
import static com.sun.gluegen.runtime.BufferFactory.*;

/**
 *
 * @author Michael Bien
 */
public class CLConcurrencyTest {

     @Test
     public void testEvents() throws IOException {

        out.println(" - - - event synchronisation test - - - ");

        final int groupSize = 256;
        final int elements = roundUp(groupSize, ONE_MB/SIZEOF_INT * 5); // 5MB per buffer

        CLContext context = CLContext.create();

        CLBuffer<ByteBuffer> clBufferA = context.createByteBuffer(elements*SIZEOF_INT, Mem.READ_ONLY);
        CLBuffer<ByteBuffer> clBufferB = context.createByteBuffer(elements*SIZEOF_INT, Mem.READ_ONLY);
        CLBuffer<ByteBuffer> clBufferC = context.createByteBuffer(elements*SIZEOF_INT, Mem.READ_ONLY);
        CLBuffer<ByteBuffer> clBufferD = context.createByteBuffer(elements*SIZEOF_INT, Mem.READ_ONLY);

        fillBuffer(clBufferA.buffer, 12345);
        fillBuffer(clBufferB.buffer, 67890);

        CLProgram program = context.createProgram(getClass().getResourceAsStream("testkernels.cl")).build();

        CLKernel vectorAddKernel = program.getCLKernel("VectorAddGM")
                                          .setArg(3, elements);

        CLCommandQueue queue = context.getCLDevices()[0].createCommandQueue();

        final CLEventList events = new CLEventList(2);
        
        assertEquals(0, events.size());

        queue.putWriteBuffer(clBufferA, false, events)       // write A
             .putWriteBuffer(clBufferB, false, events);      // write B

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
     public void concurrencyTest() throws IOException, InterruptedException {

        out.println(" - - - queue synchronisation test - - - ");

        final int elements = ONE_MB/SIZEOF_INT * 10; // 20MB per buffer

        CLContext context = CLContext.create();

        CLDevice[] devices = context.getCLDevices();

        if(devices.length < 2) {
            out.println("aborting test... need at least 2 devices");
            context.release();
            return;
        }

        final CLBuffer<ByteBuffer> clBufferC = context.createByteBuffer(elements*SIZEOF_INT, Mem.READ_ONLY);
        final CLBuffer<ByteBuffer> clBufferD = context.createByteBuffer(elements*SIZEOF_INT, Mem.READ_ONLY);

        final CLBuffer<ByteBuffer> clBufferA1 = context.createByteBuffer(elements*SIZEOF_INT, Mem.READ_ONLY);
        final CLBuffer<ByteBuffer> clBufferB1 = context.createByteBuffer(elements*SIZEOF_INT, Mem.READ_ONLY);
        final CLBuffer<ByteBuffer> clBufferA2 = context.createByteBuffer(elements*SIZEOF_INT, Mem.READ_ONLY);
        final CLBuffer<ByteBuffer> clBufferB2 = context.createByteBuffer(elements*SIZEOF_INT, Mem.READ_ONLY);

        CLProgram program = context.createProgram(getClass().getResourceAsStream("testkernels.cl")).build();

        final CLKernel vectorAddKernel1 = program.getCLKernel("VectorAddGM")
                                                .setArg(3, elements);

        //TODO introduce public api for cloning/creating kernels
        final CLKernel vectorAddKernel2 = vectorAddKernel1.copy()
                                                .setArg(3, elements);


        int secondDevice = devices.length > 1 ? 1 : 0;

        final CLCommandQueue queue1 = devices[0           ].createCommandQueue();
        final CLCommandQueue queue2 = devices[secondDevice].createCommandQueue();

        if(secondDevice > 0)
             System.out.println("using two devices");

        final QueueBarrier barrier = new QueueBarrier(2);

        Thread thread1 = new Thread("C") {

            @Override
            public void run() {

                fillBuffer(clBufferA1.buffer, 12345);
                fillBuffer(clBufferB1.buffer, 67890);

//                System.out.println("C buffer");
                queue1.putWriteBuffer(clBufferA1, false)      // write A
                      .putWriteBuffer(clBufferB1, true);      // write B

//                System.out.println("C args");
                vectorAddKernel1.setArgs(clBufferA1, clBufferB1, clBufferC); // C = A+B

//                System.out.println("C kernels");
                CLEventList events1 = new CLEventList(2);
                queue1.put1DRangeKernel(vectorAddKernel1, 0, elements, 256, events1)
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
                queue2.putWriteBuffer(clBufferA2, false)      // write A
                      .putWriteBuffer(clBufferB2, true);      // write B

//                System.out.println("D args");
                vectorAddKernel2.setArgs(clBufferA2, clBufferB2, clBufferD); // D = A+B

//                System.out.println("D kernels");
                CLEventList events2 = new CLEventList(2);
                queue2.put1DRangeKernel(vectorAddKernel2, 0, elements, 256, events2)
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

//        vectorAddKernel2.release();

        out.println("results are valid");

     }




}