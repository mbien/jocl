/*
 * Created on Tuesday, May 03 2011
 */
package com.jogamp.opencl.util.concurrent;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLPlatform;
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

    @Rule
    public MethodRule methodTimeout= new Timeout(10000);

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


}
