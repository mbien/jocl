package com.mbien.opencl;

import com.sun.gluegen.runtime.BufferFactory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static java.lang.System.*;

/**
 * Test for testing basic functionality.
 * @author Michael Bien
 */
public class JOCLTest {

    public JOCLTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        out.println("OS: " + System.getProperty("os.name"));
        out.println("VM: " + System.getProperty("java.vm.name"));
    }

    @Test
    public void lowLevelTest1() {

        out.println(" - - - lowLevelTest; contextless binding - - - ");

        int ret = CL.CL_SUCCESS;

        CL cl = CLContext.getLowLevelBinding();

        int[] intBuffer = new int[1];
        // find all available OpenCL platforms
        ret = cl.clGetPlatformIDs(0, null, 0, intBuffer, 0);
        checkForError(ret);
        out.println("#platforms: "+intBuffer[0]);

        long[] platformId = new long[intBuffer[0]];
        ret = cl.clGetPlatformIDs(platformId.length, platformId, 0, null, 0);
        checkForError(ret);

        // print platform info
        long[] longBuffer = new long[1];
        ByteBuffer bb = ByteBuffer.allocate(128);
        bb.order(ByteOrder.nativeOrder());

        for (int i = 0; i < platformId.length; i++)  {

            long platform = platformId[i];
            out.println("platform id: "+platform);

            ret = cl.clGetPlatformInfo(platform, CL.CL_PLATFORM_PROFILE, bb.capacity(), bb, longBuffer, 0);
            checkForError(ret);
            out.println("    profile: "+new String(bb.array(), 0, (int)longBuffer[0]));

            ret = cl.clGetPlatformInfo(platform, CL.CL_PLATFORM_VERSION, bb.capacity(), bb, longBuffer, 0);
            checkForError(ret);
            out.println("    version: "+new String(bb.array(), 0, (int)longBuffer[0]));

            ret = cl.clGetPlatformInfo(platform, CL.CL_PLATFORM_NAME, bb.capacity(), bb, longBuffer, 0);
            checkForError(ret);
            out.println("    name: "+new String(bb.array(), 0, (int)longBuffer[0]));

            ret = cl.clGetPlatformInfo(platform, CL.CL_PLATFORM_VENDOR, bb.capacity(), bb, longBuffer, 0);
            checkForError(ret);
            out.println("    vendor: "+new String(bb.array(), 0, (int)longBuffer[0]));

            //find all devices
            ret = cl.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_ALL, 0, null, 0, intBuffer, 0);
            checkForError(ret);
            out.println("#devices: "+intBuffer[0]);

            long[] devices = new long[intBuffer[0]];
            ret = cl.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_ALL, devices.length, devices, 0, null, 0);

            //print device info
            for (int j = 0; j < devices.length; j++) {
                long device = devices[j];
                ret = cl.clGetDeviceInfo(device, CL.CL_DEVICE_NAME, bb.capacity(), bb, longBuffer, 0);
                checkForError(ret);
                out.println("    device: "+new String(bb.array(), 0, (int)longBuffer[0]));

                ret = cl.clGetDeviceInfo(device, CL.CL_DEVICE_TYPE, bb.capacity(), bb, longBuffer, 0);
                checkForError(ret);
                out.println("    type: " + CLDevice.Type.valueOf(bb.get()));
                bb.rewind();

            }

        }

    }

    @Test
    public void lowLevelTest2() {

        out.println(" - - - lowLevelTest2; VectorAdd kernel - - - ");

//        CreateContextCallback cb = new CreateContextCallback() {
//            @Override
//            public void createContextCallback(String errinfo, ByteBuffer private_info, long cb, Object user_data) {
//                throw new RuntimeException("not yet implemented...");
//            }
//        };

        long[] longBuffer = new long[1];
        ByteBuffer bb = ByteBuffer.allocate(4096).order(ByteOrder.nativeOrder());

        CL cl = CLContext.getLowLevelBinding();

        int ret = CL.CL_SUCCESS;
        int[] intArray = new int[1];

        long context = cl.clCreateContextFromType(null, 0, CL.CL_DEVICE_TYPE_ALL, null, null, null, 0);
        out.println("context handle: "+context);

        // TODO fix gluegen bug: array-buffer mixing... bb is a noop
        ret = cl.clGetContextInfo(context, CL.CL_CONTEXT_DEVICES, 0, bb, longBuffer, 0);
        checkError("on clGetContextInfo", ret);

        int sizeofLong = 8; // TODO sizeof long...
        out.println("context created with " + longBuffer[0]/sizeofLong + " devices");

        ret = cl.clGetContextInfo(context, CL.CL_CONTEXT_DEVICES, bb.capacity(), bb, null, 0);
        checkError("on clGetContextInfo", ret);

        for (int i = 0; i < longBuffer[0]/sizeofLong; i++) {
            out.println("device id: "+bb.getLong());
        }

        long firstDeviceID = bb.getLong(0);

        // Create a command-queue
        long commandQueue = cl.clCreateCommandQueue(context, firstDeviceID, 0, intArray, 0);
        checkError("on clCreateCommandQueue", intArray[0]);

        int elementCount = 11444777;	// Length of float arrays to process (odd # for illustration)
        int localWorkSize = 256;      // set and log Global and Local work size dimensions
        int globalWorkSize = roundUp(localWorkSize, elementCount);  // rounded up to the nearest multiple of the LocalWorkSize
        int sizeofFloat = 4; // TODO sizeof float ...

        // Allocate the OpenCL buffer memory objects for source and result on the device GMEM
        long devSrcA = cl.clCreateBuffer(context, CL.CL_MEM_READ_ONLY, sizeofFloat * globalWorkSize, null, intArray, 0);
        checkError("on clCreateBuffer", intArray[0]);
        long devSrcB = cl.clCreateBuffer(context, CL.CL_MEM_READ_ONLY, sizeofFloat * globalWorkSize, null, intArray, 0);
        checkError("on clCreateBuffer", intArray[0]);
        long devDst  = cl.clCreateBuffer(context, CL.CL_MEM_WRITE_ONLY, sizeofFloat * globalWorkSize, null, intArray, 0);
        checkError("on clCreateBuffer", intArray[0]);

        String src =
              " // OpenCL Kernel Function for element by element vector addition                                                \n"
            + "__kernel void VectorAdd(__global const float* a, __global const float* b, __global float* c, int iNumElements) { \n"
            + "    // get index into global data array                                                                          \n"
            + "    int iGID = get_global_id(0);                                                                                 \n"
            + "    // bound check (equivalent to the limit on a 'for' loop for standard/serial C code                           \n"
            + "    if (iGID >= iNumElements)  {                                                                                 \n"
            + "        return;                                                                                                  \n"
            + "    }                                                                                                            \n"
            + "    // add the vector elements                                                                                   \n"
            + "    c[iGID] = a[iGID] + b[iGID];                                                                                 \n"
            + "}                                                                                                                \n";


        // Create the program
        long program = cl.clCreateProgramWithSource(context, 1, new String[] {src}, new long[]{src.length()}, 0, intArray, 0);
        checkError("on clCreateProgramWithSource", intArray[0]);

        // Build the program
        ret = cl.clBuildProgram(program, null, null, null, null);
        checkError("on clBuildProgram", ret);

        // Read program infos
        bb.rewind();
        ret = cl.clGetProgramInfo(program, CL.CL_PROGRAM_NUM_DEVICES, bb.capacity(), bb, null, 0);
        checkError("on clGetProgramInfo1", ret);
        out.println("program associated with "+bb.getInt(0)+" device(s)");

        ret = cl.clGetProgramInfo(program, CL.CL_PROGRAM_SOURCE, 0, bb, longBuffer, 0);
        checkError("on clGetProgramInfo CL_PROGRAM_SOURCE", ret);
        out.println("program source length (cl): "+longBuffer[0]);
        out.println("program source length (java): "+src.length());

        bb.rewind();
        ret = cl.clGetProgramInfo(program, CL.CL_PROGRAM_SOURCE, bb.capacity(), bb, null, 0);
        checkError("on clGetProgramInfo CL_PROGRAM_SOURCE", ret);
        out.println("program source:\n"+new String(bb.array(), 0, (int)longBuffer[0]));

        // Check program status
        Arrays.fill(longBuffer, 42);
        bb.rewind();
        ret = cl.clGetProgramBuildInfo(program, firstDeviceID, CL.CL_PROGRAM_BUILD_STATUS, bb.capacity(), bb, null, 0);
        checkError("on clGetProgramBuildInfo1", ret);

        out.println("program build status: " + getBuildStatus(bb.getInt(0)));
        assertEquals("build status", CL.CL_BUILD_SUCCESS, bb.getInt(0));

        // Read build log
        // TODO fix gluegen bug: array-buffer mixing... bb is a noop
        ret = cl.clGetProgramBuildInfo(program, firstDeviceID, CL.CL_PROGRAM_BUILD_LOG, 0, bb, longBuffer, 0);
        checkError("on clGetProgramBuildInfo2", ret);
        out.println("program log length: " + longBuffer[0]);

        bb.rewind();
        ret = cl.clGetProgramBuildInfo(program, firstDeviceID, CL.CL_PROGRAM_BUILD_LOG, bb.capacity(), bb, null, 0);
        checkError("on clGetProgramBuildInfo3", ret);
        out.println("log:\n" + new String(bb.array(), 0, (int)longBuffer[0]));

        // Create the kernel
        Arrays.fill(intArray, 42);
        long kernel = cl.clCreateKernel(program, "VectorAdd", intArray, 0);
        checkError("on clCreateKernel", intArray[0]);


        ByteBuffer srcA = BufferFactory.newDirectByteBuffer(globalWorkSize*BufferFactory.SIZEOF_FLOAT);
        ByteBuffer srcB = BufferFactory.newDirectByteBuffer(globalWorkSize*BufferFactory.SIZEOF_FLOAT);
        ByteBuffer dst  = BufferFactory.newDirectByteBuffer(globalWorkSize*BufferFactory.SIZEOF_FLOAT);
        ByteBuffer elementCountBuffer = BufferFactory.newDirectByteBuffer(BufferFactory.SIZEOF_INT);
        elementCountBuffer.putInt(elementCount);

        srcA.limit(elementCount*BufferFactory.SIZEOF_FLOAT);
        srcB.limit(elementCount*BufferFactory.SIZEOF_FLOAT);

        fillBuffer(srcA, 23456);
        fillBuffer(srcB, 46987);

        // Set the Argument values
        ret = cl.clSetKernelArg(kernel, 0, BufferFactory.SIZEOF_LONG, wrap(devSrcA));  checkError("on clSetKernelArg0", ret);
        ret = cl.clSetKernelArg(kernel, 1, BufferFactory.SIZEOF_LONG, wrap(devSrcB));  checkError("on clSetKernelArg1", ret);
        ret = cl.clSetKernelArg(kernel, 2, BufferFactory.SIZEOF_LONG, wrap(devDst));   checkError("on clSetKernelArg2", ret);
        ret = cl.clSetKernelArg(kernel, 3, BufferFactory.SIZEOF_INT, elementCountBuffer);  checkError("on clSetKernelArg3", ret);

        // Asynchronous write of data to GPU device
        ret = cl.clEnqueueWriteBuffer(commandQueue, devSrcA, CL.CL_FALSE, 0, srcA.capacity(), srcA, 0, null, 0, null, 0);
        checkError("on clEnqueueWriteBuffer", ret);
        ret = cl.clEnqueueWriteBuffer(commandQueue, devSrcB, CL.CL_FALSE, 0, srcB.capacity(), srcB, 0, null, 0, null, 0);
        checkError("on clEnqueueWriteBuffer", ret);

        // Launch kernel
        ret = cl.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, 0,
                                                                 new long[]{ globalWorkSize }, 0,
                                                                 new long[]{ localWorkSize }, 0, 0,
                                                                 null, 0,
                                                                 null, 0);
        checkError("on clEnqueueNDRangeKernel", ret);

        // Synchronous/blocking read of results
        ret = cl.clEnqueueReadBuffer(commandQueue, devDst, CL.CL_TRUE, 0, BufferFactory.SIZEOF_FLOAT * globalWorkSize, dst, 0, null, 0, null, 0);
        checkError("on clEnqueueReadBuffer", ret);

