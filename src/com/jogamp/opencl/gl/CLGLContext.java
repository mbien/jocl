/*
 * Copyright 2009 - 2010 JogAmp Community. All rights reserved.
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

package com.jogamp.opencl.gl;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import java.nio.Buffer;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.common.nio.NativeSizeBuffer;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.egl.EGLContext;
import jogamp.opengl.macosx.cgl.MacOSXCGLContext;
import jogamp.opengl.macosx.cgl.CGL;
import jogamp.opengl.windows.wgl.WindowsWGLContext;
import jogamp.opengl.x11.glx.X11GLXContext;
import javax.media.opengl.GLContext;

import static com.jogamp.opencl.gl.CLGLI.*;

/**
 * OpenCL Context supporting JOGL-JOCL interoperablity.
 * @author Michael Bien
 */
public final class CLGLContext extends CLContext {

    final long glID;
    private final GLContext glContext;

    private CLGLContext(CLPlatform platform, GLContext glContext, long clContextID, long glContextID, ErrorDispatcher dispatcher) {
        super(platform, clContextID, dispatcher);
        this.glID = glContextID;
        this.glContext = glContext;
    }

    /**
     * Creates a shared context on all available devices (CL_DEVICE_TYPE_ALL).
     * @see GLContext#makeCurrent()
     */
    public static CLGLContext create(GLContext glContext) {
        return create(glContext, (CLPlatform)null, CLDevice.Type.ALL);
    }

    /**
     * Creates a shared context on the specified platform on all available devices (CL_DEVICE_TYPE_ALL).
     * @see GLContext#makeCurrent()
     */
    public static CLGLContext create(GLContext glContext, CLPlatform platform) {
        return create(glContext, platform, CLDevice.Type.ALL);
    }

    /**
     * Creates a shared context on the specified platform and with the specified
     * device types.
     * @see GLContext#makeCurrent()
     */
    public static CLGLContext create(GLContext glContext, CLDevice.Type... deviceTypes) {
        return create(glContext, null, deviceTypes);
    }

    /**
     * Creates a shared context on the specified platform and with the specified
     * device types.
     * @see GLContext#makeCurrent()
     */
    public static CLGLContext create(GLContext glContext, CLPlatform platform, CLDevice.Type... deviceTypes) {

        if(platform == null) {
            platform = CLPlatform.getDefault();
        }

        long[] glID = new long[1];
        NativeSizeBuffer properties = setupContextProperties(platform, glContext, glID);
        ErrorDispatcher dispatcher = createErrorHandler();
        long clID = createContextFromType(dispatcher, properties, toDeviceBitmap(deviceTypes));

        return new CLGLContext(platform, glContext, clID, glID[0], dispatcher);

    }

    /**
     * Creates a shared context on the specified platform and with the specified
     * devices.
     * @see GLContext#makeCurrent()
     */
    public static CLGLContext create(GLContext glContext, CLDevice... devices) {

        if(devices == null) {
            throw new IllegalArgumentException("no devices specified");
        }else if(devices[0] == null) {
            throw new IllegalArgumentException("first device was null");
        }

        CLPlatform platform = devices[0].getPlatform();

        long[] glID = new long[1];
        NativeSizeBuffer properties = setupContextProperties(platform, glContext, glID);
        ErrorDispatcher dispatcher = createErrorHandler();
        long clID = createContext(dispatcher, properties, devices);

        CLGLContext context = new CLGLContext(platform, glContext, clID, glID[0], dispatcher);
        if(devices != null) {
            for (int i = 0; i < devices.length; i++) {
                context.overrideContext(devices[i]);
            }
        }
        return context;
    }


