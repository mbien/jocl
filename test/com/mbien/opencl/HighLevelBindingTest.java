package com.mbien.opencl;

import com.sun.gluegen.runtime.BufferFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static java.lang.System.*;
import static com.mbien.opencl.TestUtils.*;

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

        int elementCount = 11444777;	// Length of float arrays to process (odd # for illustration)
        int localWorkSize = 256;      // set and log Global and Local work size dimensions
        int globalWorkSize = roundUp(localWorkSize, elementCount);  // rounded up to the nearest multiple of the LocalWorkSize

        out.println("allocateing buffers of size: "+globalWorkSize);

        ByteBuffer srcA = BufferFactory.newDirectByteBuffer(globalWorkSize*BufferFactory.SIZEOF_INT);
        ByteBuffer srcB = BufferFactory.newDirectByteBuffer(globalWorkSize*BufferFactory.SIZEOF_INT);
        ByteBuffer dest = BufferFactory.newDirectByteBuffer(globalWorkSize*BufferFactory.SIZEOF_INT);

        fillBuffer(srcA, 23456);
        fillBuffer(srcB, 46987);

        CLBuffer clBufferA = context.createBuffer(CL.CL_MEM_READ_ONLY, srcA);
        CLBuffer clBufferB = context.createBuffer(CL.CL_MEM_READ_ONLY, srcB);
        CLBuffer clBufferC = context.createBuffer(CL.CL_MEM_WRITE_ONLY, dest);

        Map<String, CLKernel> kernels = program.getCLKernels();
        for (CLKernel kernel : kernels.values()) {
            out.println("kernel: "+kernel.toString());
        }

        assertNotNull(kernels.get("VectorAddGM"));
        assertNotNull(kernels.get("Test"));

        CLKernel vectorAddKernel = kernels.get("VectorAddGM");

        vectorAddKernel.setArg(0, BufferFactory.SIZEOF_LONG, clBufferA)
                       .setArg(1, BufferFactory.SIZEOF_LONG, clBufferB)
                       .setArg(2, BufferFactory.SIZEOF_LONG, clBufferC)
                       .setArg(3, BufferFactory.SIZEOF_INT, elementCount);

        CLCommandQueue queue = programDevices[0].createCommandQueue();

        // Asynchronous write of data to GPU device, blocking read later
        queue.putWriteBuffer(clBufferA, false)
             .putWriteBuffer(clBufferB, false)
             .putNDRangeKernel(vectorAddKernel, 1, null, new long[]{ globalWorkSize }, new long[]{ localWorkSize })
             .putReadBuffer(clBufferC, true).release();

        out.println("a+b=c result snapshot: ");
        for(int i = 0; i < 10; i++)
            out.print(dest.getInt()+", ");
        out.println("...; "+dest.remaining()/BufferFactory.SIZEOF_INT + " more");

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
    
}
