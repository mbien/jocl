/*
 * Copyright 2011 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package com.jogamp.opencl.util;

import com.jogamp.opencl.CLCommandQueue.Mode;
import com.jogamp.opencl.CLDevice;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

/**
 * Pre-defined filters.
 * @author Michael Bien
 * @see com.jogamp.opencl.CLPlatform#listCLDevices(com.jogamp.opencl.util.Filter[]) 
 * @see com.jogamp.opencl.CLPlatform#getMaxFlopsDevice(com.jogamp.opencl.util.Filter[])
 */
public class CLDeviceFilters {
    
    /**
     * Accepts all devices of the given type.
     */
    public static Filter<CLDevice> type(final CLDevice.Type type) {
        return new Filter<CLDevice>() {
            public boolean accept(CLDevice item) {
                if(type.equals(CLDevice.Type.ALL)) {
                    return true;
                }
                return item.getType().equals(type);
            }
        };
    }
    
    /**
     * Accepts all devices of the given {@link ByteOrder}.
     */
    public static Filter<CLDevice> byteOrder(final ByteOrder order) {
        return new Filter<CLDevice>() {
            public boolean accept(CLDevice item) {
                return item.getByteOrder().equals(order);
            }
        };
    }
    
    /**
     * Accepts all devices which support OpenGL-OpenCL interoparability.
     */
    public static Filter<CLDevice> glSharing() {
        return new Filter<CLDevice>() {
            public boolean accept(CLDevice item) {
                return item.isGLMemorySharingSupported();
            }
        };
    }
    
    /**
     * Accepts all devices supporting the given extensions.
     */
    public static Filter<CLDevice> extension(final String... extensions) {
        return new Filter<CLDevice>() {
            private final List<String> extensionList = Arrays.asList(extensions);
            public boolean accept(CLDevice item) {
                return item.getExtensions().containsAll(extensionList);
            }
        };
    }
    
    /**
     * Accepts all devices supporting the specified command queue modes.
     */
    public static Filter<CLDevice> queueMode(final Mode... modes) {
        return new Filter<CLDevice>() {
            private final List<Mode> modeList = Arrays.asList(modes);
            public boolean accept(CLDevice item) {
                return item.getQueueProperties().containsAll(modeList);
            }
        };
    }
    
}