    private static NativeSizeBuffer setupContextProperties(CLPlatform platform, GLContext glContext, long[] glID) {

        if(platform == null) {
            throw new RuntimeException("no OpenCL installation found");
        }
        if(glContext == null) {
            throw new IllegalArgumentException("GLContext was null.");
        }

        // context must be current
        if(!glContext.isCurrent()) {
            throw new IllegalArgumentException("OpenGL context is not current,\n"+
                    " creating a OpenCL context for context sharing is not allowed in this situation.");
        }

        GLContextImpl ctxImpl = (GLContextImpl)glContext;
        glID[0] = glContext.getHandle();

        NativeSizeBuffer properties;
        if(glContext instanceof X11GLXContext) {
//          spec: "When the GLX binding API is supported, the attribute
//          CL_GL_CONTEXT_KHR should be set to a GLXContext handle to an
//          OpenGL context, and the attribute CL_GLX_DISPLAY_KHR should be
//          set to the Display handle of the X Window System display used to
//          create the OpenGL context."
            properties = NativeSizeBuffer.allocateDirect(7);
            long displayHandle = ctxImpl.getDrawableImpl().getNativeSurface().getDisplayHandle();
            properties.put(CL_GL_CONTEXT_KHR).put(glID[0])
                      .put(CL_GLX_DISPLAY_KHR).put(displayHandle)
                      .put(CL_CONTEXT_PLATFORM).put(platform.ID);
        }else if(glContext instanceof WindowsWGLContext) {
//          spec: "When the WGL binding API is supported, the attribute
//          CL_GL_CONTEXT_KHR should be set to an HGLRC handle to an OpenGL
//          context, and the attribute CL_WGL_HDC_KHR should be set to the
//          HDC handle of the display used to create the OpenGL context."
            properties = NativeSizeBuffer.allocateDirect(7);
            long surfaceHandle = ctxImpl.getDrawableImpl().getNativeSurface().getSurfaceHandle();
            properties.put(CL_GL_CONTEXT_KHR).put(glID[0])
                      .put(CL_WGL_HDC_KHR).put(surfaceHandle)
                      .put(CL_CONTEXT_PLATFORM).put(platform.ID);
        }else if(glContext instanceof MacOSXCGLContext) {
//          spec: "When the CGL binding API is supported, the attribute
//          CL_CGL_SHAREGROUP_KHR should be set to a CGLShareGroup handle to
//          a CGL share group object."
            long cgl = CGL.getCGLContext(glID[0]);
            long group = CGL.CGLGetShareGroup(cgl);
            properties = NativeSizeBuffer.allocateDirect(5);
            properties.put(CL_CGL_SHAREGROUP_KHR).put(group)
                      .put(CL_CONTEXT_PLATFORM).put(platform.ID);
        }else if(glContext instanceof EGLContext) {
//            TODO test EGL
//          spec: "When the EGL binding API is supported, the attribute
//          CL_GL_CONTEXT_KHR should be set to an EGLContext handle to an
//          OpenGL ES or OpenGL context, and the attribute
//          CL_EGL_DISPLAY_KHR should be set to the EGLDisplay handle of the
//          display used to create the OpenGL ES or OpenGL context."
            properties = NativeSizeBuffer.allocateDirect(7);
            long displayHandle = ctxImpl.getDrawableImpl().getNativeSurface().getDisplayHandle();
            properties.put(CL_GL_CONTEXT_KHR).put(glID[0])
                      .put(CL_EGL_DISPLAY_KHR).put(displayHandle)
                      .put(CL_CONTEXT_PLATFORM).put(platform.ID);
        }else{
            throw new RuntimeException("unsupported GLContext: "+glContext);
        }

        return properties.put(0).rewind(); // 0 terminated array
    }

    // Buffers
    /**
     * Creates a CLGLBuffer for memory sharing with the specified OpenGL buffer.
     * @param glBuffer The OpenGL buffer handle like a vertex buffer or pixel buffer object.
     * @param glBufferSize The size of the OpenGL buffer in bytes
     * @param flags optional flags.
     */
    public final CLGLBuffer<?> createFromGLBuffer(int glBuffer, long glBufferSize, Mem... flags) {
        return createFromGLBuffer(null, glBuffer, glBufferSize, Mem.flagsToInt(flags));
    }

    /**
     * Creates a CLGLBuffer for memory sharing with the specified OpenGL buffer.
     * @param glBuffer The OpenGL buffer handle like a vertex buffer or pixel buffer object.
     * @param glBufferSize The size of the OpenGL buffer in bytes
     * @param flags optional flags.
     */
    public final CLGLBuffer<?> createFromGLBuffer(int glBuffer, long glBufferSize, int flags) {
        return createFromGLBuffer(null, glBuffer, glBufferSize, flags);
    }

    /**
     * Creates a CLGLBuffer for memory sharing with the specified OpenGL buffer.
     * @param directBuffer A direct allocated NIO buffer for data transfers between java and OpenCL.
     * @param glBuffer The OpenGL buffer handle like a vertex buffer or pixel buffer object.
     * @param glBufferSize The size of the OpenGL buffer in bytes
     * @param flags optional flags.
     */
    public final <B extends Buffer> CLGLBuffer<B> createFromGLBuffer(B directBuffer, int glBuffer, long glBufferSize, Mem... flags) {
        return createFromGLBuffer(directBuffer, glBuffer, glBufferSize, Mem.flagsToInt(flags));
    }

