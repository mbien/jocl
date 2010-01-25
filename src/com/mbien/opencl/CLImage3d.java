package com.mbien.opencl;

import com.sun.gluegen.runtime.BufferFactory;
import java.nio.Buffer;
import java.nio.IntBuffer;

import static com.mbien.opencl.CL.*;
import static com.mbien.opencl.CLException.*;

/**
 *
 * @author Michael Bien
 */
public final class CLImage3d<B extends Buffer> extends CLImage<B> {

    public final int depth;

    private CLImage3d(CLContext context, B directBuffer, CLImageFormat format, int width, int height, int depth, long id) {
        super(context, directBuffer, format, width, height, id);
        this.depth = depth;
    }

    static <B extends Buffer> CLImage3d<B> createImage(CLContext context, B directBuffer,
            int width, int height, int depth, int rowPitch, int slicePitch, CLImageFormat format, int flags) {

        CL cl = context.cl;
        IntBuffer err = BufferFactory.newDirectByteBuffer(4).asIntBuffer();

        long id = cl.clCreateImage3D(context.ID, flags, format, width, height, depth, rowPitch, slicePitch, directBuffer, err);
        checkForError(err.get(), "can not create 2d image");

        return new CLImage3d<B>(context, directBuffer, format, width, height, depth, id);
    }

    @Override
    public <T extends Buffer> CLImage3d<T> cloneWith(T directBuffer) {
        return new CLImage3d<T>(context, directBuffer, format, width, height, depth, ID);
    }

    /**
     * Returns the size in bytes of a 2D slice of this 3D image.
     */
    public int getSlicePitch() {
        return (int)imageInfo.getLong(CL_IMAGE_SLICE_PITCH);
    }

    /**
     * Returns the depth of this image in pixels.
     */
    public int getDepth() {
        return depth;
    }
}
