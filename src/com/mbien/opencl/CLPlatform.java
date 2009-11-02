package com.mbien.opencl;

import com.mbien.opencl.impl.CLImpl;
import java.nio.ByteBuffer;
import static com.mbien.opencl.CLException.*;
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

    static{
        System.loadLibrary("gluegen-rt");
        System.loadLibrary("jocl");
        cl = new CLImpl();
    }

    CLPlatform(long id) {
        this.ID = id;
    }

    /**
     * Lists all available OpenCL implementaitons.
     * @throws CLException if something went wrong initializing OpenCL
     */
    public static CLPlatform[] listCLPlatforms() {

        int[] intBuffer = new int[1];
        // find all available OpenCL platforms
        int ret = cl.clGetPlatformIDs(0, null, 0, intBuffer, 0);
        checkForError(ret, "can not enumerate platforms");

        // receive platform ids
        long[] platformId = new long[intBuffer[0]];
        ret = cl.clGetPlatformIDs(platformId.length, platformId, 0, null, 0);
        checkForError(ret, "can not enumerate platforms");

        CLPlatform[] platforms = new CLPlatform[platformId.length];

        for (int i = 0; i < platformId.length; i++)
            platforms[i] = new CLPlatform(platformId[i]);

        return platforms;
    }

    /**
     * Returns the low level binding interface to the OpenCL APIs.
     */
    public static CL getLowLevelBinding() {
        return cl;
    }

    /**
     * Lists all physical devices available on this platform.
     */
    public CLDevice[] listCLDevices() {

        int[] intBuffer = new int[1];

        //find all devices
        int ret = cl.clGetDeviceIDs(ID, CL.CL_DEVICE_TYPE_ALL, 0, null, 0, intBuffer, 0);
        checkForError(ret, "error while enumerating devices");

        long[] deviceIDs = new long[intBuffer[0]];
        ret = cl.clGetDeviceIDs(ID, CL.CL_DEVICE_TYPE_ALL, deviceIDs.length, deviceIDs, 0, null, 0);
        checkForError(ret, "error while enumerating devices");

        CLDevice[] devices = new CLDevice[deviceIDs.length];

        //print device info
        for (int i = 0; i < deviceIDs.length; i++)
            devices[i] = new CLDevice(cl, deviceIDs[i]);

        return devices;

    }

    static final CLDevice findMaxFlopsDevice(CLDevice[] devices) {

        CLDevice maxFLOPSDevice = null;

        int maxflops = -1;

        for (int i = 0; i < devices.length; i++) {

            CLDevice device = devices[i];
            int maxComputeUnits     = device.getMaxComputeUnits();
            int maxClockFrequency   = device.getMaxClockFrequency();
            int flops = maxComputeUnits*maxClockFrequency;

            if(flops > maxflops) {
                maxflops = flops;
                maxFLOPSDevice = device;
            }
        }

        return maxFLOPSDevice;
    }


    /**
     * Gets the device with maximal FLOPS from this platform.
     * The device speed is estimated by calulating the product of
     * MAX_COMPUTE_UNITS and MAX_CLOCK_FREQUENCY.
     */
    public CLDevice getMaxFlopsDevice() {
        return findMaxFlopsDevice(listCLDevices());
    }

    /**
     * Returns the platform name.
     */
    public String getName() {
        return getInfoString(CL.CL_PLATFORM_NAME);
    }

    /**
     * Returns the platform version.
     */
    public String getVersion() {
        return getInfoString(CL.CL_PLATFORM_VERSION);
    }

    /**
     * Returns the platform profile.
     */
    public String getProfile() {
        return getInfoString(CL.CL_PLATFORM_PROFILE);
    }

    /**
     * Returns the platform vendor.
     */
    public String getVendor() {
        return getInfoString(CL.CL_PLATFORM_VENDOR);
    }

    /**
     * Returns a info string in exchange for a key (CL_PLATFORM_*).
     */
    public String getInfoString(int key) {
        long[] longBuffer = new long[1];
        ByteBuffer bb = ByteBuffer.allocate(512);

        int ret = cl.clGetPlatformInfo(ID, key, bb.capacity(), bb, longBuffer, 0);
        checkForError(ret, "can not receive info string");

        return new String(bb.array(), 0, (int)longBuffer[0]);
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
