package com.mbien.opencl;

import com.sun.gluegen.runtime.BufferFactory;
import java.nio.Buffer;
import java.nio.IntBuffer;

import static com.mbien.opencl.CLException.*;

/**
 *
 * @author Michael Bien
 */
public final class CLImage2d<B extends Buffer> extends CLImage<B> {

    private CLImage2d(CLContext context, B directBuffer, CLImageFormat format, int width, int height, long id) {
        super(context, directBuffer, format, width, height, id);
    }

    static <B extends Buffer> CLImage2d<B> createImage(CLContext context, B directBuffer,
            int width, int height, int rowPitch, CLImageFormat format, int flags) {

        CL cl = context.cl;
        IntBuffer err = BufferFactory.newDirectByteBuffer(4).asIntBuffer();

        long id = cl.clCreateImage2D(context.ID, flags, format, width, height, rowPitch, directBuffer, err);
        checkForError(err.get(), "can not create 2d image");

        return new CLImage2d<B>(context, directBuffer, format, width, height, id);
    }

    @Override
    public <T extends Buffer> CLImage2d<T> cloneWith(T directBuffer) {
        return new CLImage2d<T>(context, directBuffer, format, width, height, ID);
    }

}
