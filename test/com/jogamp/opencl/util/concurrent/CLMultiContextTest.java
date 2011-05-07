/*
 * Created on Tuesday, May 03 2011
 */
package com.jogamp.opencl.util.concurrent;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.util.concurrent.CLQueueContextFactory.CLSimpleContextFactory;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.rules.Timeout;
import com.jogamp.opencl.util.CLMultiContext;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.System.*;

/**
 *
 * @author Michael Bien
 */
public class CLMultiContextTest {

//    @Rule
//    public MethodRule methodTimeout= new Timeout(10000);

    @Test
    public void createMultiContextTest() {

        CLMultiContext mc = CLMultiContext.create(CLPlatform.listCLPlatforms());

        try{
            List<CLContext> contexts = mc.getContexts();
            List<CLDevice> devices = mc.getDevices();

            assertFalse(contexts.isEmpty());
            assertFalse(devices.isEmpty());

            for (CLContext context : contexts) {
                out.println(context);
            }
            for (CLDevice device : devices) {
                out.println(device);
            }

        }finally{
            mc.release();
        }

    }

    private final static String programSource =
          " // OpenCL Kernel Function for element by element vector addition                                  \n"
        + "kernel void vectorAdd(global const int* a, global const int* b, global int* c, int iNumElements) { \n"
        + "    // get index in global data array                                                              \n"
        + "    int iGID = get_global_id(0);                                                                   \n"
        + "    // bound check (equivalent to the limit on a 'for' loop for standard/serial C code             \n"
        + "    if (iGID >= iNumElements)  {                                                                   \n"
        + "        return;                                                                                    \n"
        + "    }                                                                                              \n"
        + "    // add the vector elements                                                                     \n"
        + "    c[iGID] = a[iGID] + b[iGID];                                                                   \n"
        + "}                                                                                                  \n";

    @Test
    public void commandQueuePoolTest() {

        CLMultiContext mc = CLMultiContext.create(CLPlatform.listCLPlatforms());

        try {

            CLSimpleContextFactory factory = CLQueueContextFactory.createSimple(programSource);
            CLCommandQueuePool pool = CLCommandQueuePool.create(factory, mc);

            assertTrue(pool.getSize() > 0);

            pool.release();
        }finally{
            mc.release();
        }
    }

}
