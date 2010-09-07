/*
 * Created on Tuesday, September 07 2010 15:35
 */
package com.jogamp.opencl;

import com.jogamp.opencl.util.CLUtil;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * OpenCL property.
 * @author Michael Bien
 * @see CLUtil#obtainDeviceProperties(com.jogamp.opencl.CLDevice)
 * @see CLUtil#obtainPlatformProperties(com.jogamp.opencl.CLPlatform) 
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CLProperty {
    /**
     * The property key.
     */
    String value();
}
