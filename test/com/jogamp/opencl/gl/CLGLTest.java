/*
 * Created on Saturday, April 24 2010 02:58 AM
 */

package com.jogamp.opencl.gl;

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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import javax.media.opengl.GLContext;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static java.lang.System.*;

/**
 * Test testing the JOGL - JOCL interoperability.
 * @author Michael Bien
 */
public class CLGLTest {

    private static GLContext glcontext;

    @BeforeClass
    public static void init() {

        Display display = NewtFactory.createDisplay(null); // local display
        assertNotNull(display);

        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        assertNotNull(screen);

        Window window = NewtFactory.createWindow(screen, new GLCapabilities(GLProfile.getDefault()), false /* undecorated */);
        assertNotNull(window);

        window.setSize(640, 480);

        GLWindow glWindow = GLWindow.create(window);
        
        assumeNotNull(glWindow);
        glWindow.setVisible(true);

        glcontext = glWindow.getContext();
    }

    @AfterClass
    public static void release() {
        if(glcontext!= null) {
            glcontext.destroy();
            glcontext = null;
        }
    }

    @Test
    public void createContextTest() {

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

        assumeNotNull(glcontext);
        CLContext context = CLGLContext.create(glcontext, device);
        assertNotNull(context);
//        assertTrue(glcontext.isCurrent());

        try{
            out.println(context);
        }finally{
            context.release();
        }


    }


}
