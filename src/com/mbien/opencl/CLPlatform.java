package com.mbien.opencl;

import com.mbien.opencl.impl.CLImpl;
import com.sun.gluegen.runtime.PointerBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import static com.mbien.opencl.CLException.*;
import static com.mbien.opencl.CL.*;

/**
 *
 * @author Michael Bien
 */
public final class CLPlatform {

    /**
     * OpenCL platform id for this platform.
     */
    public final long ID;

    private static final CL cl;

    private Set<String> extensions;

    static{
        NativeLibLoader.loadJOCL();
//        System.loadLibrary("gluegen-rt");
//        ProcAddressHelper.resetProcAddressTable(table, null);
        cl = new CLImpl();
    }

    CLPlatform(long id) {
        this.ID = id;
    }

    /**
     * Returns the default OpenCL platform or null when no platform found.
     */
    public static CLPlatform getDefault() {
        CLPlatform[] platforms = listCLPlatforms();
        if(platforms.length > 0)
            return platforms[0];
        return null;
    }

    /**
     * Lists all available OpenCL implementations.
     * @throws CLException if something went wrong initializing OpenCL
     */
    public static CLPlatform[] listCLPlatforms() {

        IntBuffer ib = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        // find all available OpenCL platforms
        int ret = cl.clGetPlatformIDs(0, null, ib);
        checkForError(ret, "can not enumerate platforms");

        // receive platform ids
        PointerBuffer platformId = PointerBuffer.allocateDirect(ib.get(0));
        ret = cl.clGetPlatformIDs(platformId.capacity(), platformId, null);
        checkForError(ret, "can not enumerate platforms");

        CLPlatform[] platforms = new CLPlatform[platformId.capacity()];

        for (int i = 0; i < platformId.capacity(); i++)
            platforms[i] = new CLPlatform(platformId.get(i));

        return platforms;
    }

    /**
     * Returns the low level binding interface to the OpenCL APIs.
     */
    public static CL getLowLevelCLInterface() {
        return cl;
    }

    /**
     * Hint to allow the implementation to release the resources allocated by the OpenCL compiler.
     * Calls to {@link CLProgram#build()} after unloadCompiler will reload the compiler if necessary.
     */
    public static void unloadCompiler() {
        int ret = cl.clUnloadCompiler();
        checkForError(ret, "error while sending unload compiler hint");
    }

    /**
     * Lists all physical devices available on this platform.
     * @see #listCLDevices(com.mbien.opencl.CLDevice.Type)
     */
    public CLDevice[] listCLDevices() {
        return this.listCLDevices(CLDevice.Type.ALL);
    }

    /**
     * Lists all physical devices available on this platform matching the given {@link CLDevice.Type}.
     */
    public CLDevice[] listCLDevices(CLDevice.Type type) {

        IntBuffer ib = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();

        //find all devices
        int ret = cl.clGetDeviceIDs(ID, type.TYPE, 0, null, ib);

        // return an empty array rather than throwing an exception
        if(ret == CL.CL_DEVICE_NOT_FOUND) {
            return new CLDevice[0];
        }

        checkForError(ret, "error while enumerating devices");

        PointerBuffer deviceIDs = PointerBuffer.allocateDirect(ib.get(0));
        ret = cl.clGetDeviceIDs(ID, type.TYPE, deviceIDs.capacity(), deviceIDs, null);
        checkForError(ret, "error while enumerating devices");

        CLDevice[] devices = new CLDevice[deviceIDs.capacity()];

        //print device info
        for (int i = 0; i < deviceIDs.capacity(); i++)
            devices[i] = new CLDevice(cl, deviceIDs.get(i));

        return devices;

    }

    static CLDevice findMaxFlopsDevice(CLDevice[] devices) {
        return findMaxFlopsDevice(devices, null);
    }
    
    static CLDevice findMaxFlopsDevice(CLDevice[] devices, CLDevice.Type type) {

        CLDevice maxFLOPSDevice = null;

        int maxflops = -1;

        for (int i = 0; i < devices.length; i++) {

            CLDevice device = devices[i];

            if(type == null || type.equals(device.getType())) {

                int maxComputeUnits     = device.getMaxComputeUnits();
                int maxClockFrequency   = device.getMaxClockFrequency();
                int flops = maxComputeUnits*maxClockFrequency;

                if(flops > maxflops) {
                    maxflops = flops;
                    maxFLOPSDevice = device;
                }
            }

        }

        return maxFLOPSDevice;
    }


    /**
     * Returns the device with maximal FLOPS from this platform.
     * The device speed is estimated by calculating the product of
     * MAX_COMPUTE_UNITS and MAX_CLOCK_FREQUENCY.
     * @see #getMaxFlopsDevice(com.mbien.opencl.CLDevice.Type)
     */
    public CLDevice getMaxFlopsDevice() {
        return findMaxFlopsDevice(listCLDevices());
    }

    /**
     * Returns the device with maximal FLOPS and the specified type from this platform.
     * The device speed is estimated by calculating the product of
     * MAX_COMPUTE_UNITS and MAX_CLOCK_FREQUENCY.
     */
    public CLDevice getMaxFlopsDevice(CLDevice.Type type) {
        return findMaxFlopsDevice(listCLDevices(type));
    }

    /**
     * Returns the platform name.
     */
    public String getName() {
        return getInfoString(CL_PLATFORM_NAME);
    }

    /**
     * Returns the platform version.
     */
    public String getVersion() {
        return getInfoString(CL_PLATFORM_VERSION);
    }

    /**
     * Returns the platform profile.
     */
    public String getProfile() {
        return getInfoString(CL_PLATFORM_PROFILE);
    }

    /**
     * Returns the platform vendor.
     */
    public String getVendor() {
        return getInfoString(CL_PLATFORM_VENDOR);
    }

    /**
     * Returns true if the extension is supported on this platform.
     */
    public boolean isExtensionAvailable(String extension) {
        return getExtensions().contains(extension);
    }

    /**
     * Returns all platform extension names as unmodifiable Set.
     */
    public Set<String> getExtensions() {

        if(extensions == null) {
            extensions = new HashSet<String>();
            String ext = getInfoString(CL_PLATFORM_EXTENSIONS);
            Scanner scanner = new Scanner(ext);

            while(scanner.hasNext())
                extensions.add(scanner.next());

            extensions = Collections.unmodifiableSet(extensions);
        }

        return extensions;
    }

    /**
     * Returns a info string in exchange for a key (CL_PLATFORM_*).
     */
    public String getInfoString(int key) {
        PointerBuffer pb = PointerBuffer.allocateDirect(1);
        ByteBuffer bb = ByteBuffer.allocateDirect(512);

        int ret = cl.clGetPlatformInfo(ID, key, bb.capacity(), bb, pb);
        checkForError(ret, "can not receive info string");

        return CLUtils.clString2JavaString(bb, (int)pb.get(0));
    }

    @Override
    public String toString() {
        return "CLPlatform [name:" + getName()
                         +" vendor:"+getVendor()
                         +" profile:"+getProfile()
                         +" version:"+getVersion()+"]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CLPlatform other = (CLPlatform) obj;
        if (this.ID != other.ID) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + (int) (this.ID ^ (this.ID >>> 32));
        return hash;
    }


}
