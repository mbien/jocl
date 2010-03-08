package com.mbien.opencl.gl;

import com.mbien.opencl.CLContext;
import com.mbien.opencl.CLDevice;
import java.nio.Buffer;
import com.mbien.opencl.CLMemory.Mem;
import com.mbien.opencl.CLPlatform;
import com.sun.gluegen.runtime.PointerBuffer;
import com.sun.opengl.impl.GLContextImpl;
import com.sun.opengl.impl.macosx.cgl.MacOSXCGLContext;
import com.sun.opengl.impl.windows.wgl.WindowsWGLContext;
import com.sun.opengl.impl.x11.glx.X11GLXContext;
import javax.media.nativewindow.DefaultGraphicsConfiguration;
import javax.media.opengl.GLContext;

import static com.mbien.opencl.gl.CLGLI.*;

/**
 * OpenCL Context supporting JOGL-JOCL interoperablity.
 * @author Michael Bien
 */
public final class CLGLContext extends CLContext {

    final long glID;

    private CLGLContext(CLPlatform platform, long clContextID, long glContextID) {
        super(platform, clContextID);
        this.glID = glContextID;
    }

    /**
     * Creates a shared context on all available devices (CL_DEVICE_TYPE_ALL).
     */
    public static CLGLContext create(GLContext glContext) {
        return create(glContext, (CLPlatform)null, CLDevice.Type.ALL);
    }

    /**
     * Creates a shared context on the specified platform on all available devices (CL_DEVICE_TYPE_ALL).
     */
    public static CLGLContext create(GLContext glContext, CLPlatform platform) {
        return create(glContext, platform, CLDevice.Type.ALL);
    }

    /**
     * Creates a shared context on the specified platform and with the specified
     * device types.
     */
    public static CLGLContext create(GLContext glContext, CLDevice.Type... deviceTypes) {
        return create(glContext, null, deviceTypes);
    }

    /**
     * Creates a shared context on the specified devices.
     * The platform to be used is implementation dependent.
     */
    public static CLGLContext create(GLContext glContext, CLDevice... devices) {
        return create(glContext, null, devices);
    }

    /**
     * Creates a shared context on the specified platform and with the specified
     * device types.
     */
    public static CLGLContext create(GLContext glContext, CLPlatform platform, CLDevice.Type... deviceTypes) {

        if(platform == null) {
            platform = CLPlatform.getDefault();
        }

        long[] glID = new long[1];
        PointerBuffer properties = setupContextProperties(platform, glContext, glID);
        long clID = createContextFromType(properties, toDeviceBitmap(deviceTypes));

        return new CLGLContext(platform, clID, glID[0]);

    }

    /**
     * Creates a shared context on the specified platform and with the specified
     * devices.
     */
    public static CLGLContext create(GLContext glContext, CLPlatform platform, CLDevice... devices) {

        if(platform == null) {
            platform = CLPlatform.getDefault();
        }

        long[] glID = new long[1];
        PointerBuffer properties = setupContextProperties(platform, glContext, glID);
        long clID = createContext(properties, devices);

        CLGLContext context = new CLGLContext(platform, clID, glID[0]);
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

        GLContextImpl ctxImpl = (GLContextImpl)glContext;

        DefaultGraphicsConfiguration config = (DefaultGraphicsConfiguration)ctxImpl.getDrawableImpl()
             .getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration();

        PointerBuffer properties;
        if(glContext instanceof X11GLXContext) {
            properties = PointerBuffer.allocateDirect(7);
            long handle = config.getScreen().getDevice().getHandle();
            glID[0] = ((X11GLXContext)glContext).getContext();
            properties.put(CL_GL_CONTEXT_KHR).put(glID[0])
                      .put(CL_GLX_DISPLAY_KHR).put(handle)
                      .put(CL_CONTEXT_PLATFORM).put(platform.ID);
        }else if(glContext instanceof WindowsWGLContext) {
            // TODO test on windows
            //WIN32
            //cl_context_properties props[] = {
            //         CL_GL_CONTEXT_KHR, (cl_context_properties)0,
            //         CL_WGL_HDC_KHR, (cl_context_properties)0,
            //         CL_CONTEXT_PLATFORM, (cl_context_properties)cpPlatform, 0};
            properties = PointerBuffer.allocateDirect(7);
            long handle = config.getScreen().getDevice().getHandle();
            glID[0] = ((WindowsWGLContext)glContext).getHGLRC();
            properties.put(CL_GL_CONTEXT_KHR).put(glID[0])
                      .put(CL_WGL_HDC_KHR).put(handle)
                      .put(CL_CONTEXT_PLATFORM).put(platform.ID);
        }else if(glContext instanceof MacOSXCGLContext) {
            // TODO test on mac
            //MACOSX
            //cl_context_properties props[] = {
            //         CL_CGL_SHAREGROUP_KHR, (cl_context_properties)0,
            //         CL_CONTEXT_PLATFORM, (cl_context_properties)cpPlatform, 0};
            properties = PointerBuffer.allocateDirect(5);
            glID[0] = ((MacOSXCGLContext)glContext).getCGLContext();
            properties.put(CL_CGL_SHAREGROUP_KHR).put(glID[0])
                      .put(CL_CONTEXT_PLATFORM).put(platform.ID);
        }else{
            throw new RuntimeException("unsupported GLContext: "+glContext);
        }

        return properties.put(0).rewind(); // 0 terminated array
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


}
