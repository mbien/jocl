package com.mbien.opencl;

import com.sun.gluegen.runtime.PointerBuffer;
import java.nio.Buffer;

import static com.mbien.opencl.CL.*;

/**
 *
 * @author Michael Bien
 */
public abstract class CLImage<B extends Buffer> extends CLMemory<B>  {

    protected final CLImageFormat format;

    final CLInfoAccessor imageInfo;

    protected CLImage(CLContext context, B directBuffer, CLImageFormat format, long id) {
        super(context, directBuffer, id);
        this.imageInfo = new CLImageInfoAccessor();
        this.format = format;
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
        return (int)imageInfo.getLong(CL_IMAGE_WIDTH);
    }

    /**
     * Returns the height of this image in pixels.
     */
    public int getHeight() {
        return (int)imageInfo.getLong(CL_IMAGE_HEIGHT);
    }



    private final class CLImageInfoAccessor extends CLInfoAccessor {
        @Override
        protected int getInfo(int name, long valueSize, Buffer value, PointerBuffer valueSizeRet) {
            return cl.clGetImageInfo(ID, name, valueSize, value, valueSizeRet);
        }
    }


}
