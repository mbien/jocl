package com.mbien.opencl;

import java.nio.Buffer;
import com.mbien.opencl.CLMemory.Mem;
import com.sun.opengl.impl.GLContextImpl;
import com.sun.opengl.impl.macosx.cgl.MacOSXCGLContext;
import com.sun.opengl.impl.windows.wgl.WindowsWGLContext;
import com.sun.opengl.impl.x11.glx.X11GLXContext;
import java.nio.LongBuffer;
import javax.media.nativewindow.DefaultGraphicsConfiguration;
import javax.media.opengl.GLContext;

import static com.mbien.opencl.CLGLI.*;

/**
 *
 * @author Michael Bien
 */
public final class CLGLContext extends CLContext {

    final long glContextID;

    private CLGLContext(long clContextID, long glContextID) {
        super(clContextID);
        this.glContextID = glContextID;
    }

    public static CLGLContext create(GLContext glContext) {


//UNIX
//cl_context_properties props[] = {
//         CL_GL_CONTEXT_KHR, (cl_context_properties)glXGetCurrentContext(),
//         CL_GLX_DISPLAY_KHR,  (cl_context_properties) glXGetCurrentDisplay(),
//         CL_CONTEXT_PLATFORM, (cl_context_properties)cpPlatform, 0};

//WIN32
//cl_context_properties props[] = {
//         CL_GL_CONTEXT_KHR, (cl_context_properties)TODO0,
//         CL_WGL_HDC_KHR, (cl_context_properties)TODO 0,
//         CL_CONTEXT_PLATFORM, (cl_context_properties)cpPlatform, 0};

//MACOSX
//cl_context_properties props[] = {
//         CL_CGL_SHAREGROUP_KHR, (cl_context_properties)TODO 0,
//         CL_CONTEXT_PLATFORM, (cl_context_properties)cpPlatform, 0};

        long glID = 0;

        GLContextImpl ctxImpl = (GLContextImpl)glContext;

        DefaultGraphicsConfiguration config = (DefaultGraphicsConfiguration)ctxImpl.getDrawableImpl()
             .getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration();

        LongBuffer properties = LongBuffer.allocate(5);
        if(glContext instanceof X11GLXContext) {
            long handle = config.getScreen().getDevice().getHandle();
            glID = ((X11GLXContext)glContext).getContext();
            properties.put(CLGLI.CL_GL_CONTEXT_KHR).put(glID)
                      .put(CLGLI.CL_GLX_DISPLAY_KHR).put(handle);
        }else if(glContext instanceof WindowsWGLContext) {
            // TODO test on windows
            throw new RuntimeException("cl-gl interoperability on windows not yet implemented");
        }else if(glContext instanceof MacOSXCGLContext) {
            // TODO test on mac
            throw new RuntimeException("cl-gl interoperability on mac not yet implemented");
        }else{
            throw new RuntimeException("unsupported GLContext: "+glContext);
        }
        
        properties.put(0).rewind(); // 0 terminated array

        long clID = createContextFromType(properties, CL_DEVICE_TYPE_ALL);

        return new CLGLContext(clID, glID);
    }


    public final <B extends Buffer> CLBuffer<B> createFromGLBuffer(B directBuffer, int glBuffer, Mem... flags) {
        return createFromGLBuffer(directBuffer, glBuffer, Mem.flagsToInt(flags));
    }

    public final <B extends Buffer> CLBuffer<B> createFromGLBuffer(B directBuffer, int glBuffer, int flags) {
        CLBuffer<B> buffer = CLBuffer.create(this, directBuffer, flags, glBuffer);
        memoryObjects.add(buffer);
        return buffer;
    }

    // TODO move somewhere else
    public GLObjectType getGLObjectType(CLBuffer<?> buffer) {
        int[] array = new int[1];
        int ret = ((CLGLI)cl).clGetGLObjectInfo(buffer.ID, array, 0, null, 0);
        CLException.checkForError(ret, "error while asking for gl object info");
        return GLObjectType.valueOf(array[0]);
    }

    public int getGLObjectID(CLBuffer<?> buffer) {
        int[] array = new int[1];
        int ret = ((CLGLI)cl).clGetGLObjectInfo(buffer.ID, null, 0, array, 0);
        CLException.checkForError(ret, "error while asking for gl object info");
        return array[0];
    }

    public enum GLObjectType {

        GL_OBJECT_BUFFER(CL_GL_OBJECT_BUFFER),
        GL_OBJECT_TEXTURE2D(CL_GL_OBJECT_TEXTURE2D),
        GL_OBJECT_TEXTURE3D(CL_GL_OBJECT_TEXTURE3D),
        GL_OBJECT_RENDERBUFFER(CL_GL_OBJECT_RENDERBUFFER);

        public final int TYPE;

        private GLObjectType(int type) {
            this.TYPE = type;
        }

        public static GLObjectType valueOf(int type) {
            if(type == CL_GL_OBJECT_BUFFER)
                return GL_OBJECT_BUFFER;
            else if(type == CL_GL_OBJECT_TEXTURE2D)
                return GL_OBJECT_TEXTURE2D;
            else if(type == CL_GL_OBJECT_TEXTURE3D)
                return GL_OBJECT_TEXTURE3D;
            else if(type == CL_GL_OBJECT_RENDERBUFFER)
                return GL_OBJECT_RENDERBUFFER;
            return null;
        }
    }


}
