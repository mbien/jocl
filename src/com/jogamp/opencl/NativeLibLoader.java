package com.jogamp.opencl;

import java.security.PrivilegedAction;
import com.jogamp.common.jvm.JNILibLoaderBase;
import com.jogamp.common.os.NativeLibrary;

import static java.security.AccessController.*;

/**
 * Responsible for JOCL lib loading.
 * @author Michael Bien
 */
class NativeLibLoader extends JNILibLoaderBase {

    /**
     * Loads the native binding and returns the OpenCL library for dynamic linking.
     */
    static NativeLibrary loadJOCL() {

        return doPrivileged(new PrivilegedAction<NativeLibrary>() {
            public NativeLibrary run() {
                loadLibrary("jocl", null, true);
                return NativeLibrary.open("OpenCL", NativeLibLoader.class.getClassLoader());
            }
        });
    }
}
