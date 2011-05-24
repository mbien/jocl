/*
 * Created on Thursday, May 19 2011 16:47
 */
package com.jogamp.opencl.spi;

/**
 *
 * @author Michael Bien
 */
public interface CLPlatformInfoAccessor extends CLInfoAccessor {

    long[] getDeviceIDs(long type);

}
