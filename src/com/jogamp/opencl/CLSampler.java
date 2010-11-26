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

import com.jogamp.common.nio.PointerBuffer;

import java.nio.Buffer;

import static com.jogamp.opencl.CLException.*;
import static com.jogamp.opencl.CL.*;
import static com.jogamp.opencl.util.CLUtil.*;

/**
 * Object representing an OpenCL sampler.
 * @see CLContext#createSampler(com.jogamp.opencl.CLSampler.AddressingMode, com.jogamp.opencl.CLSampler.FilteringMode, boolean)
 * @author Michael Bien
 */
public class CLSampler extends CLObject implements CLResource {

    private final CLSamplerInfoAccessor samplerInfo;

    private CLSampler(CLContext context, long id,  AddressingMode addrMode, FilteringMode filtMode, boolean normalizedCoords) {
        super(context, id);
        this.samplerInfo = new CLSamplerInfoAccessor();
    }

    static CLSampler create(CLContext context, AddressingMode addrMode, FilteringMode filtMode, boolean normalizedCoords) {
        int[] error = new int[1];

        long id = context.cl.clCreateSampler(context.ID, clBoolean(normalizedCoords), addrMode.MODE, filtMode.MODE, error, 0);

        checkForError(error[0], "can not create sampler");
        return new CLSampler(context, id, addrMode, filtMode, normalizedCoords);
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
        if(ret != CL.CL_SUCCESS) {
            throw newException(ret, "can not release "+this);
        }
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
