/*
 * Created on Wednesday, May 25 2011 00:57
 */
package com.jogamp.opencl.impl;

import java.nio.IntBuffer;
import com.jogamp.common.nio.NativeSizeBuffer;
import com.jogamp.opencl.llb.CL;
import com.jogamp.opencl.spi.CLAccessorFactory;
import com.jogamp.opencl.spi.CLInfoAccessor;
import com.jogamp.opencl.spi.CLPlatformInfoAccessor;
import java.nio.Buffer;

import static com.jogamp.opencl.CLException.*;

/**
 *
 * @author Michael Bien
 */
public class CLTLAccessorFactory implements CLAccessorFactory {

    @Override
    public CLInfoAccessor createDeviceInfoAccessor(CL cl, long id) {
        return new CLDeviceInfoAccessor(cl, id);
    }

    @Override
    public CLPlatformInfoAccessor createPlatformInfoAccessor(CL cl, long id) {
        return new CLTLPlatformInfoAccessor(cl, id);
    }

    private final static class CLDeviceInfoAccessor extends CLTLInfoAccessor {

        private final CL cl;
        private final long ID;

        private CLDeviceInfoAccessor(CL cl, long id) {
            this.cl = cl;
            this.ID = id;
        }

        @Override
        public int getInfo(int name, long valueSize, Buffer value, NativeSizeBuffer valueSizeRet) {
            return cl.clGetDeviceInfo(ID, name, valueSize, value, valueSizeRet);
        }

    }

    private final static class CLTLPlatformInfoAccessor extends CLTLInfoAccessor implements CLPlatformInfoAccessor {

        private final long ID;
        private final CL cl;

        private CLTLPlatformInfoAccessor(CL cl, long id) {
            this.ID = id;
            this.cl = cl;
        }

        @Override
        public int getInfo(int name, long valueSize, Buffer value, NativeSizeBuffer valueSizeRet) {
            return cl.clGetPlatformInfo(ID, name, valueSize, value, valueSizeRet);
        }

        @Override
        public long[] getDeviceIDs(long type) {

            IntBuffer buffer = getBB(4).asIntBuffer();
            int ret = cl.clGetDeviceIDs(ID, type, 0, null, buffer);
            int count = buffer.get(0);

            // return an empty buffer rather than throwing an exception
            if(ret == CL.CL_DEVICE_NOT_FOUND || count == 0) {
                return new long[0];
            }else{
                checkForError(ret, "error while enumerating devices");

                NativeSizeBuffer deviceIDs = NativeSizeBuffer.wrap(getBB(count*NativeSizeBuffer.elementSize()));
                ret = cl.clGetDeviceIDs(ID, type, count, deviceIDs, null);
                checkForError(ret, "error while enumerating devices");

                long[] ids = new long[count];
                for (int i = 0; i < ids.length; i++) {
                    ids[i] = deviceIDs.get(i);
                }
                return ids;
            }

        }

    }

}
