package com.mbien.opencl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
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
    public void lowLevelTest() {

        out.println(" - - - lowLevelTest - - - ");

        CreateContextCallback cb = new CreateContextCallback() {
            @Override
            public void createContextCallback(String errinfo, ByteBuffer private_info, long cb, Object user_data) {
                throw new RuntimeException(errinfo);
            }
        };

        out.println("creating OpenCL context");

        int ret = 0;

        CL cl = CLContext.getLowLevelBinding();

        int[] intBuffer = new int[1];
        // find all available OpenCL platforms
        ret = cl.clGetPlatformIDs(0, null, 0, intBuffer, 0);
        assertEquals(CL.CL_SUCCESS, ret);
        out.println("#platforms: "+intBuffer[0]);

        long[] platformId = new long[intBuffer[0]];
        ret = cl.clGetPlatformIDs(platformId.length, platformId, 0, null, 0);
        assertEquals(CL.CL_SUCCESS, ret);

        // print platform info
        long[] longBuffer = new long[1];
        ByteBuffer bb = ByteBuffer.allocate(128);
        bb.order(ByteOrder.nativeOrder());

        for (int i = 0; i < platformId.length; i++)  {

            long platform = platformId[i];
            out.println("platform id: "+platform);

            ret = cl.clGetPlatformInfo(platform, CL.CL_PLATFORM_PROFILE, bb.capacity(), bb, longBuffer, 0);
            assertEquals(CL.CL_SUCCESS, ret);
            out.println("    profile: "+new String(bb.array(), 0, (int)longBuffer[0]));

            ret = cl.clGetPlatformInfo(platform, CL.CL_PLATFORM_VERSION, bb.capacity(), bb, longBuffer, 0);
            assertEquals(CL.CL_SUCCESS, ret);
            out.println("    version: "+new String(bb.array(), 0, (int)longBuffer[0]));

            ret = cl.clGetPlatformInfo(platform, CL.CL_PLATFORM_NAME, bb.capacity(), bb, longBuffer, 0);
            assertEquals(CL.CL_SUCCESS, ret);
            out.println("    name: "+new String(bb.array(), 0, (int)longBuffer[0]));

            ret = cl.clGetPlatformInfo(platform, CL.CL_PLATFORM_VENDOR, bb.capacity(), bb, longBuffer, 0);
            assertEquals(CL.CL_SUCCESS, ret);
            out.println("    vendor: "+new String(bb.array(), 0, (int)longBuffer[0]));

            //find all devices
            ret = cl.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_ALL, 0, null, 0, intBuffer, 0);
            assertEquals(CL.CL_SUCCESS, ret);
            out.println("#devices: "+intBuffer[0]);

            long[] devices = new long[intBuffer[0]];
            ret = cl.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_ALL, devices.length, devices, 0, null, 0);

            //print device info
            for (int j = 0; j < devices.length; j++) {
                long device = devices[j];
                ret = cl.clGetDeviceInfo(device, CL.CL_DEVICE_NAME, bb.capacity(), bb, longBuffer, 0);
                assertEquals(CL.CL_SUCCESS, ret);
                out.println("    device: "+new String(bb.array(), 0, (int)longBuffer[0]));

                ret = cl.clGetDeviceInfo(device, CL.CL_DEVICE_TYPE, bb.capacity(), bb, longBuffer, 0);
                assertEquals(CL.CL_SUCCESS, ret);
                out.println("    type: " + CLDevice.Type.valueOf(bb.get()));
                bb.rewind();

            }

        }

        Arrays.fill(longBuffer, 0);

        long context = cl.clCreateContextFromType(null, 0, CL.CL_DEVICE_TYPE_ALL, cb, null, null, 0);
        out.println("context handle: "+context);

        ret = cl.clGetContextInfo(context, CL.CL_CONTEXT_DEVICES, 0, null, longBuffer, 0);
        assertEquals(CL.CL_SUCCESS, ret);

        out.println("CL_CONTEXT_DEVICES result: "+longBuffer[0]);

        ret = cl.clGetContextInfo(context, CL.CL_CONTEXT_NUM_DEVICES, 0, null, longBuffer, 0);
        assertEquals(CL.CL_SUCCESS, ret);

        out.println("CL_CONTEXT_NUM_DEVICES result: "+longBuffer[0]);

        cl.clReleaseContext(context);
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


}