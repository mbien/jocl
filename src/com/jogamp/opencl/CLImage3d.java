/*
 * Copyright 2009 - 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

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
        IntBuffer err = Buffers.newDirectIntBuffer(1);
        B host_ptr = null;
        if(isHostPointerFlag(flags)) {
            host_ptr = directBuffer;
        }
        long id = cl.clCreateImage3D(context.ID, flags, format.getFormatImpl(), width, height, depth, rowPitch, slicePitch, host_ptr, err);
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
