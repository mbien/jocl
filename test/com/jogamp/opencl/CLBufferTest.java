package com.jogamp.opencl;

import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLMemory.Map;
import com.jogamp.common.nio.Buffers;
import java.nio.ByteBuffer;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.System.*;
import static com.jogamp.opencl.TestUtils.*;
import static com.jogamp.common.nio.Buffers.*;

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

         // the CL.MEM_* flag is probably completely irrelevant in our case since we do not use a kernel in this test
        CLBuffer<ByteBuffer> clBufferA = context.createByteBuffer(elements*SIZEOF_INT, Mem.READ_ONLY);
        CLBuffer<ByteBuffer> clBufferB = context.createByteBuffer(elements*SIZEOF_INT, Mem.READ_ONLY);

        // fill only first read buffer -> we will copy the payload to the second later.
        fillBuffer(clBufferA.buffer, 12345);

        CLCommandQueue queue = context.getDevices()[0].createCommandQueue();

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

        ByteBuffer buffer = Buffers.newDirectByteBuffer(elements*SIZEOF_INT);
        // fill only first read buffer -> we will copy the payload to the second later.
        fillBuffer(buffer, 12345);

        CLCommandQueue queue = context.getDevices()[0].createCommandQueue();

        Mem[] bufferConfig = new Mem[] {Mem.COPY_BUFFER, Mem.USE_BUFFER};

        for(int i = 0; i < bufferConfig.length; i++) {

            out.println("testing with "+bufferConfig[i] + " config");

            CLBuffer<ByteBuffer> clBufferA = context.createBuffer(buffer, Mem.READ_ONLY, bufferConfig[i]);
            CLBuffer<ByteBuffer> clBufferB = context.createByteBuffer(elements*SIZEOF_INT, Mem.READ_ONLY);

            // asynchronous write of data to GPU device, blocking read later to get the computed results back.
            queue.putCopyBuffer(clBufferA, clBufferB, clBufferA.buffer.capacity()) // copy A -> B
                 .putReadBuffer(clBufferB, true)                                   // read B
                 .finish();

            assertEquals(2, context.getMemoryObjects().size());
            clBufferA.release();
            assertEquals(1, context.getMemoryObjects().size());
            clBufferB.release();
            assertEquals(0, context.getMemoryObjects().size());

            // uploading worked when a==b.
            out.println("validating computed results...");
            checkIfEqual(clBufferA.buffer, clBufferB.buffer, elements);
            out.println("results are valid");
        }

        context.release();
    }
    
    @Test
    public void mapBufferTest() {

        out.println(" - - - highLevelTest; map buffer test - - - ");

        final int elements = NUM_ELEMENTS;
        final int sizeInBytes = elements*SIZEOF_INT;

        CLContext context;
        CLBuffer<?> clBufferA;
        CLBuffer<?> clBufferB;

        // We will have to allocate mappable NIO memory on non CPU contexts
        // since we can't map e.g GPU memory.
        if(CLPlatform.getDefault().listCLDevices(CLDevice.Type.CPU).length > 0) {

            context = CLContext.create(CLDevice.Type.CPU);

            clBufferA = context.createBuffer(sizeInBytes, Mem.READ_WRITE);
            clBufferB = context.createBuffer(sizeInBytes, Mem.READ_WRITE);
        }else{

            context = CLContext.create();

            clBufferA = context.createByteBuffer(sizeInBytes, Mem.READ_WRITE, Mem.USE_BUFFER);
            clBufferB = context.createByteBuffer(sizeInBytes, Mem.READ_WRITE, Mem.USE_BUFFER);
        }

        CLCommandQueue queue = context.getDevices()[0].createCommandQueue();
        
        // fill only first buffer -> we will copy the payload to the second later.
        ByteBuffer mappedBufferA = queue.putMapBuffer(clBufferA, Map.READ_WRITE, true);
        assertEquals(sizeInBytes, mappedBufferA.capacity());

        fillBuffer(mappedBufferA, 12345);           // write to A

        queue.putUnmapMemory(clBufferA)             // unmap A
             .putCopyBuffer(clBufferA, clBufferB);  // copy A -> B

        // map B for read operations
        ByteBuffer mappedBufferB = queue.putMapBuffer(clBufferB, Map.READ, true);
        assertEquals(sizeInBytes, mappedBufferB.capacity());

        out.println("validating computed results...");
        checkIfEqual(mappedBufferA, mappedBufferB, elements); // A == B ?
        out.println("results are valid");

        queue.putUnmapMemory(clBufferB);            // unmap B

        context.release();

    }
    
}
