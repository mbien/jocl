/*
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

/*
 * Created on Saturday, April 24 2010 02:58 AM
 */

package com.jogamp.opencl.gl;

import com.jogamp.opencl.CLImageFormat.ChannelType;
import com.jogamp.opencl.CLImageFormat.ChannelOrder;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLImageFormat;
import java.nio.ByteBuffer;
import javax.media.opengl.GL2;
import javax.media.opengl.GLException;
import com.jogamp.opencl.CLDevice;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.TestUtils;
import com.jogamp.opencl.util.CLDeviceFilters;
import com.jogamp.opencl.util.CLPlatformFilters;
import java.nio.IntBuffer;
import javax.media.opengl.DebugGL2;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.Timeout;

import static com.jogamp.opencl.util.CLPlatformFilters.*;
import static org.junit.Assert.*;
import static java.lang.System.*;

/**
 * Test testing the JOGL - JOCL interoperability.
 * @author Michael Bien
 */
public class CLGLTest {

    @Rule
    public MethodRule methodTimeout= new Timeout(15000);

    private static GLContext glcontext;
    private static GLWindow glWindow;
    private static Window window;

    public static void initGL() {
        GLProfile.initSingleton(true);

        Display display = NewtFactory.createDisplay(null); // local display
        assertNotNull(display);

        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        assertNotNull(screen);

        window = NewtFactory.createWindow(screen, new GLCapabilities(GLProfile.getDefault()));
        assertNotNull(window);

        window.setSize(640, 480);

        glWindow = GLWindow.create(window);

        assertNotNull(glWindow);
        glWindow.setVisible(true);

        glcontext = glWindow.getContext();
        assertNotNull(glcontext);
//        out.println(" - - - - glcontext - - - - ");
//        out.println(glcontext);
//        out.println(" - - - - - - - - - - - - - ");
    }

    private void deinitGL() throws GLException {
        glcontext.release();
        glWindow.destroy();
        window.destroy();
        
        glcontext = null;
        glWindow = null;
        window = null;
    }

    @Test
    public void createContextTest() {

        initGL();

        out.println(" - - - glcl; createContextTest - - - ");

        CLPlatform platform = CLPlatform.getDefault(CLPlatformFilters.glSharing());
        CLDevice device = platform.getMaxFlopsDevice(CLDeviceFilters.glSharing());

        if(device == null) {
            out.println("Aborting test: no GLCL capable devices found.");
            return;
        }else{
            out.println("isGLMemorySharingSupported==true on: \n    "+device);
        }

        out.println(device.getPlatform());
        
        assertNotNull(glcontext);
        makeGLCurrent();
        assertTrue(glcontext.isCurrent());
        
        CLContext context = CLGLContext.create(glcontext, device);
        assertNotNull(context);

        try{
            out.println(context);
            /*
            CLDevice currentDevice = context.getCurrentGLCLDevice();
            assertNotNull(currentDevice);
            out.println(currentDevice);
             */
        }finally{
            // destroy cl context, gl context still current
            context.release();

            deinitGL();
        }

    }
    