    /**
     * Creates a CLGLBuffer for memory sharing with the specified OpenGL buffer.
     * @param directBuffer A direct allocated NIO buffer for data transfers between java and OpenCL.
     * @param glBuffer The OpenGL buffer handle like a vertex buffer or pixel buffer object.
     * @param glBufferSize The size of the OpenGL buffer in bytes
     * @param flags optional flags.
     */
    public final <B extends Buffer> CLGLBuffer<B> createFromGLBuffer(B directBuffer, int glBuffer, long glBufferSize, int flags) {
        CLGLBuffer<B> buffer = CLGLBuffer.create(this, directBuffer, glBufferSize, flags, glBuffer);
        memoryObjects.add(buffer);
        return buffer;
    }

    // Renderbuffers
    public final CLGLImage2d<?> createFromGLRenderbuffer(int glBuffer, Mem... flags) {
        return createFromGLRenderbuffer(null, glBuffer, Mem.flagsToInt(flags));
    }

    public final CLGLImage2d<?> createFromGLRenderbuffer(int glBuffer, int flags) {
        return createFromGLRenderbuffer(null, glBuffer, flags);
    }

    public final <B extends Buffer> CLGLImage2d<B> createFromGLRenderbuffer(B directBuffer, int glBuffer, Mem... flags) {
        return createFromGLRenderbuffer(directBuffer, glBuffer, Mem.flagsToInt(flags));
    }

    public final <B extends Buffer> CLGLImage2d<B> createFromGLRenderbuffer(B directBuffer, int glBuffer, int flags) {
        CLGLImage2d<B> buffer = CLGLImage2d.createFromGLRenderbuffer(this, directBuffer, flags, glBuffer);
        memoryObjects.add(buffer);
        return buffer;
    }

    //2d Textures
    public final CLGLTexture2d<?> createFromGLTexture2d(int target, int texture, int mipmap, Mem... flags) {
        return createFromGLTexture2d(null, target, texture, mipmap, Mem.flagsToInt(flags));
    }

    public final CLGLTexture2d<?> createFromGLTexture2d(int target, int texture, int mipmap, int flags) {
        return createFromGLTexture2d(null, target, texture, mipmap, flags);
    }

    public final <B extends Buffer> CLGLTexture2d<B> createFromGLTexture2d(B directBuffer, int target, int texture, int mipmap, Mem... flags) {
        return createFromGLTexture2d(directBuffer, target, texture, mipmap, Mem.flagsToInt(flags));
    }

    public final <B extends Buffer> CLGLTexture2d<B> createFromGLTexture2d(B directBuffer, int target, int texture, int mipmap, int flags) {
        CLGLTexture2d<B> buffer = CLGLTexture2d.createFromGLTexture2d(this, directBuffer, target, texture, mipmap, flags);
        memoryObjects.add(buffer);
        return buffer;
    }

    //3d Textures
    public final CLGLTexture3d<?> createFromGLTexture3d(int target, int texture, int mipmap, Mem... flags) {
        return createFromGLTexture3d(null, target, texture, mipmap, Mem.flagsToInt(flags));
    }

    public final CLGLTexture3d<?> createFromGLTexture3d(int target, int texture, int mipmap, int flags) {
        return createFromGLTexture3d(null, target, texture, mipmap, flags);
    }

    public final <B extends Buffer> CLGLTexture3d<B> createFromGLTexture3d(B directBuffer, int target, int texture, int mipmap, Mem... flags) {
        return createFromGLTexture3d(directBuffer, target, texture, mipmap, Mem.flagsToInt(flags));
    }

    public final <B extends Buffer> CLGLTexture3d<B> createFromGLTexture3d(B directBuffer, int target, int texture, int mipmap, int flags) {
        CLGLTexture3d<B> buffer = CLGLTexture3d.createFromGLTexture3d(this, directBuffer, target, texture, mipmap, flags);
        memoryObjects.add(buffer);
        return buffer;
    }

    /**
     * Return the low level OpenCL interface with OpenGL interoperability.
     */
    @Override
    public CLGLI getCL() {
        return (CLGLI)super.getCL();
    }

    /**
     * Returns the OpenGL context this context was shared with.
     */
    public GLContext getGLContext() {
        return glContext;
    }

    @Override
    public CLGLContext getContext() {
        return this;
    }

}
