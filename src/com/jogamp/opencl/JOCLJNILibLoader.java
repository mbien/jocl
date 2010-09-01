package com.jogamp.opencl;

import com.jogamp.common.jvm.JNILibLoaderBase;
import com.jogamp.common.os.NativeLibrary;


/**
 * Responsible for JOCL lib loading.
 * @author Michael Bien
 */
class JOCLJNILibLoader extends JNILibLoaderBase {

    /**
     * Loads the native binding and returns the OpenCL library for dynamic linking.
     * @return Returns libOpenCL represented as NativeLibrary.
     */
    static NativeLibrary loadOpenCL() {
        loadLibrary("jocl", null, false);
        return NativeLibrary.open("OpenCL", JOCLJNILibLoader.class.getClassLoader());
    }
}
