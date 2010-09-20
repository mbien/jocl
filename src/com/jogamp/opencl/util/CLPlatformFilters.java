package com.jogamp.opencl.util;

import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.CLVersion;
import java.util.Arrays;

/**
 * Pre-defined filters.
 * @author Michael Bien
 * @see CLPlatform#getDefault(com.jogamp.opencl.util.Filter<com.jogamp.opencl.CLPlatform>[])
 * @see CLPlatform#listCLPlatforms(com.jogamp.opencl.util.Filter<com.jogamp.opencl.CLPlatform>[]) 
 */
public class CLPlatformFilters {

    /**
     * Accepts all platforms supporting at least the given OpenCL spec version.
     */
    public static Filter<CLPlatform> version(final CLVersion version) {
        return new Filter<CLPlatform>() {
            public boolean accept(CLPlatform item) {
                return item.isAtLeast(version);
            }
        };
    }

    /**
     * Accepts all platforms containing devices of the given type.
     */
    public static Filter<CLPlatform> type(final CLDevice.Type type) {
        return new Filter<CLPlatform>() {
            public boolean accept(CLPlatform item) {
                return item.listCLDevices(type).length > 0;
            }
        };
    }

    /**
     * Accepts all platforms containing devices of the given extensions.
     */
    public static Filter<CLPlatform> extensions(final String... extensions) {
        return new Filter<CLPlatform>() {
            public boolean accept(CLPlatform item) {
                return item.getExtensions().containsAll(Arrays.asList(extensions));
            }
        };
    }
}
