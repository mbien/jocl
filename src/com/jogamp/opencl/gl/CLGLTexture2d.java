package com.jogamp.opencl.gl;

import com.jogamp.opencl.CL;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.impl.CLImageFormatImpl;
import java.nio.Buffer;

import static com.jogamp.opencl.CL.*;

/**
 * 2D OpenCL image representing an 2D OpenGL texture.
 * @author Michael Bien
 */
public class CLGLTexture2d<B extends Buffer> extends CLGLImage2d<B> implements CLGLTexture {

    public final int target;
    
    public final int mipMapLevel;

    public CLGLTexture2d(CLContext context, B directBuffer, CLImageFormat format, CLImageInfoAccessor accessor, int target, int mipLevel, int width, int height, long id, int glid, int flags) {
        super(context, directBuffer, format, accessor, width, height, id, glid, flags);
        this.target = target;
        this.mipMapLevel = mipLevel;
    }

    static <B extends Buffer> CLGLTexture2d<B> createFromGLTexture2d(CLContext context, B directBuffer, int target, int texture, int mipLevel, int flags) {

        CLGLBuffer.checkBuffer(directBuffer, flags);

        CL cl = getCL(context);
        int[] result = new int[1];
        CLGLI clgli = (CLGLI)cl;

        long id = clgli.clCreateFromGLTexture2D(context.ID, flags, target, mipLevel, texture, result, 0);

        CLImageInfoAccessor accessor = new CLImageInfoAccessor(cl, id);

        CLImageFormat format = createUninitializedImageFormat();
        accessor.getInfo(CL_IMAGE_FORMAT, CLImageFormatImpl.size(), format.getFormatImpl().getBuffer(), null);

        int width = (int)accessor.getLong(CL_IMAGE_WIDTH);
        int height = (int)accessor.getLong(CL_IMAGE_HEIGHT);

        return new CLGLTexture2d<B>(context, directBuffer, format, accessor, target, mipLevel, width, height, id, texture, flags);

    }

    public int getTextureTarget() {
        return target;
    }

    public int getMipMapLevel() {
        return mipMapLevel;
    }

    @Override
    public GLObjectType getGLObjectType() {
        return GLObjectType.GL_OBJECT_TEXTURE2D;
    }


}
