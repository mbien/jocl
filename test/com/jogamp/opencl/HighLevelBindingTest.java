package com.jogamp.opencl;

import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLMemory.GLObjectType;
import com.jogamp.opencl.CLSampler.AddressingMode;
import com.jogamp.opencl.CLSampler.FilteringMode;
import com.jogamp.opencl.CLImageFormat.ChannelOrder;
import com.jogamp.opencl.CLImageFormat.ChannelType;
import com.jogamp.opencl.CLDevice.FPConfig;
import com.jogamp.opencl.CLDevice.GlobalMemCacheType;
import com.jogamp.opencl.CLDevice.LocalMemType;
import com.jogamp.opencl.CLDevice.Type;
import com.jogamp.opencl.CLDevice.Capabilities;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.System.*;
import static com.jogamp.opencl.TestUtils.*;
import static com.jogamp.opencl.util.CLPlatformFilters.*;
import static com.jogamp.opencl.CLVersion.*;
import static com.jogamp.opencl.CLDevice.Type.*;
import static com.jogamp.common.nio.Buffers.*;

/**
 * Test testing the high level bindings.
 * @author Michael Bien
 */
public class HighLevelBindingTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        out.println("OS: " + System.getProperty("os.name"));
        out.println("ARCH: " + System.getProperty("os.arch"));
        out.println("VM: " + System.getProperty("java.vm.name"));
        out.println("lib path: " + System.getProperty("java.library.path"));
    }

    @Test
    public void enumsTest() {
        
        // enum tests
        final EnumSet<FPConfig> singleFPConfig = FPConfig.valuesOf(CL.CL_FP_DENORM | CL.CL_FP_ROUND_TO_INF);
        assertEquals(0, FPConfig.valuesOf(0).size());
        assertTrue(singleFPConfig.contains(FPConfig.DENORM));
        assertTrue(singleFPConfig.contains(FPConfig.ROUND_TO_INF));

        // CLDevice enums
        for (FPConfig e : FPConfig.values()) {
            EnumSet<FPConfig> set = FPConfig.valuesOf(e.CONFIG);
            assertTrue(set.contains(e));
        }
        for (GlobalMemCacheType e : GlobalMemCacheType.values()) {
            assertEquals(e, GlobalMemCacheType.valueOf(e.TYPE));
        }
        for (LocalMemType e : LocalMemType.values()) {
            assertEquals(e, LocalMemType.valueOf(e.TYPE));
        }
        for (Type e : Type.values()) {
            assertEquals(e, Type.valueOf(e.TYPE));
        }
        for (Capabilities e : Capabilities.values()) {
            assertEquals(e, Capabilities.valueOf(e.CAPS));
        }

        // CLMemory enums
        for (Mem e : Mem.values()) {
            assertEquals(e, Mem.valueOf(e.CONFIG));
        }

        for (GLObjectType e : GLObjectType.values()) {
            assertEquals(e, GLObjectType.valueOf(e.TYPE));
        }

        // CLSampler enums
        for (AddressingMode e : AddressingMode.values()) {
            assertEquals(e, AddressingMode.valueOf(e.MODE));
        }
        for (FilteringMode e : FilteringMode.values()) {
            assertEquals(e, FilteringMode.valueOf(e.MODE));
        }

        // CLImage enums
        for (ChannelOrder e : ChannelOrder.values()) {
            assertEquals(e, ChannelOrder.valueOf(e.ORDER));
        }
        for (ChannelType e : ChannelType.values()) {
            assertEquals(e, ChannelType.valueOf(e.TYPE));
        }

    }



    @Test
    public void contextlessTest() {

        out.println(" - - - highLevelTest; contextless - - - ");

        // platform/device info tests
        CLPlatform[] clPlatforms = CLPlatform.listCLPlatforms();

        for (CLPlatform platform : clPlatforms) {

            out.println("platform info:");
            out.println("    name: "+platform.getName());
            out.println("    id: "+platform.ID);
            out.println("    profile: "+platform.getProfile());
            out.println("    spec version: "+platform.getSpecVersion());
            out.println("    impl version: "+platform.getVersion().getImplVersion());
            out.println("    vendor: "+platform.getVendor());
            out.println("    max FLOPS device: "+platform.getMaxFlopsDevice());
            out.println("    extensions: "+platform.getExtensions());

            CLDevice[] clDevices = platform.listCLDevices();
            for (CLDevice device : clDevices) {
                out.println("device info:");
                out.println("    name: "+device.getName());
                out.println("    profile: "+device.getProfile());
                out.println("    vendor: "+device.getVendor());
                out.println("    vendor id: "+device.getVendorID());
                out.println("    version: "+device.getVersion());
//                out.println("    C version: "+device.getCVersion()); //CL 1.1
                out.println("    driver version: "+device.getDriverVersion());
                out.println("    type: "+device.getType());
                out.println("    global mem: "+device.getGlobalMemSize()/(1024*1024)+" MB");
                out.println("    max alloc mem: "+device.getMaxMemAllocSize()/(1024*1024)+" MB");
                out.println("    max param size: "+device.getMaxParameterSize()+" byte");
                out.println("    local mem: "+device.getLocalMemSize()/1024+" KB");
                out.println("    local mem type: "+device.getLocalMemType());
                out.println("    global mem cache size: "+device.getGlobalMemCacheSize());
                out.println("    global mem cacheline size: "+device.getGlobalMemCachelineSize());
                out.println("    global mem cache type: "+device.getGlobalMemCacheType());
                out.println("    constant buffer size: "+device.getMaxConstantBufferSize());
                out.println("    error correction support: "+device.isErrorCorrectionSupported());
                out.println("    queue properties: "+device.getQueueProperties());
                out.println("    clock: "+device.getMaxClockFrequency()+" MHz");
                out.println("    timer res: "+device.getProfilingTimerResolution()+" ns");
                out.println("    max work group size: "+device.getMaxWorkGroupSize());
                out.println("    max compute units: "+device.getMaxComputeUnits());
                out.println("    max work item dimensions: "+device.getMaxWorkItemDimensions());
                out.println("    max work item sizes: "+Arrays.toString(device.getMaxWorkItemSizes()));
                out.println("    compiler available: "+device.isCompilerAvailable());
                out.println("    image support: "+device.isImageSupportAvailable());
                out.println("    max read image args: "+device.getMaxReadImageArgs());
                out.println("    max write image args: "+device.getMaxWriteImageArgs());
                out.println("    max image2d dimensions: "+Arrays.asList(device.getMaxImage2dWidth(), device.getMaxImage2dHeight()));
                out.println("    max image3d dimensions: "+Arrays.asList(device.getMaxImage2dWidth(), device.getMaxImage2dHeight(), device.getMaxImage3dDepth()));
                out.println("    number of address bits: "+device.getAddressBits());
                out.println("    half FP available: "+device.isHalfFPAvailable());
                out.println("    double FP available: "+device.isDoubleFPAvailable());
                out.println("    little endian: "+device.isLittleEndian());
                out.println("    half FP config: "+device.getHalfFPConfig());
                out.println("    single FP config: "+device.getSingleFPConfig());
                out.println("    double FP config: "+device.getDoubleFPConfig());
                out.println("    execution capabilities: "+device.getExecutionCapabilities());
                out.println("    gl memory sharing: "+device.isGLMemorySharingSupported());
                out.println("    extensions: "+device.getExtensions());
            }
        }

    }

    @Test
    public void platformTest() {

        CLPlatform platformGPU = CLPlatform.getDefault(version(CL_1_0), type(GPU));
        CLPlatform platformCPU = CLPlatform.getDefault(version(CL_1_0), type(CPU));
        
        if(platformGPU != null) {
            assertTrue(platformGPU.listCLDevices(GPU).length > 0);
        }else if(platformCPU != null) {
            assertTrue(platformCPU.listCLDevices(CPU).length > 0);
        }else{
            fail("please tell us about your hardware");
        }
    }

    @Test
    public void createContextTest() {

        out.println(" - - - highLevelTest; create context - - - ");

        CLPlatform platform = CLPlatform.getDefault();
        CLDevice[] devices = platform.listCLDevices();
        int deviceCount = devices.length;

        CLContext c = CLContext.create();
        assertNotNull(c);
        assertEquals(deviceCount, c.getDevices().length);
        c.release();

        c = CLContext.create(platform);
        assertNotNull(c);
        assertEquals(deviceCount, c.getDevices().length);
        c.release();

        for (CLDevice device : devices) {
            c = CLContext.create(device);
            assertNotNull(c);
            assertEquals(1, c.getDevices().length);
            c.release();
        }

        c = CLContext.create(CLDevice.Type.ALL);
        assertNotNull(c);
        assertEquals(deviceCount, c.getDevices().length);
        c.release();

        c = CLContext.create(platform, CLDevice.Type.ALL);
        assertNotNull(c);
        assertEquals(deviceCount, c.getDevices().length);
        c.release();


        //Exceptions
        try{
            CLContext.create((CLDevice)null);
            fail("create with null device");
        }catch(IllegalArgumentException ex) {
            // expected
        }
        try{
            CLContext.create((CLDevice.Type)null);
            fail("create with null CLDevice.Type");
        }catch(IllegalArgumentException ex) {
            // expected
        }
        try{
            CLContext.create((CLPlatform)null, (CLDevice.Type)null);
            fail("create with null CLDevice.Type");
        }catch(IllegalArgumentException ex) {
            // expected
        }

    }

    @Test
    public void vectorAddGMTest() throws IOException {

        out.println(" - - - highLevelTest; global memory kernel - - - ");

        CLPlatform[] clPlatforms = CLPlatform.listCLPlatforms();
        CLContext context = CLContext.create(clPlatforms[0]);

        CLDevice[] contextDevices = context.getDevices();

        out.println("context devices:");
        for (CLDevice device : contextDevices) {
            out.println("   "+device.toString());
        }

        out.println("max FLOPS device: " + context.getMaxFlopsDevice());

        CLProgram program = context.createProgram(getClass().getResourceAsStream("testkernels.cl")).build();

        CLDevice[] programDevices = program.getCLDevices();
        CLDevice device = programDevices[0];

        assertEquals(contextDevices.length, programDevices.length);

        out.println("build log:\n"+program.getBuildLog());
        out.println("build status:\n"+program.getBuildStatus());

        String source = program.getSource();
        assertFalse(source.trim().isEmpty());
//        out.println("source:\n"+source);

        Map<CLDevice, byte[]> binaries = program.getBinaries();
        assertFalse(binaries.isEmpty());

        int elementCount = 11444777;	// Length of float arrays to process (odd # for illustration)
        int localWorkSize = device.getMaxWorkItemSizes()[0];      // set and log Global and Local work size dimensions
        int globalWorkSize = roundUp(localWorkSize, elementCount);  // rounded up to the nearest multiple of the LocalWorkSize

        out.println("allocateing buffers of size: "+globalWorkSize);

        ByteBuffer srcA = newDirectByteBuffer(globalWorkSize*SIZEOF_INT);
        ByteBuffer srcB = newDirectByteBuffer(globalWorkSize*SIZEOF_INT);
        ByteBuffer dest = newDirectByteBuffer(globalWorkSize*SIZEOF_INT);

        fillBuffer(srcA, 23456);
        fillBuffer(srcB, 46987);

        CLBuffer<ByteBuffer> clBufferA = context.createBuffer(srcA, Mem.READ_ONLY);
        CLBuffer<ByteBuffer> clBufferB = context.createBuffer(srcB, Mem.READ_ONLY);
        CLBuffer<ByteBuffer> clBufferC = context.createBuffer(dest, Mem.WRITE_ONLY);

        CLKernel vectorAddKernel = program.createCLKernel("VectorAddGM");

        vectorAddKernel.setArg(0, clBufferA)
                       .setArg(1, clBufferB)
                       .setArg(2, clBufferC)
                       .setArg(3, elementCount);

        CLCommandQueue queue = device.createCommandQueue();

        // Asynchronous write of data to GPU device, blocking read later
        queue.putWriteBuffer(clBufferA, false)
             .putWriteBuffer(clBufferB, false)
             .put1DRangeKernel(vectorAddKernel, 0, globalWorkSize, localWorkSize)
             .putReadBuffer(clBufferC, true)
             .finish().release();

        out.println("a+b=c result snapshot: ");
        for(int i = 0; i < 10; i++)
            out.print(dest.getInt()+", ");
        out.println("...; "+dest.remaining()/SIZEOF_INT + " more");

        assertTrue(3 == context.getMemoryObjects().size());
        clBufferA.release();
        assertTrue(2 == context.getMemoryObjects().size());

        assertTrue(2 == context.getMemoryObjects().size());
        clBufferB.release();
        assertTrue(1 == context.getMemoryObjects().size());

        assertTrue(1 == context.getMemoryObjects().size());
        clBufferC.release();
        assertTrue(0 == context.getMemoryObjects().size());


        assertTrue(1 == context.getPrograms().size());
        program.release();
        assertTrue(0 == context.getPrograms().size());

        context.release();
    }
    
}
