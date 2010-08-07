package com.jogamp.opencl;

import com.jogamp.common.nio.Buffers;
import java.nio.Buffer;
import java.nio.IntBuffer;

import static com.jogamp.opencl.CLException.*;

/**
 *
 * @author Michael Bien
 */
public class CLImage2d<B extends Buffer> extends CLImage<B> {

    private CLImage2d(CLContext context, B directBuffer, CLImageFormat format, int width, int height, long id, int flags) {
        super(context, directBuffer, format, width, height, id, flags);
    }
    
    protected CLImage2d(CLContext context, B directBuffer, CLImageFormat format, CLImageInfoAccessor accessor, int width, int height, long id, int flags) {
        super(context, directBuffer, format, accessor, width, height, id, flags);
    }

    static <B extends Buffer> CLImage2d<B> createImage(CLContext context, B directBuffer,
            int width, int height, int rowPitch, CLImageFormat format, int flags) {

        CL cl = context.cl;
        IntBuffer err = Buffers.newDirectIntBuffer(1);
        B host_ptr = null;
        if(isHostPointerFlag(flags)) {
            host_ptr = directBuffer;
        }
        long id = cl.clCreateImage2D(context.ID, flags, format.getFormatImpl(), width, height, rowPitch, host_ptr, err);
        checkForError(err.get(), "can not create 2d image");

        return new CLImage2d<B>(context, directBuffer, format, width, height, id, flags);
    }

    @Override
    public <T extends Buffer> CLImage2d<T> cloneWith(T directBuffer) {
        return new CLImage2d<T>(context, directBuffer, format, width, height, ID, FLAGS);
    }


    @Override
    public String toString() {
        return "CLImage2d [id: " + ID+" width: "+width+" height: "+height+"]";
    }

}
