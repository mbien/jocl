package com.jogamp.opencl;

import com.jogamp.opencl.impl.CLImageFormatImpl;

import static com.jogamp.opencl.CL.*;

/**
 * Represents the OpenCL image format with its channeltype and order.
 * @author Michael Bien
 * @see CLContext#getSupportedImage2dFormats(com.jogamp.opencl.CLMemory.Mem[])
 * @see CLContext#getSupportedImage3dFormats(com.jogamp.opencl.CLMemory.Mem[])
 */
public final class CLImageFormat {

    private final CLImageFormatImpl format;

    CLImageFormat() {
        format = CLImageFormatImpl.create();
    }

    CLImageFormat(CLImageFormatImpl format) {
        this.format = format;
    }

    public CLImageFormat(ChannelOrder order, ChannelType type) {
        format = CLImageFormatImpl.create();
        setImageChannelOrder(order);
        setImageChannelDataType(type);
    }

    public CLImageFormat setImageChannelOrder(ChannelOrder order) {
        format.setImageChannelOrder(order.ORDER);
        return this;
    }

    public CLImageFormat setImageChannelDataType(ChannelType type) {
        format.setImageChannelDataType(type.TYPE);
        return this;
    }

    public ChannelOrder getImageChannelOrder() {
        return ChannelOrder.valueOf(format.getImageChannelOrder());
    }

    public ChannelType getImageChannelDataType() {
        return ChannelType.valueOf(format.getImageChannelDataType());
    }

    /**
     * Returns the struct accessor for the cl_image_format struct.
     */
    public CLImageFormatImpl getFormatImpl() {
        return format;
    }

