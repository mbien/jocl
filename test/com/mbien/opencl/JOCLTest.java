package com.mbien.opencl;

import com.mbien.opencl.impl.CLImpl;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
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

    @Test
    public void basicLowLevelTest() {

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
        ret = cl.clGetPlatformIDs(0, null, 0, intBuffer, 0);
        assertEquals(CL.CL_SUCCESS, ret);
        System.out.println("#platforms: "+intBuffer[0]);

        long[] platformId = new long[intBuffer[0]];
        ret = cl.clGetPlatformIDs(platformId.length, platformId, 0, null, 0);
        assertEquals(CL.CL_SUCCESS, ret);
        
        long[] longBuffer = new long[1];

        ByteBuffer bb = ByteBuffer.allocate(128);
        byte[] str = new byte[128];

        for (int i = 0; i < platformId.length; i++)  {

            long platform = platformId[i];
            System.out.println("platform id: "+platform);

            ret = cl.clGetPlatformInfo(platform, CL.CL_PLATFORM_PROFILE, bb.capacity(), bb, null, 0);
            assertEquals(CL.CL_SUCCESS, ret);
            bb.get(str);

            System.out.println("    profile: "+new String(str));
            Arrays.fill(str, (byte)0);
            bb.rewind();

            ret = cl.clGetPlatformInfo(platform, CL.CL_PLATFORM_VERSION, bb.capacity(), bb, null, 0);
            assertEquals(CL.CL_SUCCESS, ret);
            bb.get(str);
            System.out.println("    version: "+new String(str));
            Arrays.fill(str, (byte)0);
            bb.rewind();

            ret = cl.clGetPlatformInfo(platform, CL.CL_PLATFORM_NAME, bb.capacity(), bb, null, 0);
            assertEquals(CL.CL_SUCCESS, ret);
            bb.get(str);
            System.out.println("    name: "+new String(str));
            Arrays.fill(str, (byte)0);
            bb.rewind();

            ret = cl.clGetPlatformInfo(platform, CL.CL_PLATFORM_VENDOR, bb.capacity(), bb, null, 0);
            assertEquals(CL.CL_SUCCESS, ret);
            bb.get(str);
            System.out.println("    vendor: "+new String(str));
            Arrays.fill(str, (byte)0);
            bb.rewind();

        }

        Arrays.fill(longBuffer, 0);


        long context = cl.clCreateContextFromType(null, CL.CL_DEVICE_TYPE_ALL, cb, null, null);
        System.out.println("context handle: "+context);

        ret = cl.clGetContextInfo(context, CL.CL_CONTEXT_DEVICES, 0, null, longBuffer, 0);
        assertEquals(CL.CL_SUCCESS, ret);

        System.out.println("CL_CONTEXT_DEVICES result: "+longBuffer[0]);
//        System.out.println("CL_CONTEXT_DEVICES result: "+buffer[1]);


    }


}