    @Test
    public void vboSharing() {
        
        out.println(" - - - glcl; vboSharing - - - ");
        
        initGL();
        makeGLCurrent();
        assertTrue(glcontext.isCurrent());
        
        CLPlatform platform = CLPlatform.getDefault(glSharing(glcontext));
        if(platform == null) {
            out.println("test aborted");
            return;
        }
        
        CLDevice theChosenOne = platform.getMaxFlopsDevice(CLDeviceFilters.glSharing());
        out.println(theChosenOne);
        
        CLGLContext context = CLGLContext.create(glcontext, theChosenOne);
        
        try{
            out.println(context);
            
            GL2 gl = getGL();
            
            int[] id = new int[1];
            gl.glGenBuffers(id.length, id, 0);
            
            IntBuffer glData = Buffers.newDirectIntBuffer(new int[] {0,1,2,3,4,5,6,7,8});
            glData.rewind();

            // create and write GL buffer
            gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
                gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, id[0]);
                gl.glBufferData(GL2.GL_ARRAY_BUFFER, glData.capacity()*4, glData, GL2.GL_STATIC_DRAW);
                gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
            gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
            gl.glFinish();
            

            // create CLGL buffer
            IntBuffer clData = Buffers.newDirectIntBuffer(9);
            CLGLBuffer<IntBuffer> clBuffer = context.createFromGLBuffer(clData, id[0], glData.capacity()*4, Mem.READ_ONLY);
            
            assertEquals(glData.capacity(), clBuffer.getCLCapacity());
            assertEquals(glData.capacity()*4, clBuffer.getCLSize());
                
            
            CLCommandQueue queue = theChosenOne.createCommandQueue();

            // read gl buffer into cl nio buffer
            queue.putAcquireGLObject(clBuffer)
                 .putReadBuffer(clBuffer, true)
                 .putReleaseGLObject(clBuffer);

            while(clData.hasRemaining()) {
                assertEquals(glData.get(), clData.get());
            }

            out.println(clBuffer);

            clBuffer.release();
            assertTrue(context.getMemoryObjects().isEmpty());
            queue.finish();
            
            gl.glDeleteBuffers(1, id, 0);
            gl.glFinish();
            
        }finally{
//            context.release();
            deinitGL();
        }
        
    }

    @Test
    public void texture2dSharing() {

        out.println(" - - - glcl; textureSharing - - - ");

        initGL();
        makeGLCurrent();
        assertTrue(glcontext.isCurrent());

        CLPlatform platform = CLPlatform.getDefault(glSharing(glcontext));
        if(platform == null) {
            out.println("test aborted");
            return;
        }

        CLDevice theChosenOne = platform.getMaxFlopsDevice(CLDeviceFilters.glSharing());
        out.println(theChosenOne);

        CLGLContext context = CLGLContext.create(glcontext, theChosenOne);

        try{
            out.println(context);

            int size = 64;
            ByteBuffer reference = Buffers.newDirectByteBuffer(size*size*4);
            TestUtils.fillBuffer(reference, 42);

            GL2 gl = getGL();

            int[] texID = new int[1];
            gl.glGenTextures(1, texID, 0);
            gl.glBindTexture(GL2.GL_TEXTURE_2D, texID[0]);

                gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 1);
                gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
                gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
                gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
                gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
                gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_DECAL);

                gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA8, size, size, 0, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, reference);
            gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
            gl.glFinish();

            CLGLTexture2d<ByteBuffer> texture = context.createFromGLTexture2d(Buffers.newDirectByteBuffer(size*size*4), GL2.GL_TEXTURE_2D, texID[0], 0);
            out.println(texture);
            assertEquals(4, texture.getElementSize());
            assertEquals(size*4, texture.getRowPitch());
            assertEquals(size, texture.getWidth());
            assertEquals(size, texture.getHeight());
            assertEquals(GL2.GL_TEXTURE_2D, texture.getTextureTarget());

            CLImageFormat format = texture.getFormat();
            assertNotNull(format);
            out.println(format);
            assertEquals(ChannelOrder.RGBA, format.getImageChannelOrder());
            assertEquals(ChannelType.UNORM_INT8, format.getImageChannelDataType());

            CLCommandQueue queue = theChosenOne.createCommandQueue();
            queue.putAcquireGLObject(texture);
            queue.putReadImage(texture, true);
            queue.putReleaseGLObject(texture);

            ByteBuffer buffer = texture.getBuffer();
            while(reference.hasRemaining()) {
                assertEquals(reference.get(), buffer.get());
            }

            texture.release();
            assertTrue(texture.isReleased());
            assertTrue(context.getMemoryObjects().isEmpty());

            gl.glDeleteTextures(1, texID, 0);
            gl.glFinish();
        }finally{
//            context.release();
            deinitGL();
        }

    }

    private GL2 getGL() {
        return new DebugGL2(glcontext.getGL().getGL2());
    }

    private static void makeGLCurrent() {
        // we are patient...
        while(true) {
            try{
                glcontext.makeCurrent();
                break;
            }catch(RuntimeException ex) {
                try {
                    Thread.sleep(200);
                    // I don't give up yet!
                } catch (InterruptedException ignore) { }
            }
        }
    }


}
