package com.mbien.opencl;

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
    public  final long platformID;

    private final CL cl;

    CLPlatform(CL cl, long id) {
        this.platformID = id;
        this.cl = cl;
    }

    /**
     * Lists all physical devices available on this platform.
     */
    public CLDevice[] listCLDevices() {

        int[] intBuffer = new int[1];

        //find all devices
        int ret = cl.clGetDeviceIDs(platformID, CL.CL_DEVICE_TYPE_ALL, 0, null, 0, intBuffer, 0);
        checkForError(ret, "error while enumerating devices");

        long[] deviceIDs = new long[intBuffer[0]];
        ret = cl.clGetDeviceIDs(platformID, CL.CL_DEVICE_TYPE_ALL, deviceIDs.length, deviceIDs, 0, null, 0);
        checkForError(ret, "error while enumerating devices");

        CLDevice[] devices = new CLDevice[deviceIDs.length];

        //print device info
        for (int i = 0; i < deviceIDs.length; i++)
            devices[i] = new CLDevice(cl, deviceIDs[i]);

        return devices;

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

        int ret = cl.clGetPlatformInfo(platformID, key, bb.capacity(), bb, longBuffer, 0);
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




}
