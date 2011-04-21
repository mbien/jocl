/*
 * Copyright 2010 JogAmp Community. All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package com.jogamp.opencl;

import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLMemory.Map;
import com.jogamp.common.nio.Buffers;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.System.*;
import static com.jogamp.opencl.TestUtils.*;
import static com.jogamp.common.nio.Buffers.*;
import static com.jogamp.opencl.util.CLPlatformFilters.*;
import static com.jogamp.opencl.CLVersion.*;

/**
 *
 * @author Michael Bien
 */
public class CLBufferTest {


    @Test
    public void createBufferTest() {

        out.println(" - - - highLevelTest; create buffer test - - - ");

        CLContext context = CLContext.create();
        try{
            int size = 6;

            CLBuffer<ByteBuffer> bb = context.createByteBuffer(size);
            CLBuffer<ShortBuffer> sb = context.createShortBuffer(size);
            CLBuffer<IntBuffer> ib = context.createIntBuffer(size);
            CLBuffer<LongBuffer> lb = context.createLongBuffer(size);
            CLBuffer<FloatBuffer> fb = context.createFloatBuffer(size);
            CLBuffer<DoubleBuffer> db = context.createDoubleBuffer(size);

            List<CLMemory<? extends Buffer>> buffers = context.getMemoryObjects();
            assertEquals(6, buffers.size());

            assertEquals(1, bb.getElementSize());
            assertEquals(2, sb.getElementSize());
            assertEquals(4, ib.getElementSize());
            assertEquals(8, lb.getElementSize());
            assertEquals(4, fb.getElementSize());
            assertEquals(8, db.getElementSize());

            ByteBuffer anotherNIO = newDirectByteBuffer(2);

            for (CLMemory<? extends Buffer> memory : buffers) {

                CLBuffer<? extends Buffer> buffer = (CLBuffer<? extends Buffer>) memory;
                Buffer nio = buffer.getBuffer();

                assertEquals(nio.capacity(), buffer.getCLCapacity());
                assertEquals(buffer.getNIOSize(), buffer.getCLSize());
                assertEquals(sizeOfBufferElem(nio), buffer.getElementSize());
                assertEquals(nio.capacity() * sizeOfBufferElem(nio), buffer.getCLSize());
                
                CLBuffer<ByteBuffer> clone = buffer.cloneWith(anotherNIO);

                assertEquals(buffer.ID, clone.ID);
                assertTrue(clone.equals(buffer));
                assertTrue(buffer.equals(clone));

                assertEquals(buffer.getCLSize(), clone.getCLCapacity());
                assertEquals(buffer.getCLSize(), clone.getCLSize());
                assertEquals(anotherNIO.capacity(), clone.getNIOCapacity());
            }

        }finally{
            context.release();
        }

    }

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
        ByteBuffer mappedBufferA = queue.putMapBuffer(clBufferA, Map.WRITE, true);
        assertEquals(sizeInBytes, mappedBufferA.capacity());

        fillBuffer(mappedBufferA, 12345);                // write to A

        queue.putUnmapMemory(clBufferA, mappedBufferA)// unmap A
             .putCopyBuffer(clBufferA, clBufferB);    // copy A -> B

        // map B for read operations
        ByteBuffer mappedBufferB = queue.putMapBuffer(clBufferB, Map.READ, true);
        assertEquals(sizeInBytes, mappedBufferB.capacity());

        out.println("validating computed results...");
        checkIfEqual(mappedBufferA, mappedBufferB, elements); // A == B ?
        out.println("results are valid");

        queue.putUnmapMemory(clBufferB, mappedBufferB);     // unmap B

        context.release();

    }

    @Test
    public void subBufferTest() {

        out.println(" - - - subBufferTest - - - ");

        CLPlatform platform = CLPlatform.getDefault(version(CL_1_1));
        if(platform == null) {
            out.println("aborting subBufferTest");
            return;
        }

        CLContext context = CLContext.create(platform);
        try{
            final int subelements = 5;
            // device only
            {
                CLBuffer<?> buffer = context.createBuffer(64);

                assertFalse(buffer.isSubBuffer());
                assertNotNull(buffer.getSubBuffers());
                assertTrue(buffer.getSubBuffers().isEmpty());

                CLSubBuffer<?> subBuffer = buffer.createSubBuffer(10, subelements);

                assertTrue(subBuffer.isSubBuffer());
                assertEquals(subelements, subBuffer.getCLSize());
                assertEquals(10, subBuffer.getOffset());
                assertEquals(10, subBuffer.getCLOffset());
                assertEquals(buffer, subBuffer.getParent());
                assertEquals(1, buffer.getSubBuffers().size());

                subBuffer.release();
                assertEquals(0, buffer.getSubBuffers().size());
            }

            // device + direct buffer
            {
                CLBuffer<FloatBuffer> buffer = context.createFloatBuffer(64);
                assertFalse(buffer.isSubBuffer());
                assertNotNull(buffer.getSubBuffers());
                assertTrue(buffer.getSubBuffers().isEmpty());

                CLSubBuffer<FloatBuffer> subBuffer = buffer.createSubBuffer(10, subelements);

                assertTrue(subBuffer.isSubBuffer());
                assertEquals(subelements, subBuffer.getBuffer().capacity());
                assertEquals(10, subBuffer.getOffset());
                assertEquals(40, subBuffer.getCLOffset());
                assertEquals(buffer, subBuffer.getParent());
                assertEquals(1, buffer.getSubBuffers().size());

                assertEquals(subBuffer.getCLCapacity(), subBuffer.getBuffer().capacity());

                subBuffer.release();
                assertEquals(0, buffer.getSubBuffers().size());
            }

        }finally{
            context.release();
        }

    }

    @Test
    public void destructorCallbackTest() throws InterruptedException {

        out.println(" - - - destructorCallbackTest - - - ");

        CLPlatform platform = CLPlatform.getDefault(version(CL_1_1));
        if(platform == null) {
            out.println("aborting destructorCallbackTest");
            return;
        }

        CLContext context = CLContext.create(platform);

        try{

            final CLBuffer<?> buffer = context.createBuffer(32);
            final CountDownLatch countdown = new CountDownLatch(1);

            buffer.registerDestructorCallback(new CLMemObjectListener() {
                public void memoryDeallocated(CLMemory<?> mem) {
                    out.println("buffer released");
                    assertEquals(mem, buffer);
                    countdown.countDown();
                }
            });
            buffer.release();

            countdown.await(2, TimeUnit.SECONDS);
            assertEquals(countdown.getCount(), 0);

        }finally{
            context.release();
        }


    }


}
