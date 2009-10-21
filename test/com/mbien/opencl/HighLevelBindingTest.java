package com.mbien.opencl;

import com.mbien.opencl.CLBuffer.MEM;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.System.*;
import static com.mbien.opencl.TestUtils.*;
import static com.sun.gluegen.runtime.BufferFactory.*;

/**
 * Test testing the high level bindings.
 * @author Michael Bien
 */
public class HighLevelBindingTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        out.println("OS: " + System.getProperty("os.name"));
        out.println("VM: " + System.getProperty("java.vm.name"));
    }

    @Test
    public void contextlessTest() {

        out.println(" - - - highLevelTest; contextless - - - ");

        CLPlatform[] clPlatforms = CLPlatform.listCLPlatforms();

        for (CLPlatform platform : clPlatforms) {

            out.println("platform info:");
            out.println("    name: "+platform.getName());
            out.println("    profile: "+platform.getProfile());
            out.println("    version: "+platform.getVersion());
            out.println("    vendor: "+platform.getVendor());

            CLDevice[] clDevices = platform.listCLDevices();
            for (CLDevice device : clDevices) {
                out.println("device info:");
                out.println("    name: "+device.getName());
                out.println("    profile: "+device.getProfile());
                out.println("    vendor: "+device.getVendor());
                out.println("    type: "+device.getType());
                out.println("    global mem: "+device.getGlobalMemSize()/(1024*1024)+" MB");
                out.println("    local mem: "+device.getLocalMemSize()/1024+" KB");
                out.println("    clock: "+device.getMaxClockFrequency()+" MHz");
                out.println("    max work group size: "+device.getMaxWorkGroupSize());
                out.println("    max compute units: "+device.getMaxComputeUnits());
                out.println("    extensions: "+device.getExtensions());
            }
        }

    }

    @Test
    public void vectorAddGMTest() throws IOException {

        out.println(" - - - highLevelTest; global memory kernel - - - ");

        CLContext context = CLContext.create();

        CLDevice[] contextDevices = context.getCLDevices();

        out.println("context devices:");
        for (CLDevice device : contextDevices) {
            out.println("   "+device.toString());
        }

        CLProgram program = context.createProgram(getClass().getResourceAsStream("testkernels.cl")).build();

        CLDevice[] programDevices = program.getCLDevices();

        assertEquals(contextDevices.length, programDevices.length);

        out.println("program devices:");
        for (CLDevice device : programDevices) {
            out.println("   "+device.toString());
            out.println("   build log: "+program.getBuildLog(device));
            out.println("   build status: "+program.getBuildStatus(device));
        }

        String source = program.getSource();
        assertFalse(source.trim().isEmpty());
//        out.println("source:\n"+source);

//        Map<CLDevice, byte[]> binaries = program.getBinaries();
//        assertFalse(binaries.isEmpty());

        int elementCount = 11444777;	// Length of float arrays to process (odd # for illustration)
        int localWorkSize = 256;      // set and log Global and Local work size dimensions
        int globalWorkSize = roundUp(localWorkSize, elementCount);  // rounded up to the nearest multiple of the LocalWorkSize

        out.println("allocateing buffers of size: "+globalWorkSize);

        ByteBuffer srcA = newDirectByteBuffer(globalWorkSize*SIZEOF_INT);
        ByteBuffer srcB = newDirectByteBuffer(globalWorkSize*SIZEOF_INT);
        ByteBuffer dest = newDirectByteBuffer(globalWorkSize*SIZEOF_INT);

        fillBuffer(srcA, 23456);
        fillBuffer(srcB, 46987);

        CLBuffer clBufferA = context.createBuffer(srcA, MEM.READ_ONLY);
        CLBuffer clBufferB = context.createBuffer(srcB, MEM.READ_ONLY);
        CLBuffer clBufferC = context.createBuffer(dest, MEM.WRITE_ONLY);

        Map<String, CLKernel> kernels = program.getCLKernels();
        for (CLKernel kernel : kernels.values()) {
            out.println("kernel: "+kernel.toString());
        }

        assertNotNull(kernels.get("VectorAddGM"));
        assertNotNull(kernels.get("Test"));

        CLKernel vectorAddKernel = kernels.get("VectorAddGM");

        vectorAddKernel.setArg(0, clBufferA)
                       .setArg(1, clBufferB)
                       .setArg(2, clBufferC)
                       .setArg(3, elementCount);

        CLCommandQueue queue = programDevices[0].createCommandQueue();

        // Asynchronous write of data to GPU device, blocking read later
        queue.putWriteBuffer(clBufferA, false)
             .putWriteBuffer(clBufferB, false)
             .putNDRangeKernel(vectorAddKernel, 1, null, new long[]{ globalWorkSize }, new long[]{ localWorkSize })
             .putReadBuffer(clBufferC, true)
             .finish().release();

        out.println("a+b=c result snapshot: ");
        for(int i = 0; i < 10; i++)
            out.print(dest.getInt()+", ");
        out.println("...; "+dest.remaining()/SIZEOF_INT + " more");

        assertTrue(3 == context.getCLBuffers().size());
        clBufferA.release();
        assertTrue(2 == context.getCLBuffers().size());

        assertTrue(2 == context.getCLBuffers().size());
        clBufferB.release();
        assertTrue(1 == context.getCLBuffers().size());

        assertTrue(1 == context.getCLBuffers().size());
        clBufferC.release();
        assertTrue(0 == context.getCLBuffers().size());


        assertTrue(1 == context.getCLPrograms().size());
        program.release();
        assertTrue(0 == context.getCLPrograms().size());

//        CLDevice device = ctx.getMaxFlopsDevice();
//        out.println("max FLOPS device: " + device);
        context.release();
    }

    @Test
    public void writeCopyReadBufferTest() throws IOException {

        out.println(" - - - highLevelTest; copy buffer test - - - ");

        final int elements = 10000000; //many..

        CLContext context = CLContext.create();

         // the CL.MEM_* flag is probably completly irrelevant in our case since we do not use a kernel in this test
        CLBuffer clBufferA = context.createBuffer(elements*SIZEOF_INT, MEM.READ_ONLY);
        CLBuffer clBufferB = context.createBuffer(elements*SIZEOF_INT, MEM.READ_ONLY);

        // fill only first read buffer -> we will copy the payload to the second later.
        fillBuffer(clBufferA.buffer, 12345);

        CLCommandQueue queue = context.getCLDevices()[0].createCommandQueue();

        // asynchronous write of data to GPU device, blocking read later to get the computed results back.
        queue.putWriteBuffer(clBufferA, false)                                 // write A
             .putCopyBuffer(clBufferA, clBufferB, clBufferA.buffer.capacity()) // copy A -> B
             .putReadBuffer(clBufferB, true)                                   // read B
             .finish();

        context.release();

        ByteBuffer a = clBufferA.buffer;
        ByteBuffer b = clBufferB.buffer;

        // print first few elements of the resulting buffer to the console.
        out.println("validating computed results...");
        for(int i = 0; i < elements; i++) {
            int aVal = a.getInt();
            int bVal = b.getInt();
            if(aVal != bVal) {
                out.println("a: "+aVal);
                out.println("b: "+bVal);
                out.println("position: "+a.position());
                fail("a!=b");
            }

        }
        out.println("results are valid");

    }
    
}
