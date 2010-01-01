package com.mbien.opencl;

import com.sun.gluegen.runtime.CPU;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static java.lang.System.*;
import static com.mbien.opencl.TestUtils.*;
import static com.mbien.opencl.CLUtils.*;
import static com.sun.gluegen.runtime.BufferFactory.*;

/**
 * Test testing the low level bindings.
 * @author Michael Bien
 */
public class LowLevelBindingTest {

    private final static String programSource =
              " // OpenCL Kernel Function for element by element vector addition                                  \n"
            + "kernel void VectorAdd(global const int* a, global const int* b, global int* c, int iNumElements) { \n"
            + "    // get index into global data array                                                            \n"
            + "    int iGID = get_global_id(0);                                                                   \n"
            + "    // bound check (equivalent to the limit on a 'for' loop for standard/serial C code             \n"
            + "    if (iGID >= iNumElements)  {                                                                   \n"
            + "        return;                                                                                    \n"
            + "    }                                                                                              \n"
            + "    // add the vector elements                                                                     \n"
            + "    c[iGID] = a[iGID] + b[iGID];                                                                   \n"
            + "}                                                                                                  \n"
            + "kernel void Test(global const int* a, global const int* b, global int* c, int iNumElements) {      \n"
            + "    // get index into global data array                                                            \n"
            + "    int iGID = get_global_id(0);                                                                   \n"
            + "    // bound check (equivalent to the limit on a 'for' loop for standard/serial C code             \n"
            + "    if (iGID >= iNumElements)  {                                                                   \n"
            + "        return;                                                                                    \n"
            + "    }                                                                                              \n"
            + "    c[iGID] = iGID;                                                                                \n"
            + "}                                                                                                  \n";


    @BeforeClass
    public static void setUpClass() throws Exception {
        out.println("OS: " + System.getProperty("os.name"));
        out.println("VM: " + System.getProperty("java.vm.name"));
    }

