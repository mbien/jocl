package com.mbien.opencl;

import com.mbien.opencl.impl.CLImpl;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Michael Bien
 */
public class JOCLTest {

    public JOCLTest() {
    }

    @Before
    public void setUpClass() throws Exception {
    }

    @After
    public void tearDownClass() throws Exception {
    }

//    @Test
    public void highLevelTest() {
        System.out.println(" - - - highLevelTest - - - ");

        CLPlatform[] clPlatforms = CLContext.listCLPlatforms();

        for (CLPlatform platform : clPlatforms) {

            System.out.println("platform info:");
            System.out.println("name: "+platform.getName());
            System.out.println("profile: "+platform.getProfile());
            System.out.println("version: "+platform.getVersion());
            System.out.println("vendor: "+platform.getVendor());

            CLDevice[] clDevices = platform.listCLDevices();
            for (CLDevice device : clDevices) {
                System.out.println("device info:");
                System.out.println("name: "+device.getName());
                System.out.println("profile: "+device.getProfile());
                System.out.println("vendor: "+device.getVendor());
                System.out.println("type: "+device.getType());
                System.out.println("global mem: "+device.getGlobalMemSize()/(1024*1024)+" MB");
                System.out.println("local mem: "+device.getLocalMemSize()/1024+" KB");
                System.out.println("clock: "+device.getMaxClockFrequency()+" MHz");
                System.out.println("max work group size: "+device.getMaxWorkGroupSize());
                System.out.println("max compute units: "+device.getMaxComputeUnits());
                System.out.println("extensions: "+device.getExtensions());
            }
        }


        CLContext ctx = CLContext.create();
        CLDevice device = ctx.getMaxFlopsDevice();
        System.out.println("max FLOPS device: " + device);
        ctx.release();
    }

    @Test
    public void lowLevelTest() {
        System.out.println(" - - - lowLevelTest - - - ");

        // already loaded
        System.out.print("loading native libs...");
        System.loadLibrary("gluegen-rt");
        System.loadLibrary("jocl");
        System.out.println("done");

        CreateContextCallback cb = new CreateContextCallback() {
            @Override
            public void createContextCallback(String errinfo, ByteBuffer private_info, long cb, Object user_data) {
                throw new RuntimeException(errinfo);
            }
        };

        System.out.println("creating OpenCL context");

        int ret = 0;

        CL cl = new CLImpl();

        int[] intBuffer = new int[1];
        // find all available OpenCL platforms
        ret = cl.clGetPlatformIDs(0, null, 0, intBuffer, 0);
        assertEquals(CL.CL_SUCCESS, ret);
        System.out.println("#platforms: "+intBuffer[0]);

        long[] platformId = new long[intBuffer[0]];
        ret = cl.clGetPlatformIDs(platformId.length, platformId, 0, null, 0);
        assertEquals(CL.CL_SUCCESS, ret);

        // print platform info
        long[] longBuffer = new long[1];
        ByteBuffer bb = ByteBuffer.allocate(128);
        bb.order(ByteOrder.nativeOrder());

        for (int i = 0; i < platformId.length; i++)  {

            long platform = platformId[i];
            System.out.println("platform id: "+platform);

            ret = cl.clGetPlatformInfo(platform, CL.CL_PLATFORM_PROFILE, bb.capacity(), bb, longBuffer, 0);
            assertEquals(CL.CL_SUCCESS, ret);
            System.out.println("    profile: "+new String(bb.array(), 0, (int)longBuffer[0]));

            ret = cl.clGetPlatformInfo(platform, CL.CL_PLATFORM_VERSION, bb.capacity(), bb, longBuffer, 0);
            assertEquals(CL.CL_SUCCESS, ret);
            System.out.println("    version: "+new String(bb.array(), 0, (int)longBuffer[0]));

            ret = cl.clGetPlatformInfo(platform, CL.CL_PLATFORM_NAME, bb.capacity(), bb, longBuffer, 0);
            assertEquals(CL.CL_SUCCESS, ret);
            System.out.println("    name: "+new String(bb.array(), 0, (int)longBuffer[0]));

            ret = cl.clGetPlatformInfo(platform, CL.CL_PLATFORM_VENDOR, bb.capacity(), bb, longBuffer, 0);
            assertEquals(CL.CL_SUCCESS, ret);
            System.out.println("    vendor: "+new String(bb.array(), 0, (int)longBuffer[0]));

            //find all devices
            ret = cl.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_ALL, 0, null, 0, intBuffer, 0);
            assertEquals(CL.CL_SUCCESS, ret);
            System.out.println("#devices: "+intBuffer[0]);

            long[] devices = new long[intBuffer[0]];
            ret = cl.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_ALL, devices.length, devices, 0, null, 0);

            //print device info
            for (int j = 0; j < devices.length; j++) {
                long device = devices[j];
                ret = cl.clGetDeviceInfo(device, CL.CL_DEVICE_NAME, bb.capacity(), bb, longBuffer, 0);
                assertEquals(CL.CL_SUCCESS, ret);
                System.out.println("    device: "+new String(bb.array(), 0, (int)longBuffer[0]));

                ret = cl.clGetDeviceInfo(device, CL.CL_DEVICE_TYPE, bb.capacity(), bb, longBuffer, 0);
                assertEquals(CL.CL_SUCCESS, ret);
                System.out.println("    type: " + CLDevice.Type.valueOf(bb.get()));
                bb.rewind();

            }

        }

        Arrays.fill(longBuffer, 0);

        long context = cl.clCreateContextFromType(null, 0, CL.CL_DEVICE_TYPE_ALL, cb, null, null, 0);
        System.out.println("context handle: "+context);

        ret = cl.clGetContextInfo(context, CL.CL_CONTEXT_DEVICES, 0, null, longBuffer, 0);
        assertEquals(CL.CL_SUCCESS, ret);

        System.out.println("CL_CONTEXT_DEVICES result: "+longBuffer[0]);

        cl.clReleaseContext(context);
    }


}