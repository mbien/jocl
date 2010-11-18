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

    public static StringBuilder print(StringBuilder sb) {

        // host
        sb.append("HOST_JRE: ").append(System.getProperty("java.runtime.version")).append("\n");
        sb.append("HOST_JVM: ").append(System.getProperty("java.vm.name")).append("\n");
        sb.append("HOST_ARCH: ").append(Platform.getArch()).append("\n");
        sb.append("HOST_NUM_CORES: ").append(Runtime.getRuntime().availableProcessors()).append("\n");
        sb.append("HOST_OS: ").append(Platform.getOS()).append("\n");
        sb.append("HOST_LITTLE_ENDIAN: ").append(Platform.isLittleEndian()).append("\n");

        CLPlatform.initialize();

        // binding
        sb.append("CL_BINDING_UNAVAILABLE_FUNCTIONS: ");
        sb.append(((CLImpl) CLPlatform.getLowLevelCLInterface()).getAddressTable().getNullPointerFunctions());
        sb.append("\n");

        // OpenCL
        CLPlatform[] platforms = CLPlatform.listCLPlatforms();

        for (CLPlatform platform : platforms) {
            Map<String, String> platformProperties = platform.getProperties();
            sb.append("\n");
            printInfo(sb, "", platformProperties);

            CLDevice[] devices = platform.listCLDevices();
            for (CLDevice device : devices) {
                Map<String, String> deviceProperties = device.getProperties();
                sb.append("\n");
                printInfo(sb, " - ", deviceProperties);
            }
        }

        return sb;
    }


    private static void printInfo(StringBuilder sb, String prefix, Map<String, String> properties) {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            sb.append(prefix).append(entry.getKey()).append(": ").append(entry.getValue()).append(Platform.getNewline());
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(print(new StringBuilder()).toString());
    }
}
