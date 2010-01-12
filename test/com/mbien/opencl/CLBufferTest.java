package com.mbien.opencl;

import com.mbien.opencl.CLBuffer.Mem;
import com.sun.opengl.util.BufferUtil;
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
public class CLBufferTest {

    @Test
    public void writeCopyReadBufferTest() {

        out.println(" - - - highLevelTest; copy buffer test - - - ");

        final int elements = NUM_ELEMENTS;

        CLContext context = CLContext.create();

         // the CL.MEM_* flag is probably completly irrelevant in our case since we do not use a kernel in this test
        CLBuffer<ByteBuffer> clBufferA = context.createByteBuffer(elements*SIZEOF_INT, Mem.READ_ONLY);
        CLBuffer<ByteBuffer> clBufferB = context.createByteBuffer(elements*SIZEOF_INT, Mem.READ_ONLY);

        // fill only first read buffer -> we will copy the payload to the second later.
        fillBuffer(clBufferA.buffer, 12345);

        CLCommandQueue queue = context.getCLDevices()[0].createCommandQueue();

        // asynchronous write of data to GPU device, blocking read later to get the computed results back.
        queue.putWriteBuffer(clBufferA, false)                                 // write A
             .putCopyBuffer(clBufferA, clBufferB, clBufferA.buffer.capacity()) // copy A -> B
             .putReadBuffer(clBufferB, true)                                   // read B
             .finish();

        context.release();

        out.println("validating computed results...");
        checkIfEqual(clBufferA.buffer, clBufferB.buffer, elements);
        out.println("results are valid");

    }

    @Test
    public void bufferWithHostPointerTest() {

        out.println(" - - - highLevelTest; host pointer test - - - ");

        final int elements = NUM_ELEMENTS;

        CLContext context = CLContext.create();

        ByteBuffer buffer = BufferUtil.newByteBuffer(elements*SIZEOF_INT);
        // fill only first read buffer -> we will copy the payload to the second later.
        fillBuffer(buffer, 12345);

        CLCommandQueue queue = context.getCLDevices()[0].createCommandQueue();

        Mem[] bufferConfig = new Mem[] {Mem.COPY_BUFFER, Mem.USE_BUFFER};

        for(int i = 0; i < bufferConfig.length; i++) {

            out.println("testing with "+bufferConfig[i] + " config");

            CLBuffer<ByteBuffer> clBufferA = context.createBuffer(buffer, Mem.READ_ONLY, bufferConfig[i]);
            CLBuffer<ByteBuffer> clBufferB = context.createByteBuffer(elements*SIZEOF_INT, Mem.READ_ONLY);

            // asynchronous write of data to GPU device, blocking read later to get the computed results back.
            queue.putCopyBuffer(clBufferA, clBufferB, clBufferA.buffer.capacity()) // copy A -> B
                 .putReadBuffer(clBufferB, true)                                   // read B
                 .finish();

            assertEquals(2, context.getCLBuffers().size());
            clBufferA.release();
            assertEquals(1, context.getCLBuffers().size());
            clBufferB.release();
            assertEquals(0, context.getCLBuffers().size());

            // uploading worked when a==b.
            out.println("validating computed results...");
            checkIfEqual(clBufferA.buffer, clBufferB.buffer, elements);
            out.println("results are valid");
        }

        context.release();
    }
}
