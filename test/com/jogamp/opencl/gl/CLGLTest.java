/*
 * Created on Saturday, April 24 2010 02:58 AM
 */

package com.jogamp.opencl.gl;

import com.jogamp.common.os.Platform;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.rules.Timeout;
import com.jogamp.opencl.CLDevice;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLPlatform;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLContext;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.System.*;

/**
 * Test testing the JOGL - JOCL interoperability.
 * @author Michael Bien
 */
public class CLGLTest {

    @Rule
    public MethodRule methodTimeout= new Timeout(5000);

    private static GLContext glcontext;
    private static GLWindow glWindow;

//    @BeforeClass
    public static void init() {
        GLProfile.initSingleton(true);

        // FIXME remove when JOCL is stabelized on mac
        if(Platform.getOS().toLowerCase().contains("mac")) {
            fail("quick exit to prevent deadlock");
        }

        Display display = NewtFactory.createDisplay(null); // local display
        assertNotNull(display);

        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        assertNotNull(screen);

        Window window = NewtFactory.createWindow(screen, new GLCapabilities(GLProfile.getDefault()));
        assertNotNull(window);

        window.setSize(640, 480);

        glWindow = GLWindow.create(window);
        
        assertNotNull(glWindow);
        glWindow.setVisible(true);

        glcontext = glWindow.getContext();
//        glcontext.makeCurrent();
        out.println("useing glcontext:");
        out.println(glcontext);
    }

//    @AfterClass
    public static void release() {
        if(glcontext!= null) {
            glcontext = null;
        }
    }

    @Test
    public void createContextTest() {

        init();

        out.println(" - - - glcl; createContextTest - - - ");

        CLDevice[] devices = CLPlatform.getDefault().listCLDevices();
        CLDevice device = null;
        for (CLDevice d : devices) {
            if(d.isGLMemorySharingSupported()) {
                device = d;
                break;
            }
        }

        if(device == null) {
            out.println("Aborting test: no GLCL capable devices found.");
            return;
        }else{
            out.println("isGLMemorySharingSupported==true on: \n    "+device);
        }

        assertNotNull(glcontext);
        out.println(device.getPlatform());
        CLContext context = CLGLContext.create(glcontext, device);
        assertNotNull(context);
        assertTrue(glcontext.isCurrent());

        try{
            out.println(context);
            /*
            CLDevice currentDevice = context.getCurrentGLCLDevice();
            assertNotNull(currentDevice);
            out.println(currentDevice);
             */
        }finally{
            context.release();
        }

        glcontext.destroy();


        release();
    }


}
