package com.mbien.opencl.gl;

import com.mbien.opencl.CL;
import com.mbien.opencl.CLContext;
import com.mbien.opencl.CLImage3d;
import com.mbien.opencl.CLImageFormat;
import com.mbien.opencl.CLMemory.GLObjectType;
import com.mbien.opencl.impl.CLImageFormatImpl;
import java.nio.Buffer;

import static com.mbien.opencl.CL.*;

/**
 * 3D OpenCL image representing an 3D OpenGL texture.
 * @author Michael Bien
 */
public class CLGLTexture3d<B extends Buffer> extends CLImage3d<B> implements CLGLObject, CLGLTexture {

    /**
     * The OpenGL object handle.
     */
    public final int GLID;

    public final int target;
    
    public final int mipMapLevel;

    private CLGLTexture3d(CLContext context, B directBuffer, CLImageFormat format, CLImageInfoAccessor accessor, int target, int mipLevel, int width, int height, int depth, long id, int glid) {
        super(context, directBuffer, format, accessor, width, height, depth, id);
        this.GLID = glid;
        this.target = target;
        this.mipMapLevel = mipLevel;
    }

    static <B extends Buffer> CLGLTexture3d<B> createFromGLTexture3d(CLContext context, B directBuffer, int flags, int target, int mipLevel, int texture) {

        CLGLBuffer.checkBuffer(directBuffer, flags);

        CL cl = getCL(context);
        int[] result = new int[1];
        CLGLI clgli = (CLGLI)cl;

        long id = clgli.clCreateFromGLTexture3D(context.ID, flags, target, mipLevel, texture, result, 0);

        CLImageInfoAccessor accessor = new CLImageInfoAccessor(cl, id);

        CLImageFormat format = createUninitializedImageFormat();
        accessor.getInfo(CL_IMAGE_FORMAT, CLImageFormatImpl.size(), format.getFormatImpl().getBuffer(), null);

        int width = (int)accessor.getLong(CL_IMAGE_WIDTH);
        int height = (int)accessor.getLong(CL_IMAGE_HEIGHT);
        int depth = (int)accessor.getLong(CL_IMAGE_DEPTH);

        return new CLGLTexture3d<B>(context, directBuffer, format, accessor, target, mipLevel, width, height, depth, id, texture);
    }

    public int getGLObjectID() {
        return GLID;
    }

    public int getTextureTarget() {
        return target;
    }

    public int getMipMapLevel() {
        return mipMapLevel;
    }

    public GLObjectType getGLObjectType() {
        return GLObjectType.GL_OBJECT_TEXTURE3D;
    }

}
