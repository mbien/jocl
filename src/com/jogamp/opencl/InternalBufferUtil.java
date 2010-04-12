package com.jogamp.opencl;

import java.lang.reflect.Field;
import java.nio.Buffer;
import sun.misc.Unsafe;

/**
 *
 * @author Michael Bien
 */
class InternalBufferUtil {

    private static final long addressFieldOffset;
    private static Unsafe unsafe;

    static {
        try {
            Field f = Buffer.class.getDeclaredField("address");

            Field[] fields = Unsafe.class.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].getName().equals("theUnsafe")) {
                    fields[i].setAccessible(true);
                    unsafe = (Unsafe)fields[i].get(Unsafe.class);
                    break;
                }
            }

            addressFieldOffset = unsafe.objectFieldOffset(f);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static long getDirectBufferAddress(Buffer buffer) {
        return ((buffer == null) ? 0 : unsafe.getLong(buffer, addressFieldOffset));
    }

}