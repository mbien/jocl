package com.mbien.opencl;

import com.mbien.opencl.impl.CLImpl;
import java.nio.ByteBuffer;
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
    public void basicTest() {
//        System.out.println(0xFFFFFFFF);
//        System.out.println(0xFFFFFFFE);
//        System.out.println(0xFFFFFFFD);

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

        CLImpl impl = new CLImpl();

        long context = impl.clCreateContextFromType(null, CL.CL_DEVICE_TYPE_ALL, cb, null, null);
        System.out.println("context handle: "+context);

        int[] buffer = new int[1];
        impl.clGetContextInfo(context, CL.CL_CONTEXT_NUM_DEVICES, 0, null, buffer, 0);
        System.out.println("CL_CONTEXT_NUM_DEVICES result: "+buffer[0]);


    }


}