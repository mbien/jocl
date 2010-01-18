package com.mbien.opencl;

import java.nio.ByteBuffer;

/**
 *
 * @author Michael Bien
 */
class CLUtils {

    public static String clString2JavaString(byte[] chars, int clLength) {
        return clLength==0 ? "" : new String(chars, 0, clLength-1);
    }

    public static String clString2JavaString(ByteBuffer chars, int clLength) {
        if (clLength==0) {
            return "";
        }else{
            byte[] array = new byte[clLength-1]; // last char is always null
            chars.get(array).rewind();
            return new String(array, 0, clLength-1);
        }
    }

}
