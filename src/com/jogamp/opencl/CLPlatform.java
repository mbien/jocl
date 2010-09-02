package com.jogamp.opencl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.DynamicLookupHelper;
import java.security.PrivilegedAction;
import com.jogamp.common.JogampRuntimeException;
import com.jogamp.common.os.NativeLibrary;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.gluegen.runtime.FunctionAddressResolver;
import com.jogamp.opencl.util.CLUtil;
import com.jogamp.opencl.impl.CLImpl;
import com.jogamp.opencl.impl.CLProcAddressTable;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import static java.security.AccessController.*;
import static com.jogamp.opencl.CLException.*;
import static com.jogamp.opencl.CL.*;

/**
 * CLPlatfrorm representing an OpenCL installation (e.g. graphics driver).
 * 
 * @author Michael Bien
 * @see #getDefault()
 * @see #listCLPlatforms()
 */
public final class CLPlatform {

    /**
     * OpenCL platform id for this platform.
     */
    public final long ID;

    /**
     * Version of this OpenCL platform.
     */
    public final CLVersion version;

    private static final CL cl;

    private Set<String> extensions;

    static{
        try {

            final CLProcAddressTable table = new CLProcAddressTable(new FunctionAddressResolver() {
                public long resolve(String name, DynamicLookupHelper lookup) {
                    
                    //FIXME workaround to fix a gluegen issue
                    if(name.endsWith("Impl")) {
                        name = name.substring(0, name.length() - "Impl".length());
                    }

                    if(name.endsWith("KHR") || name.endsWith("EXT")) {
                        long address = ((CLImpl) cl).clGetExtensionFunctionAddress(name);
                        if(address != 0) {
                            return address;
                        }
                    }

                    return lookup.dynamicLookupFunction(name);
                }
            });

            cl = new CLImpl(table);
            
            //load JOCL and init table
            doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {

                    NativeLibrary libOpenCL = JOCLJNILibLoader.loadOpenCL();
                    if(libOpenCL == null) {
                        throw new JogampRuntimeException("OpenCL library not found.");
                    }

                    //eagerly init function to query extension addresses (used in reset())
                    table.initEntry("clGetExtensionFunctionAddressImpl", libOpenCL);
                    table.reset(libOpenCL);
                    return null;
                }
            });

//            System.out.println("\n"+table);
            System.out.println("unavailable functions: "+table.getNullPointerFunctions());

        }catch(Exception ex) {
            throw new JogampRuntimeException("JOCL initialization error.", ex);
        }
    }

    private CLPlatform(long id) {
        this.ID = id;
        this.version = new CLVersion(getInfoString(CL_PLATFORM_VERSION));
    }

    /**
     * Eagerly initializes JOCL. Subsequent calls do nothing.
     */
    public static void initialize() throws JogampRuntimeException {
        //see static initializer
    }

    /**
     * Returns the default OpenCL platform or null when no platform found.
     */
    public static CLPlatform getDefault() {
        CLPlatform[] platforms = listCLPlatforms();
        CLPlatform best = platforms[0];
        for (CLPlatform platform : platforms) {
            if(platform.version.compareTo(best.version) > 0) {
                best = platform;
            }
        }
        return best;
    }

    /**
     * Lists all available OpenCL implementations.
     * @throws CLException if something went wrong initializing OpenCL
     */
    public static CLPlatform[] listCLPlatforms() {

        IntBuffer ib = Buffers.newDirectIntBuffer(1);
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
     * @see #listCLDevices(com.jogamp.opencl.CLDevice.Type...)
     */
    public CLDevice[] listCLDevices() {
        return this.listCLDevices(CLDevice.Type.ALL);
    }

    /**
     * Lists all physical devices available on this platform matching the given {@link CLDevice.Type}.
     */
    public CLDevice[] listCLDevices(CLDevice.Type... types) {

        IntBuffer ib = Buffers.newDirectIntBuffer(1);

        List<CLDevice> list = new ArrayList<CLDevice>();
        for(int t = 0; t < types.length; t++) {
            CLDevice.Type type = types[t];

            //find all devices
            int ret = cl.clGetDeviceIDs(ID, type.TYPE, 0, null, ib);

            // return an empty array rather than throwing an exception
            if(ret == CL.CL_DEVICE_NOT_FOUND) {
                continue;
            }

            checkForError(ret, "error while enumerating devices");

            PointerBuffer deviceIDs = PointerBuffer.allocateDirect(ib.get(0));
            ret = cl.clGetDeviceIDs(ID, type.TYPE, deviceIDs.capacity(), deviceIDs, null);
            checkForError(ret, "error while enumerating devices");

            //add device to list
            for (int n = 0; n < deviceIDs.capacity(); n++)
                list.add(new CLDevice(cl, deviceIDs.get(n)));
        }

        CLDevice[] devices = new CLDevice[list.size()];
        for (int i = 0; i < list.size(); i++) {
            devices[i] = list.get(i);
        }

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
     * @see #getMaxFlopsDevice(com.jogamp.opencl.CLDevice.Type...)
     */
    public CLDevice getMaxFlopsDevice() {
        return findMaxFlopsDevice(listCLDevices());
    }

    /**
     * Returns the device with maximal FLOPS and the specified type from this platform.
     * The device speed is estimated by calculating the product of
     * MAX_COMPUTE_UNITS and MAX_CLOCK_FREQUENCY.
     */
    public CLDevice getMaxFlopsDevice(CLDevice.Type... types) {
        return findMaxFlopsDevice(listCLDevices(types));
    }

    /**
     * Returns the platform name.
     */
    public String getName() {
        return getInfoString(CL_PLATFORM_NAME);
    }

    /**
     * Returns the OpenCL version supported by this platform.
     */
    public CLVersion getVersion() {
        return version;
    }

    /**
     * Returns the OpenCL Specification version supported by this platform.
     */
    public String getSpecVersion() {
        return version.getSpecVersion();
    }

    /**
     * @see CLVersion#isAtLeast(com.jogamp.opencl.CLVersion)
     */
    public boolean isAtLeast(CLVersion other) {
        return version.isAtLeast(other);
    }

    /**
     * @see CLVersion#isAtLeast(int, int) 
     */
    public boolean isAtLeast(int major, int minor) {
        return version.isAtLeast(major, minor);
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
     * Returns the ICD suffix.
     */
    public String getICDSuffix() {
        return getInfoString(CL_PLATFORM_ICD_SUFFIX_KHR);
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
     * Returns a Map of platform properties with the enum names as keys.
     * @see CLUtil#obtainPlatformProperties(com.jogamp.opencl.CLPlatform)
     */
    public Map<String, String> getProperties() {
        return CLUtil.obtainPlatformProperties(this);
    }

    /**
     * Returns a info string in exchange for a key (CL_PLATFORM_*).
     */
    public String getInfoString(int key) {
        PointerBuffer size = PointerBuffer.allocateDirect(1);
        // TODO use cache/query size
        ByteBuffer bb = ByteBuffer.allocateDirect(512);

        int ret = cl.clGetPlatformInfo(ID, key, bb.capacity(), bb, size);
        checkForError(ret, "can not receive info string");

        return CLUtil.clString2JavaString(bb, (int)size.get(0));
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
