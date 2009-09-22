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
        System.out.println(0xFFFFFFFF);
        System.out.println(0xFFFFFFFE);
        System.out.println(0xFFFFFFFD);

        System.loadLibrary("gluegen-rt");
        System.loadLibrary("jocl");

        CreateContextCallback cb = new CreateContextCallback() {
            @Override
            public void createContextCallback(String errinfo, ByteBuffer private_info, long cb, Object user_data) {
                throw new RuntimeException(errinfo);
            }
        };

        CLImpl impl = new CLImpl();

        System.out.println("test call1: "+impl.clFinish(1));
        System.out.println("test call2: "+impl.clUnloadCompiler());

        long ctx = impl.clCreateContextFromType(null, CL.CL_DEVICE_TYPE_ALL, cb, null, null);

        System.out.println("context handle: "+ctx);

    }


}