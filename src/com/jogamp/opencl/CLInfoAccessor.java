package com.jogamp.opencl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.opencl.util.CLUtil;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import static com.jogamp.opencl.CLException.*;

/**
 * Internal utility for common OpenCL clGetFooInfo calls.
 * Threadsafe.
 * @author Michael Bien
 */
abstract class CLInfoAccessor {

    protected final static ThreadLocal<ByteBuffer> localBB = new ThreadLocal<ByteBuffer>() {

        @Override
        protected ByteBuffer initialValue() {
            return Buffers.newDirectByteBuffer(512);
        }

    };
    protected final static ThreadLocal<PointerBuffer> localPB = new ThreadLocal<PointerBuffer>() {

        @Override
        protected PointerBuffer initialValue() {
            return PointerBuffer.allocateDirect(1);
        }

    };

    public final long getLong(int key) {

        ByteBuffer buffer = localBB.get();
        int ret = getInfo(key, 8, buffer, null);
        checkForError(ret, "error while asking for info value");

        return buffer.getLong(0);
    }

    public final String getString(int key) {
        
        ByteBuffer buffer = localBB.get();
        PointerBuffer sizeBuffer = localPB.get();
        int ret = getInfo(key, buffer.capacity(), buffer, sizeBuffer);
        checkForError(ret, "error while asking for info string");

        int clSize = (int)sizeBuffer.get(0);
        byte[] array = new byte[clSize-1]; // last char is always null
        buffer.get(array).rewind();

        return CLUtil.clString2JavaString(array, clSize);

    }

    protected abstract int getInfo(int name, long valueSize, Buffer value, PointerBuffer valueSizeRet);


}
