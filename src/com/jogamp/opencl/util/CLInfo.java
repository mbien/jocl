/*
 * Created on Tuesday, September 07 2010 15:35
 */
package com.jogamp.opencl.util;

import com.jogamp.common.os.Platform;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.impl.CLImpl;
import java.util.Map;


/**
 * Prints out diagnostic properties about the OpenCL installation and the runtime environment of the host application.
 * @author Michael Bien
 */
public class CLInfo {

    public static void main(String[] args) throws Exception {

        // host
        System.out.println("HOST_JRE: " + System.getProperty("java.runtime.version"));
        System.out.println("HOST_JVM: " + System.getProperty("java.vm.name"));
        System.out.println("HOST_ARCH: " + Platform.getArch());
        System.out.println("HOST_NUM_CORES: " + Runtime.getRuntime().availableProcessors());
        System.out.println("HOST_OS: " + Platform.getOS());
        System.out.println("HOST_LITTLE_ENDIAN: " + Platform.isLittleEndian());
        
        CLPlatform.initialize();

        // binding
        System.out.println();
        System.out.println("CL_BINDING_UNAVAILABLE_FUNCTIONS: " +
                ((CLImpl)CLPlatform.getLowLevelCLInterface()).getAddressTable().getNullPointerFunctions());

        // OpenCL
        CLPlatform[] platforms = CLPlatform.listCLPlatforms();

        for (CLPlatform platform : platforms) {
            Map<String, String> platformProperties = platform.getProperties();
            System.out.println();
            printInfo("", platformProperties);

            CLDevice[] devices = platform.listCLDevices();
            for (CLDevice device : devices) {
                Map<String, String> deviceProperties = device.getProperties();
                System.out.println();
                printInfo(" - ", deviceProperties);
            }
        }

    }

    private static void printInfo(String prefix, Map<String, String> properties) {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            System.out.println(prefix + entry.getKey() + ": " + entry.getValue());
        }
    }
}
