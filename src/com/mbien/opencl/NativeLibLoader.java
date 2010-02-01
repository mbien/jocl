package com.mbien.opencl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import com.sun.nativewindow.impl.NativeLibLoaderBase;

/**
 *
 * @author Michael Bien
 */
class NativeLibLoader extends NativeLibLoaderBase {

    @SuppressWarnings("unchecked")
    public static void loadJOCL() {
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                loadLibrary("jocl", null, true);
                return null;
            }
        });
    }
}