//        for(int i = 0; i < 50; i++)
//            System.out.println(dst.getFloat());

        // cleanup
        ret = cl.clReleaseProgram(program);
        checkError("on clReleaseProgram", ret);

        ret = cl.clReleaseKernel(kernel);
        checkError("on clReleaseKernel", ret);

        ret = cl.clUnloadCompiler();
        checkError("on clUnloadCompiler", ret);

        ret = cl.clReleaseContext(context);
        checkError("on clReleaseContext", ret);

    }

    private void fillBuffer(ByteBuffer buffer, int seed) {

        Random rnd = new Random(seed);

        while(buffer.remaining() != 0)
            buffer.putFloat(rnd.nextFloat());

        buffer.rewind();
    }

    private ByteBuffer wrap(long value) {
        return (ByteBuffer)ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).putLong(value).rewind();
    }

    private String getBuildStatus(int status) {
        switch(status) {
            case CL.CL_BUILD_SUCCESS:
                return "CL_BUILD_SUCCESS";
            case CL.CL_BUILD_NONE:
                return "CL_BUILD_NONE";
            case CL.CL_BUILD_IN_PROGRESS:
                return "CL_BUILD_IN_PROGRESS";
            case CL.CL_BUILD_ERROR:
                return "CL_BUILD_ERROR";
// can't find this flag in spec...
//            case CL.CL_BUILD_PROGRAM_FAILURE:
//                return "CL_BUILD_PROGRAM_FAILURE";
            default:
                return "unknown status: " + status;
        }
    }

    @Test
    public void highLevelTest() {
        
        out.println(" - - - highLevelTest - - - ");

        CLPlatform[] clPlatforms = CLContext.listCLPlatforms();

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


        CLContext ctx = CLContext.create();
//        CLDevice device = ctx.getMaxFlopsDevice();
//        out.println("max FLOPS device: " + device);
        ctx.release();
    }


    


    private final int roundUp(int groupSize, int globalSize) {
        int r = globalSize % groupSize;
        if (r == 0) {
            return globalSize;
        } else {
            return globalSize + groupSize - r;
        }
    }

    private final void checkForError(int ret) {
        this.checkError("", ret);
    }

    private final void checkError(String msg, int ret) {
        if(ret != CL.CL_SUCCESS)
            throw new CLException(ret, msg);
    }


}