    @Test
    public void contextlessTest() {

        out.println(" - - - lowLevelTest; contextless binding - - - ");

        int ret = CL.CL_SUCCESS;

        CL cl = CLPlatform.getLowLevelBinding();

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
            out.println("    profile: " + clString2JavaString(bb.array(), (int)longBuffer[0]));

            ret = cl.clGetPlatformInfo(platform, CL.CL_PLATFORM_VERSION, bb.capacity(), bb, longBuffer, 0);
            checkForError(ret);
            out.println("    version: " + clString2JavaString(bb.array(), (int)longBuffer[0]));

            ret = cl.clGetPlatformInfo(platform, CL.CL_PLATFORM_NAME, bb.capacity(), bb, longBuffer, 0);
            checkForError(ret);
            out.println("    name: " + clString2JavaString(bb.array(), (int)longBuffer[0]));

            ret = cl.clGetPlatformInfo(platform, CL.CL_PLATFORM_VENDOR, bb.capacity(), bb, longBuffer, 0);
            checkForError(ret);
            out.println("    vendor: " + clString2JavaString(bb.array(), (int)longBuffer[0]));

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
                out.println("    device: " + clString2JavaString(bb.array(), (int)longBuffer[0]));

                ret = cl.clGetDeviceInfo(device, CL.CL_DEVICE_TYPE, bb.capacity(), bb, longBuffer, 0);
                checkForError(ret);
                out.println("    type: " + CLDevice.Type.valueOf(bb.get()));
                bb.rewind();

            }

        }

    }

    @Test
    public void createContextTest() {

        out.println(" - - - createContextTest - - - ");

        CL cl = CLPlatform.getLowLevelBinding();

        int[] intArray = new int[1];
        // find all available OpenCL platforms
        int ret = cl.clGetPlatformIDs(0, null, 0, intArray, 0);
        checkForError(ret);
        out.println("#platforms: "+intArray[0]);

        long[] longArray = new long[intArray[0]];
        ret = cl.clGetPlatformIDs(longArray.length, longArray, 0, null, 0);
        checkForError(ret);

        long platform = longArray[0];

        //find all devices
        ret = cl.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_ALL, 0, null, 0, intArray, 0);
        checkForError(ret);
        out.println("#devices: "+intArray[0]);

        long[] devices = new long[intArray[0]];
        ret = cl.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_ALL, devices.length, devices, 0, null, 0);

        IntBuffer intBuffer = IntBuffer.allocate(1);
        long context = cl.clCreateContext(null, devices, null, null, intBuffer);
        checkError("on clCreateContext", intBuffer.get());

        //get number of devices
        ret = cl.clGetContextInfo(context, CL.CL_CONTEXT_DEVICES, 0, null, longArray, 0);
        checkError("on clGetContextInfo", ret);

        int sizeofLong = (CPU.is32Bit()?4:8);
        out.println("context created with " + longArray[0]/sizeofLong + " devices");

        //check if equal
        assertEquals("context was not created on all devices specified", devices.length, longArray[0]/sizeofLong);

        ret = cl.clReleaseContext(context);
        checkError("on clReleaseContext", ret);
    }


    @Test
    public void lowLevelVectorAddTest() {

        out.println(" - - - lowLevelTest2; VectorAdd kernel - - - ");

//        CreateContextCallback cb = new CreateContextCallback() {
//            @Override
//            public void createContextCallback(String errinfo, ByteBuffer private_info, long cb, Object user_data) {
//                throw new RuntimeException("not yet implemented...");
//            }
//        };

        long[] longArray = new long[1];
        ByteBuffer bb = ByteBuffer.allocate(4096).order(ByteOrder.nativeOrder());

        CL cl = CLPlatform.getLowLevelBinding();

        int ret = CL.CL_SUCCESS;
        int[] intArray = new int[1];

        long context = cl.clCreateContextFromType(null, CL.CL_DEVICE_TYPE_ALL, null, null, null);
        out.println("context handle: "+context);

        ret = cl.clGetContextInfo(context, CL.CL_CONTEXT_DEVICES, 0, null, longArray, 0);
        checkError("on clGetContextInfo", ret);

        int sizeofLong = (CPU.is32Bit()?4:8);
        out.println("context created with " + longArray[0]/sizeofLong + " devices");

        ret = cl.clGetContextInfo(context, CL.CL_CONTEXT_DEVICES, bb.capacity(), bb, null, 0);
        checkError("on clGetContextInfo", ret);

        for (int i = 0; i < longArray[0]/sizeofLong; i++) {
            out.println("device id: "+bb.getLong());
        }

        long firstDeviceID = bb.getLong(0);

        // Create a command-queue
        long commandQueue = cl.clCreateCommandQueue(context, firstDeviceID, 0, intArray, 0);
        checkError("on clCreateCommandQueue", intArray[0]);

        int elementCount = 11444777;	// Length of float arrays to process (odd # for illustration)
        int localWorkSize = 256;      // set and log Global and Local work size dimensions
        int globalWorkSize = roundUp(localWorkSize, elementCount);  // rounded up to the nearest multiple of the LocalWorkSize

        out.println("allocateing buffers of size: "+globalWorkSize);

        ByteBuffer srcA = newDirectByteBuffer(globalWorkSize*SIZEOF_INT);
        ByteBuffer srcB = newDirectByteBuffer(globalWorkSize*SIZEOF_INT);
        ByteBuffer dest = newDirectByteBuffer(globalWorkSize*SIZEOF_INT);

        // Allocate the OpenCL buffer memory objects for source and result on the device GMEM
        long devSrcA = cl.clCreateBuffer(context, CL.CL_MEM_READ_ONLY, srcA.capacity(), null, intArray, 0);
        checkError("on clCreateBuffer", intArray[0]);
        long devSrcB = cl.clCreateBuffer(context, CL.CL_MEM_READ_ONLY, srcB.capacity(), null, intArray, 0);
        checkError("on clCreateBuffer", intArray[0]);
        long devDst  = cl.clCreateBuffer(context, CL.CL_MEM_WRITE_ONLY, dest.capacity(), null, intArray, 0);
        checkError("on clCreateBuffer", intArray[0]);


        // Create the program
        long program = cl.clCreateProgramWithSource(context, 1, new String[] {programSource}, new long[]{programSource.length()}, 0, intArray, 0);
        checkError("on clCreateProgramWithSource", intArray[0]);

        // Build the program
        ret = cl.clBuildProgram(program, null, null, null, null);
        checkError("on clBuildProgram", ret);

        // Read program infos
        bb.rewind();
        ret = cl.clGetProgramInfo(program, CL.CL_PROGRAM_NUM_DEVICES, bb.capacity(), bb, null, 0);
        checkError("on clGetProgramInfo1", ret);
        out.println("program associated with "+bb.getInt(0)+" device(s)");

        ret = cl.clGetProgramInfo(program, CL.CL_PROGRAM_SOURCE, 0, null, longArray, 0);
        checkError("on clGetProgramInfo CL_PROGRAM_SOURCE", ret);
        out.println("program source length (cl): "+longArray[0]);
        out.println("program source length (java): "+programSource.length());

        bb.rewind();
        ret = cl.clGetProgramInfo(program, CL.CL_PROGRAM_SOURCE, bb.capacity(), bb, null, 0);
        checkError("on clGetProgramInfo CL_PROGRAM_SOURCE", ret);
        out.println("program source:\n" + clString2JavaString(bb.array(), (int)longArray[0]));

        // Check program status
        Arrays.fill(longArray, 42);
        bb.rewind();
        ret = cl.clGetProgramBuildInfo(program, firstDeviceID, CL.CL_PROGRAM_BUILD_STATUS, bb.capacity(), bb, null, 0);
        checkError("on clGetProgramBuildInfo1", ret);

        out.println("program build status: " + CLProgram.Status.valueOf(bb.getInt(0)));
        assertEquals("build status", CL.CL_BUILD_SUCCESS, bb.getInt(0));

        // Read build log
        ret = cl.clGetProgramBuildInfo(program, firstDeviceID, CL.CL_PROGRAM_BUILD_LOG, 0, null, longArray, 0);
        checkError("on clGetProgramBuildInfo2", ret);
        out.println("program log length: " + longArray[0]);

        bb.rewind();
        ret = cl.clGetProgramBuildInfo(program, firstDeviceID, CL.CL_PROGRAM_BUILD_LOG, bb.capacity(), bb, null, 0);
        checkError("on clGetProgramBuildInfo3", ret);
        out.println("log:\n" + clString2JavaString(bb.array(), (int)longArray[0]));

        // Create the kernel
        Arrays.fill(intArray, 42);
        long kernel = cl.clCreateKernel(program, "VectorAdd", intArray, 0);
        checkError("on clCreateKernel", intArray[0]);

//        srcA.limit(elementCount*SIZEOF_FLOAT);
//        srcB.limit(elementCount*SIZEOF_FLOAT);

        fillBuffer(srcA, 23456);
        fillBuffer(srcB, 46987);

        // Set the Argument values
        ret = cl.clSetKernelArg(kernel, 0, CPU.is32Bit()?SIZEOF_INT:SIZEOF_LONG, wrap(devSrcA));  checkError("on clSetKernelArg0", ret);
        ret = cl.clSetKernelArg(kernel, 1, CPU.is32Bit()?SIZEOF_INT:SIZEOF_LONG, wrap(devSrcB));  checkError("on clSetKernelArg1", ret);
        ret = cl.clSetKernelArg(kernel, 2, CPU.is32Bit()?SIZEOF_INT:SIZEOF_LONG, wrap(devDst));   checkError("on clSetKernelArg2", ret);
        ret = cl.clSetKernelArg(kernel, 3, SIZEOF_INT,  wrap(elementCount));  checkError("on clSetKernelArg3", ret);

        out.println("used device memory: "+ (srcA.capacity()+srcB.capacity()+dest.capacity())/1000000 +"MB");

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
        ret = cl.clEnqueueReadBuffer(commandQueue, devDst, CL.CL_TRUE, 0, dest.capacity(), dest, 0, null, 0, null, 0);
        checkError("on clEnqueueReadBuffer", ret);

        out.println("a+b=c result snapshot: ");
        for(int i = 0; i < 10; i++)
            out.print(dest.getInt()+", ");
        out.println("...; "+dest.remaining()/SIZEOF_INT + " more");


        // cleanup
        ret = cl.clReleaseCommandQueue(commandQueue);
        checkError("on clReleaseCommandQueue", ret);

        ret = cl.clReleaseMemObject(devSrcA);
        checkError("on clReleaseMemObject", ret);
        ret = cl.clReleaseMemObject(devSrcB);
        checkError("on clReleaseMemObject", ret);
        ret = cl.clReleaseMemObject(devDst);
        checkError("on clReleaseMemObject", ret);

        ret = cl.clReleaseProgram(program);
        checkError("on clReleaseProgram", ret);

        ret = cl.clReleaseKernel(kernel);
        checkError("on clReleaseKernel", ret);

        ret = cl.clUnloadCompiler();
        checkError("on clUnloadCompiler", ret);

        ret = cl.clReleaseContext(context);
        checkError("on clReleaseContext", ret);

    }

//    @Test
    public void loadTest() {
        //for memory leak detection; e.g watch out for "out of host memory" errors
        out.println(" - - - loadTest - - - ");
        for(int i = 0; i < 100; i++) {
            out.println("###iteration "+i);
            lowLevelVectorAddTest();
        }
    }

    private ByteBuffer wrap(long value) {
        return (ByteBuffer) newDirectByteBuffer(8).putLong(value).rewind();
    }

    private final void checkForError(int ret) {
        this.checkError("", ret);
    }

    private final void checkError(String msg, int ret) {
        if(ret != CL.CL_SUCCESS)
            throw new CLException(ret, msg);
    }


}