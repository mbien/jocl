package com.jogamp.opencl;

import com.jogamp.common.nio.Buffers;
import java.nio.Buffer;
import java.nio.IntBuffer;

import static com.jogamp.opencl.CL.*;
import static com.jogamp.opencl.CLException.*;

/**
 *
 * @author Michael Bien
 */
public class CLImage3d<B extends Buffer> extends CLImage<B> {

    public final int depth;

    private CLImage3d(CLContext context, B directBuffer, CLImageFormat format, int width, int height, int depth, long id, int flags) {
        super(context, directBuffer, format, width, height, id, flags);
        this.depth = depth;
    }

    protected CLImage3d(CLContext context, B directBuffer, CLImageFormat format, CLImageInfoAccessor accessor, int width, int height, int depth, long id, int flags) {
        super(context, directBuffer, format, accessor, width, height, id, flags);
        this.depth = depth;
    }
    

    static <B extends Buffer> CLImage3d<B> createImage(CLContext context, B directBuffer,
            int width, int height, int depth, int rowPitch, int slicePitch, CLImageFormat format, int flags) {

        CL cl = context.cl;
        IntBuffer err = Buffers.newDirectByteBuffer(4).asIntBuffer();

        long id = cl.clCreateImage3D(context.ID, flags, format.getFormatImpl(), width, height, depth, rowPitch, slicePitch, directBuffer, err);
        checkForError(err.get(), "can not create 2d image");

        return new CLImage3d<B>(context, directBuffer, format, width, height, depth, id, flags);
    }

    @Override
    public <T extends Buffer> CLImage3d<T> cloneWith(T directBuffer) {
        return new CLImage3d<T>(context, directBuffer, format, width, height, depth, ID, FLAGS);
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

    @Override
    public String toString() {
        return "CLImage3d [id: " + ID+" width: "+width+" height: "+height+" depth: "+depth+"]";
    }
}
