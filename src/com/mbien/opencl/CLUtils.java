package com.mbien.opencl;

/**
 *
 * @author Michael Bien
 */
class CLUtils {

    public static String clString2JavaString(byte[] chars, int clLength) {
        return clLength==0 ? "" : new String(chars, 0, clLength-1);
    }

}
