/*
 * Created on Wednesday, May 25 2011 00:53
 */
package com.jogamp.opencl.spi;

import com.jogamp.opencl.llb.CL;

/**
 * Implementations of this interface are factories responsible for creating CLAccessors.
 * @author Michael Bien
 */
public interface CLAccessorFactory {

    CLInfoAccessor createDeviceInfoAccessor(CL cl, long id);

    CLPlatformInfoAccessor createPlatformInfoAccessor(CL cl, long id);

}
