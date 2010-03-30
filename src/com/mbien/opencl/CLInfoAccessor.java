package com.mbien.opencl;

import com.jogamp.gluegen.runtime.Buffers;
import com.jogamp.gluegen.runtime.Int64Buffer;
import com.mbien.opencl.util.CLUtil;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import static com.mbien.opencl.CLException.*;

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
    protected final static ThreadLocal<Int64Buffer> localPB = new ThreadLocal<Int64Buffer>() {

        @Override
        protected Int64Buffer initialValue() {
            return Int64Buffer.allocateDirect(1);
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
        Int64Buffer sizeBuffer = localPB.get();
        int ret = getInfo(key, buffer.capacity(), buffer, sizeBuffer);
        checkForError(ret, "error while asking for info string");

        int clSize = (int)sizeBuffer.get(0);
        byte[] array = new byte[clSize-1]; // last char is always null
        buffer.get(array).rewind();

        return CLUtil.clString2JavaString(array, clSize);

    }

    protected abstract int getInfo(int name, long valueSize, Buffer value, Int64Buffer valueSizeRet);


}
