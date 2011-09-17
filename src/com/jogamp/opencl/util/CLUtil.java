/*
 * Copyright 2009 - 2010 JogAmp Community. All rights reserved.
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

import com.jogamp.common.JogampRuntimeException;
import com.jogamp.opencl.llb.CL;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.CLProperty;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Michael Bien
 */
public class CLUtil {

    public static String clString2JavaString(byte[] chars, int clLength) {

        // certain char queries on windows always claim to have a fixed length
        // e.g. (clDeviceInfo(CL_DEVICE_NAME) is always 64.. but luckily they are 0 terminated)
        while(clLength > 0 && chars[--clLength] == 0);

        return clLength==0 ? "" : new String(chars, 0, clLength+1);
    }

    public static String clString2JavaString(ByteBuffer chars, int clLength) {
        if (clLength==0) {
            return "";
        }else{
            byte[] array = new byte[clLength];
            chars.get(array).rewind();
            return clString2JavaString(array, clLength);
        }
    }

    /**
     * Returns true if clBoolean == CL.CL_TRUE.
     */
    public static boolean clBoolean(int clBoolean) {
        return clBoolean == CL.CL_TRUE;
    }

    /**
     * Returns b ? CL.CL_TRUE : CL.CL_FALSE
     */
    public static int clBoolean(boolean b) {
        return b ? CL.CL_TRUE : CL.CL_FALSE;
    }

    /**
     * Rounds the value up to the nearest multiple.
     */
    public static int roundUp(int value, int requiredMultiple) {
        int r = value % requiredMultiple;
        if (r == 0) {
            return value;
        } else {
            return value + requiredMultiple - r;
        }
    }

    /**
     * Reads chars from input stream and puts them into the supplied StringBuilder.
     * The stream is closed after successful or unsuccessful read.
     */
    public static StringBuilder readStream(InputStream source, StringBuilder dest) throws IOException {
        return readStream(source, dest, new char[1024]);
    }

    /**
     * Reads chars from input stream and puts them into the supplied StringBuilder using the supplied buffer.
     * The stream is closed after successful or unsuccessful read.
     */
    public static StringBuilder readStream(InputStream source, StringBuilder dest, char[] buffer) throws IOException {
        InputStreamReader reader = new InputStreamReader(source);
        try {
            int len = 0;
            while ((len = reader.read(buffer)) != -1)
                dest.append(buffer, 0, len);
        } finally {
            reader.close();
        }
        return dest;
    }

    /**
     * Reads all platform properties and returns them as key-value map.
     */
    public static Map<String, String> obtainPlatformProperties(CLPlatform platform) {
        return readCLProperties(platform);
    }

    /**
     * Reads all device properties and returns them as key-value map.
     */
    public static Map<String, String> obtainDeviceProperties(CLDevice dev) {
        return readCLProperties(dev);
    }

    private static Map<String, String> readCLProperties(Object obj) {
        try {
            return invoke(listMethods(obj.getClass()), obj);
        } catch (IllegalArgumentException ex) {
            throw new JogampRuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new JogampRuntimeException(ex);
        }
    }

    static Map<String, String> invoke(List<Method> methods, Object obj) throws IllegalArgumentException, IllegalAccessException {
        Map<String, String> map = new LinkedHashMap<String, String>();
        for (Method method : methods) {
            Object info = null;
            try {
                info = method.invoke(obj);
            } catch (InvocationTargetException ex) {
                info = ex.getTargetException();
            }

            if(info.getClass().isArray()) {
                info = asList(info);
            }

            String value = method.getAnnotation(CLProperty.class).value();
            map.put(value, info.toString());
        }
        return map;
    }

    static List<Method> listMethods(Class<?> clazz) throws SecurityException {
        List<Method> list = new ArrayList<Method>();
        for (Method method : clazz.getDeclaredMethods()) {
            Annotation[] annotations = method.getDeclaredAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof CLProperty) {
                    list.add(method);
                }
            }
        }
        return list;
    }

    private static List<Number> asList(Object info) {
        List<Number> list = new ArrayList<Number>();
        if(info instanceof int[]) {
            int[] array = (int[]) info;
            for (int i : array) {
                list.add(i);
            }
        }else if(info instanceof long[]) {
            long[] array = (long[]) info;
            for (long i : array) {
                list.add(i);
            }
        }else if(info instanceof float[]) {
            float[] array = (float[]) info;
            for (float i : array) {
                list.add(i);
            }
        }else if(info instanceof double[]) {
            double[] array = (double[]) info;
            for (double i : array) {
                list.add(i);
            }
        }
        return list;
    }

}
