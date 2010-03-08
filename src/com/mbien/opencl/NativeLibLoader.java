package com.mbien.opencl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import com.sun.nativewindow.impl.NativeLibLoaderBase;

/**
 *
 * @author Michael Bien
 */
class NativeLibLoader extends NativeLibLoaderBase {

    public static void loadJOCL() {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                loadLibrary("jocl", null, true);
                return null;
            }
        });
    }
}
