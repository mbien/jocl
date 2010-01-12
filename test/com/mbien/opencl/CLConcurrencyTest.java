package com.mbien.opencl;

import com.mbien.opencl.CLBuffer.Mem;
import com.mbien.opencl.CLCommandQueue.Mode;
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

        final int elements = ONE_MB/SIZEOF_INT * 5; // 5MB per buffer

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

        // asynchronous write of data to GPU device, blocking read later to get the computed results back.
        queue.putWriteBuffer(clBufferA, events, false)       // write A
             .putWriteBuffer(clBufferB, events, false);      // write B

        assertEquals(2, events.size());
        queue.putWaitForEvents(events);

        events.release();
        assertEquals(0, events.size());

        vectorAddKernel.setArgs(clBufferA, clBufferB, clBufferC); // C = A+B
        queue.put1DRangeKernel(vectorAddKernel, events, 0, elements, 256);

        vectorAddKernel.setArgs(clBufferA, clBufferB, clBufferD); // D = A+B
        queue.put1DRangeKernel(vectorAddKernel, events, 0, elements, 256);

        assertEquals(2, events.size());
        queue.putWaitForEvent(events, 0)
             .putWaitForEvent(events, 1);

        queue.putReadBuffer(clBufferC, false)
             .putReadBuffer(clBufferD, true);

        events.release();

        checkIfEqual(clBufferC.buffer, clBufferD.buffer, elements);
             

        context.release();


        out.println("results are valid");

     }

}