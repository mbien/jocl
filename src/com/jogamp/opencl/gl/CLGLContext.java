package com.jogamp.opencl.gl;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import java.nio.Buffer;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.opengl.impl.GLContextImpl;
import com.jogamp.opengl.impl.egl.EGLContext;
import com.jogamp.opengl.impl.macosx.cgl.MacOSXCGLContext;
import com.jogamp.opengl.impl.windows.wgl.WindowsWGLContext;
import com.jogamp.opengl.impl.x11.glx.X11GLXContext;
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
     * Note: This will make the GLContext current.
     * @see GLContext#makeCurrent()
     */
    public static CLGLContext create(GLContext glContext) {
        return create(glContext, (CLPlatform)null, CLDevice.Type.ALL);
    }

    /**
     * Creates a shared context on the specified platform on all available devices (CL_DEVICE_TYPE_ALL).
     * Note: This will make the GLContext current.
     * @see GLContext#makeCurrent()
     */
    public static CLGLContext create(GLContext glContext, CLPlatform platform) {
        return create(glContext, platform, CLDevice.Type.ALL);
    }

    /**
     * Creates a shared context on the specified platform and with the specified
     * device types.
     * Note: This will make the GLContext current.
     * @see GLContext#makeCurrent()
     */
    public static CLGLContext create(GLContext glContext, CLDevice.Type... deviceTypes) {
        return create(glContext, null, deviceTypes);
    }

    /**
     * Creates a shared context on the specified platform and with the specified
     * device types.
     * Note: This will make the GLContext current.
     * @see GLContext#makeCurrent()
     */
    public static CLGLContext create(GLContext glContext, CLPlatform platform, CLDevice.Type... deviceTypes) {

        if(platform == null) {
            platform = CLPlatform.getDefault();
        }

        long[] glID = new long[1];
        PointerBuffer properties = setupContextProperties(platform, glContext, glID);
        ErrorDispatcher dispatcher = createErrorHandler();
        long clID = createContextFromType(dispatcher, properties, toDeviceBitmap(deviceTypes));

        return new CLGLContext(platform, glContext, clID, glID[0], dispatcher);

    }

    /**
     * Creates a shared context on the specified platform and with the specified
     * devices.
     * Note: This will make the GLContext current.
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
        PointerBuffer properties = setupContextProperties(platform, glContext, glID);
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


    private static PointerBuffer setupContextProperties(CLPlatform platform, GLContext glContext, long[] glID) {

        if(platform == null) {
            throw new RuntimeException("no OpenCL installation found");
        }
        if(glContext == null) {
            throw new IllegalArgumentException("GLContext was null.");
        }

        // context must be current
//        glContext.makeCurrent();

        GLContextImpl ctxImpl = (GLContextImpl)glContext;
        glID[0] = glContext.getHandle();

        PointerBuffer properties;
        if(glContext instanceof X11GLXContext) {
//          spec: "When the GLX binding API is supported, the attribute
//          CL_GL_CONTEXT_KHR should be set to a GLXContext handle to an
//          OpenGL context, and the attribute CL_GLX_DISPLAY_KHR should be
//          set to the Display handle of the X Window System display used to
//          create the OpenGL context."
            properties = PointerBuffer.allocateDirect(7);
            long displayHandle = ctxImpl.getDrawableImpl().getNativeWindow().getDisplayHandle();
            properties.put(CL_GL_CONTEXT_KHR).put(glID[0])
                      .put(CL_GLX_DISPLAY_KHR).put(displayHandle)
                      .put(CL_CONTEXT_PLATFORM).put(platform.ID);
        }else if(glContext instanceof WindowsWGLContext) {
//          spec: "When the WGL binding API is supported, the attribute
//          CL_GL_CONTEXT_KHR should be set to an HGLRC handle to an OpenGL
//          context, and the attribute CL_WGL_HDC_KHR should be set to the
//          HDC handle of the display used to create the OpenGL context."
            properties = PointerBuffer.allocateDirect(7);
            long surfaceHandle = ctxImpl.getDrawableImpl().getNativeWindow().getSurfaceHandle();
            properties.put(CL_GL_CONTEXT_KHR).put(glID[0])
                      .put(CL_WGL_HDC_KHR).put(surfaceHandle)
                      .put(CL_CONTEXT_PLATFORM).put(platform.ID);
        }else if(glContext instanceof MacOSXCGLContext) {
//            TODO test on mac
//          spec: "When the CGL binding API is supported, the attribute
//          CL_CGL_SHAREGROUP_KHR should be set to a CGLShareGroup handle to
//          a CGL share group object."
            properties = PointerBuffer.allocateDirect(5);
            properties.put(CL_CGL_SHAREGROUP_KHR).put(glID[0])
                      .put(CL_CONTEXT_PLATFORM).put(platform.ID);
        }else if(glContext instanceof EGLContext) {
//            TODO test EGL
//          spec: "When the EGL binding API is supported, the attribute
//          CL_GL_CONTEXT_KHR should be set to an EGLContext handle to an
//          OpenGL ES or OpenGL context, and the attribute
//          CL_EGL_DISPLAY_KHR should be set to the EGLDisplay handle of the
//          display used to create the OpenGL ES or OpenGL context."
            properties = PointerBuffer.allocateDirect(7);
            long displayHandle = ctxImpl.getDrawableImpl().getNativeWindow().getDisplayHandle();
            properties.put(CL_GL_CONTEXT_KHR).put(glID[0])
                      .put(CL_EGL_DISPLAY_KHR).put(displayHandle)
                      .put(CL_CONTEXT_PLATFORM).put(platform.ID);
        }else{
            throw new RuntimeException("unsupported GLContext: "+glContext);
        }

        return (PointerBuffer)properties.put(0).rewind(); // 0 terminated array
    }

    // Buffers
    public final CLGLBuffer<?> createFromGLBuffer(int glBuffer, Mem... flags) {
        return createFromGLBuffer(null, glBuffer, Mem.flagsToInt(flags));
    }

    public final CLGLBuffer<?> createFromGLBuffer(int glBuffer, int flags) {
        return createFromGLBuffer(null, glBuffer, flags);
    }

    public final <B extends Buffer> CLGLBuffer<B> createFromGLBuffer(B directBuffer, int glBuffer, Mem... flags) {
        return createFromGLBuffer(directBuffer, glBuffer, Mem.flagsToInt(flags));
    }

    public final <B extends Buffer> CLGLBuffer<B> createFromGLBuffer(B directBuffer, int glBuffer, int flags) {
        CLGLBuffer<B> buffer = CLGLBuffer.create(this, directBuffer, flags, glBuffer);
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
