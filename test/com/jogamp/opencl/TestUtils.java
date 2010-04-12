package com.jogamp.opencl;

import java.nio.ByteBuffer;
import java.util.Random;

import static java.lang.System.*;
import static org.junit.Assert.*;

/**
 * @author Michael Bien
 */
public class TestUtils {

    //decrease this value on systems with few memory.
    final static int ONE_MB = 1048576;

    final static int NUM_ELEMENTS = 10000000;

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

    public static final void checkIfEqual(ByteBuffer a, ByteBuffer b, int elements) {
        for(int i = 0; i < elements; i++) {
            int aVal = a.getInt();
            int bVal = b.getInt();
            if(aVal != bVal) {
                out.println("a: "+aVal);
                out.println("b: "+bVal);
                out.println("position: "+a.position());
                fail("a!=b");
            }
        }
        a.rewind();
        b.rewind();
    }
}
