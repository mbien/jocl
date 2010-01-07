package com.mbien.opencl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.mbien.opencl.CLException.*;

/**
 * Internal utility for common OpenCL clGetFooInfo calls.
 * @author Michael Bien
 */
abstract class CLInfoAccessor {

    private final static ByteBuffer buffer;
    private final static long[] longBuffer;

    // TODO revisit for command queue concurrency
    // TODO use direct memory code path as soon gluegen is fixed
    static{
        buffer = ByteBuffer.allocate(512);
        buffer.order(ByteOrder.nativeOrder());
        longBuffer = new long[1];
    }

    public CLInfoAccessor() {
    }

    final long getLong(int key) {

        buffer.rewind();
        int ret = getInfo(key, 8, buffer, null, 0);
        checkForError(ret, "error while asking for info value");

        return buffer.getLong();
    }

    final String getString(int key) {

        buffer.rewind();
        int ret = getInfo(key, buffer.capacity(), buffer, longBuffer, 0);
        checkForError(ret, "error while asking for info string");

        return CLUtils.clString2JavaString(buffer.array(), (int)longBuffer[0]);

    }

    protected abstract int getInfo(int name, long valueSize, Buffer value, long[] valueSizeRet, int valueSizeRetOffset);


}
