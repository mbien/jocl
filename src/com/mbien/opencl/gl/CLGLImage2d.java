package com.mbien.opencl.gl;

import com.mbien.opencl.CL;
import com.mbien.opencl.CLContext;
import com.mbien.opencl.CLImage2d;
import com.mbien.opencl.CLImageFormat;
import com.mbien.opencl.CLMemory.GLObjectType;
import com.mbien.opencl.impl.CLImageFormatImpl;
import java.nio.Buffer;
import javax.media.opengl.GLContext;

import static com.mbien.opencl.CL.*;

/**
 * 2D OpenCL image representing an OpenGL renderbuffer.
 * @author Michael Bien
 */
public class CLGLImage2d<B extends Buffer> extends CLImage2d<B> implements CLGLObject {

    /**
     * The OpenGL object handle.
     */
    public final int GLID;

    protected CLGLImage2d(CLContext context, B directBuffer, CLImageFormat format, CLImageInfoAccessor accessor, int width, int height, long id, int glid, int flags) {
        super(context, directBuffer, format, accessor, width, height, id, flags);
        this.GLID = glid;
    }

    static <B extends Buffer> CLGLImage2d<B> createFromGLRenderbuffer(CLContext context, B directBuffer, int flags, int glObject) {

        CLGLBuffer.checkBuffer(directBuffer, flags);

        CL cl = getCL(context);
        int[] result = new int[1];
        CLGLI clgli = (CLGLI)cl;

        long id = clgli.clCreateFromGLRenderbuffer(context.ID, flags, glObject, result, 0);

        return createImage(context, id, directBuffer, glObject, flags);
    }

    static <B extends Buffer> CLGLImage2d<B> createImage(CLContext context, long id, B directBuffer, int glObject, int flags) {
        CLImageInfoAccessor accessor = new CLImageInfoAccessor(getCL(context), id);

        CLImageFormat format = createUninitializedImageFormat();
        accessor.getInfo(CL_IMAGE_FORMAT, CLImageFormatImpl.size(), format.getFormatImpl().getBuffer(), null);

        int width = (int)accessor.getLong(CL_IMAGE_WIDTH);
        int height = (int)accessor.getLong(CL_IMAGE_HEIGHT);

        return new CLGLImage2d<B>(context, directBuffer, format, accessor, width, height, id, glObject, flags);
    }

    @Override
    public GLObjectType getGLObjectType() {
        return GLObjectType.GL_OBJECT_RENDERBUFFER;
    }

    @Override
    public int getGLObjectID() {
        return GLID;
    }

    @Override
    public CLGLContext getContext() {
        return (CLGLContext) super.getContext();
    }

    public GLContext getGLContext() {
        return getContext().getGLContext();
    }

}
