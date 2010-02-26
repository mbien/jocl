package com.mbien.opencl;

import com.mbien.opencl.CLMemory.GLObjectType;
import com.mbien.opencl.impl.CLImageFormatImpl;
import java.nio.Buffer;

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

    protected CLGLImage2d(CLContext context, B directBuffer, CLImageFormat format, CLImageInfoAccessor accessor, int width, int height, long id, int glid) {
        super(context, directBuffer, format, accessor, width, height, id);
        this.GLID = glid;
    }

    static <B extends Buffer> CLGLImage2d<B> createFromGLRenderbuffer(CLContext context, B directBuffer, int flags, int glObject) {

        CLGLBuffer.checkBuffer(directBuffer, flags);

        CL cl = context.cl;
        int[] result = new int[1];
        CLGLI clgli = (CLGLI)cl;

        long id = clgli.clCreateFromGLRenderbuffer(context.ID, flags, glObject, result, 0);

        return createImage(context, id, directBuffer, glObject);
    }

    static <B extends Buffer> CLGLImage2d<B> createImage(CLContext context, long id, B directBuffer, int glObject) {
        CLImageInfoAccessor accessor = new CLImageInfoAccessor(context.cl, id);

        CLImageFormat format = new CLImageFormat();
        accessor.getInfo(CL_IMAGE_FORMAT, CLImageFormatImpl.size(), format.getFormatImpl().getBuffer(), null);

        int width = (int)accessor.getLong(CL_IMAGE_WIDTH);
        int height = (int)accessor.getLong(CL_IMAGE_HEIGHT);

        return new CLGLImage2d<B>(context, directBuffer, format, accessor, width, height, id, glObject);
    }

    @Override
    public GLObjectType getGLObjectType() {
        return GLObjectType.GL_OBJECT_RENDERBUFFER;
    }

    @Override
    public int getGLObjectID() {
        return GLID;
    }

}