    @Override
    public String toString() {
        return "CLImageFormat["+getImageChannelOrder()+" "+getImageChannelDataType()+"]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CLImageFormat other = (CLImageFormat) obj;
        if (this.getImageChannelDataType() != other.getImageChannelDataType()) {
            return false;
        }
        if (this.getImageChannelOrder() != other.getImageChannelOrder()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 47 * hash + (this.getImageChannelDataType() != null ? this.getImageChannelDataType().hashCode() : 0);
        hash = 47 * hash + (this.getImageChannelOrder() != null ? this.getImageChannelOrder().hashCode() : 0);
        return hash;
    }

    /**
     * Specifies the number of channels and the channel layout i.e. the memory
     * layout in which channels are stored in the image.
     */
    public enum ChannelOrder {
                
        /**
         *
         */
        R(CL_R),

        /**
         *
         */
        A(CL_A),

        /**
         *
         */
        RG(CL_RG),

        /**
         *
         */
        RA(CL_RA),

        /**
         * This format can only be used if channel data type is one of the following values:
         * {@link ChannelType#UNORM_SHORT_565}, {@link ChannelType#UNORM_SHORT_555}
         * or {@link ChannelType#UNORM_INT_101010}.
         */
        RGB(CL_RGB),

        /**
         *
         */
        RGBA(CL_RGBA),

        /**
         * This format can only be used if channel data type is one of the following values:
         * {@link ChannelType#UNORM_INT8}, {@link ChannelType#SNORM_INT8}, {@link ChannelType#SIGNED_INT8}
         * or {@link ChannelType#UNSIGNED_INT8}.
         */
        ARGB(CL_ARGB),

        /**
         * @see #ARGB
         */
        BGRA(CL_BGRA),

        /**
         * This format can only be used if channel data type is one of the following values:
         * {@link ChannelType#UNORM_INT8}, {@link ChannelType#UNORM_INT16}, {@link ChannelType#SNORM_INT8},
         * {@link ChannelType#SNORM_INT16}, {@link ChannelType#HALF_FLOAT}, or {@link ChannelType#FLOAT}.
         */
        INTENSITY(CL_INTENSITY),

        /**
         * This format can only be used if channel data type is one of the following values:
         * {@link ChannelType#UNORM_INT8}, {@link ChannelType#UNORM_INT16}, {@link ChannelType#SNORM_INT8},
         * {@link ChannelType#SNORM_INT16}, {@link ChannelType#HALF_FLOAT}, or {@link ChannelType#FLOAT}.
         */
        LUMINANCE(CL_LUMINANCE);
        
        
        /**
         * Value of wrapped OpenCL flag.
         */
        public final int ORDER;

        private ChannelOrder(int order) {
            this.ORDER = order;
        }

        public static ChannelOrder valueOf(int orderFlag) {
            switch (orderFlag) {
                case CL_R:
                    return R;
                case CL_A:
                    return A;
                case CL_INTENSITY:
                    return INTENSITY;
                case CL_LUMINANCE:
                    return LUMINANCE;
                case CL_RG:
                    return RG;
                case CL_RA:
                    return RA;
                case CL_RGB:
                    return RGB;
                case CL_RGBA:
                    return RGBA;
                case CL_ARGB:
                    return ARGB;
                case CL_BGRA:
                    return BGRA;
            }
            return null;
        }

    }


    /**
     * Describes the size of the channel data type.
     */
    public enum ChannelType {

        /**
         * Each channel component is a normalized signed 8-bit integer value.
         */
        SNORM_INT8(CL_SNORM_INT8),
        
        /**
         * Each channel component is a normalized signed 16-bit integer value.
         */
        SNORM_INT16(CL_SNORM_INT16),

        /**
         * Each channel component is a normalized unsigned 8-bit integer value.
         */
        UNORM_INT8(CL_UNORM_INT8),

        /**
         * Each channel component is a normalized unsigned 16-bit integer value.
         */
        UNORM_INT16(CL_UNORM_INT16),

        /**
         * Represents a normalized 5-6-5 3-channel RGB image. The channel order must
         * be {@link ChannelOrder#RGB}.
         */
        UNORM_SHORT_565(CL_UNORM_SHORT_565),

        /**
         * Represents a normalized x-5-5-5 4-channel xRGB image. The channel order must
         * be {@link ChannelOrder#RGB}.
         */
        UNORM_SHORT_555(CL_UNORM_SHORT_555),

        /**
         * Represents a normalized x-10-10-10 4-channel xRGB image. The channel order
         * must be {@link ChannelOrder#RGB}.
         */
        UNORM_INT_101010(CL_UNORM_INT_101010),

        /**
         * Each channel component is an unnormalized signed 8-bit integer value.
         */
        SIGNED_INT8(CL_SIGNED_INT8),

        /**
         * Each channel component is an unnormalized signed 16-bit integer value.
         */
        SIGNED_INT16(CL_SIGNED_INT16),

        /**
         * Each channel component is an unnormalized signed 32-bit integer value.
         */
        SIGNED_INT32(CL_SIGNED_INT32),

        /**
         * Each channel component is an unnormalized unsigned 8-bit integer value.
         */
        UNSIGNED_INT8(CL_UNSIGNED_INT8),

        /**
         * Each channel component is an unnormalized unsigned 16-bit integer value.
         */
        UNSIGNED_INT16(CL_UNSIGNED_INT16),

        /**
         * Each channel component is an unnormalized unsigned 32-bit integer value.
         */
        UNSIGNED_INT32(CL_UNSIGNED_INT32),

        /**
         * Each channel component is a 16-bit half-float value.
         */
        HALF_FLOAT(CL_HALF_FLOAT),

        /**
         * Each channel component is a single precision floating-point value.
         */
        FLOAT(CL_FLOAT);

        /**
         * Value of wrapped OpenCL flag.
         */
        public final int TYPE;

        private ChannelType(int channel) {
            this.TYPE = channel;
        }

        public static ChannelType valueOf(int channelFlag) {
            switch (channelFlag) {
                case CL_SNORM_INT8:
                    return SNORM_INT8;
                case CL_SNORM_INT16:
                    return SNORM_INT16;
                case CL_UNORM_INT8:
                    return UNORM_INT8;
                case CL_UNORM_INT16:
                    return UNORM_INT16;
                case CL_UNORM_SHORT_565:
                    return UNORM_SHORT_565;
                case CL_UNORM_SHORT_555:
                    return UNORM_SHORT_555;
                case CL_UNORM_INT_101010:
                    return UNORM_INT_101010;
                case CL_SIGNED_INT8:
                    return SIGNED_INT8;
                case CL_SIGNED_INT16:
                    return SIGNED_INT16;
                case CL_SIGNED_INT32:
                    return SIGNED_INT32;
                case CL_UNSIGNED_INT8:
                    return UNSIGNED_INT8;
                case CL_UNSIGNED_INT16:
                    return UNSIGNED_INT16;
                case CL_UNSIGNED_INT32:
                    return UNSIGNED_INT32;
                case CL_HALF_FLOAT:
                    return HALF_FLOAT;
                case CL_FLOAT:
                    return FLOAT;
            }
            return null;
        }

    }
}
