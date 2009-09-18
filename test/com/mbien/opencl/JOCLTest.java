package com.mbien.opencl;

import com.mbien.opencl.impl.CLImpl;
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

        System.loadLibrary("jocl");

        CLImpl impl = new CLImpl();
        long ctx = impl.clCreateContextFromType(null, CL.CL_DEVICE_TYPE_ALL, null);

        System.out.println("context handle: "+ctx);

    }


}