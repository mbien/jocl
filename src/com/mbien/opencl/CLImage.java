package com.mbien.opencl;

import com.jogamp.gluegen.runtime.PointerBuffer;
import java.nio.Buffer;

import static com.mbien.opencl.CL.*;

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
        super(context, directBuffer, id, flags);
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
        public int getInfo(int name, long valueSize, Buffer value, PointerBuffer valueSizeRet) {
            return cl.clGetImageInfo(id, name, valueSize, value, valueSizeRet);
        }
    }


}
