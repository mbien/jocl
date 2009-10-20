package com.mbien.opencl;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * @author Michael Bien
 */
public class TestUtils {

    public static final void fillBuffer(ByteBuffer buffer, int seed) {

        Random rnd = new Random(seed);

        while(buffer.remaining() != 0)
            buffer.putInt(rnd.nextInt());

        buffer.rewind();
    }

    public static final int roundUp(int groupSize, int globalSize) {
        int r = globalSize % groupSize;
        if (r == 0) {
            return globalSize;
        } else {
            return globalSize + groupSize - r;
        }
    }
}
