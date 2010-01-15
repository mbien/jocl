package com.mbien.opencl;

import com.sun.gluegen.runtime.PointerBuffer;
import java.nio.Buffer;

import static com.mbien.opencl.CLException.*;
import static com.mbien.opencl.CL.*;

/**
 * Object representing an OpenCL sampler.
 * @author Michael Bien
 */
public class CLSampler implements CLResource {

    public final long ID;

    private final CLContext context;
    private final CL cl;

    private final CLSamplerInfoAccessor samplerInfo;

    CLSampler(CLContext context, AddressingMode addrMode, FilteringMode filtMode, boolean normalizedCoords) {
        
        this.context = context;
        this.cl = context.cl;

        this.samplerInfo = new CLSamplerInfoAccessor();

        int[] error = new int[1];

        ID = cl.clCreateSampler(context.ID, normalizedCoords?CL_TRUE:CL_FALSE, addrMode.MODE, filtMode.MODE, error, 0);

        checkForError(error[0], "can not create sampler");
    }

    public FilteringMode getFilteringMode() {
        int info = (int)samplerInfo.getLong(CL_SAMPLER_FILTER_MODE);
        return FilteringMode.valueOf(info);
    }

    public AddressingMode getAddressingMode() {
        int info = (int)samplerInfo.getLong(CL_SAMPLER_ADDRESSING_MODE);
        return AddressingMode.valueOf(info);
    }

    public boolean hasNormalizedCoords() {
        return samplerInfo.getLong(CL_SAMPLER_NORMALIZED_COORDS) == CL_TRUE;
    }

    public void release() {
        int ret = cl.clReleaseSampler(ID);
        context.onSamplerReleased(this);
        checkForError(ret, "can not release sampler");
    }

    private class CLSamplerInfoAccessor extends CLInfoAccessor {

        @Override
        protected int getInfo(int name, long valueSize, Buffer value, PointerBuffer valueSizeRet) {
            return cl.clGetSamplerInfo(ID, name, valueSize, value, valueSizeRet);
        }

    }

    public enum FilteringMode {

        NEAREST(CL_FILTER_NEAREST),
        LINEAR(CL_FILTER_LINEAR);

        /**
         * Value of wrapped OpenCL sampler filtering mode type.
         */
        public final int MODE;

        private FilteringMode(int mode) {
            this.MODE = mode;
        }

        public static FilteringMode valueOf(int mode) {
            switch(mode) {
                case(CL_FILTER_NEAREST):
                    return NEAREST;
                case(CL_FILTER_LINEAR):
                    return LINEAR;
            }
            return null;
        }
    }

    public enum AddressingMode {

        REPEAT(CL_ADDRESS_REPEAT),
        CLAMP_TO_EDGE(CL_ADDRESS_CLAMP_TO_EDGE),
        CLAMP(CL_ADDRESS_CLAMP),
        NONE(CL_ADDRESS_NONE);

        /**
         * Value of wrapped OpenCL sampler addressing mode type.
         */
        public final int MODE;

        private AddressingMode(int mode) {
            this.MODE = mode;
        }

        public static AddressingMode valueOf(int mode) {
            switch(mode) {
                case(CL_ADDRESS_REPEAT):
                    return REPEAT;
                case(CL_ADDRESS_CLAMP_TO_EDGE):
                    return CLAMP_TO_EDGE;
                case(CL_ADDRESS_CLAMP):
                    return CLAMP;
                case(CL_ADDRESS_NONE):
                    return NONE;
            }
            return null;
        }
    }

}
