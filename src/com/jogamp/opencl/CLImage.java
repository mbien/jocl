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

import com.jogamp.common.nio.NativeSizeBuffer;
import java.nio.Buffer;

import static com.jogamp.opencl.CL.*;

/**
 *
 * @author Michael Bien
 */
public abstract class CLImage<B extends Buffer> extends CLMemory<B>  {

    protected CLImageFormat format;

    final CLInfoAccessor imageInfo;

    public final int width;
    public final int height;

    protected CLImage(CLContext context, B directBuffer, CLImageFormat format, int width, int height, long id, int flags) {
        this(context, directBuffer, format, new CLImageInfoAccessor(context.cl, id), width, height, id, flags);
    }

    protected CLImage(CLContext context, B directBuffer, CLImageFormat format, CLImageInfoAccessor accessor, int width, int height, long id, int flags) {
        super(context, directBuffer, getSizeImpl(context.cl, id), id, flags);
        this.imageInfo = accessor;
        this.format = format;
        this.width = width;
        this.height = height;
    }

    protected static CLImageFormat createUninitializedImageFormat() {
        return new CLImageFormat();
    }

    /**
     * Returns the image format descriptor specified when image was created.
     */
    public CLImageFormat getFormat() {
        return format;
    }

    /**
     * Returns the size of each element of the image memory object given by image.
     * An element is made up of n channels. The value of n is given in {@link CLImageFormat} descriptor.
     */
    @Override
    public int getElementSize() {
        return (int)imageInfo.getLong(CL_IMAGE_ELEMENT_SIZE);
    }

    /**
     * Returns the size in bytes of a row of elements of the image object given by image.
     */
    public int getRowPitch() {
        return (int)imageInfo.getLong(CL_IMAGE_ROW_PITCH);
    }

    /**
     * Returns width of this image in pixels.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of this image in pixels.
     */
    public int getHeight() {
        return height;
    }


    protected final static class CLImageInfoAccessor extends CLInfoAccessor {

        private final long id;
        private final CL cl;

        public CLImageInfoAccessor(CL cl, long id) {
            this.cl = cl;
            this.id = id;
        }
        @Override
        public int getInfo(int name, long valueSize, Buffer value, NativeSizeBuffer valueSizeRet) {
            return cl.clGetImageInfo(id, name, valueSize, value, valueSizeRet);
        }
    }


}
