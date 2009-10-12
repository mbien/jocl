package com.mbien.opencl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * 
 * @author Michael Bien
 */
public final class CLDevice {

    /**
     * Enumeration for the type of a device.
     */
    public enum Type {
        /**
         * CL_DEVICE_TYPE_CPU
         */
        CPU(CL.CL_DEVICE_TYPE_CPU),
        /**
         * CL_DEVICE_TYPE_GPU
         */
        GPU(CL.CL_DEVICE_TYPE_GPU),
        /**
         * CL_DEVICE_TYPE_ACCELERATOR
         */
        ACCELERATOR(CL.CL_DEVICE_TYPE_ACCELERATOR),
        /**
         * CL_DEVICE_TYPE_DEFAULT
         */
        DEFAULT(CL.CL_DEVICE_TYPE_DEFAULT);

         /**
         * Value of wrapped OpenCL device type.
         */
        public final int CL_TYPE;

        private Type(int CL_TYPE) {
            this.CL_TYPE = CL_TYPE;
        }

        public static Type valueOf(int clDeviceType) {
            switch(clDeviceType) {
                case(CL.CL_DEVICE_TYPE_DEFAULT):
                    return DEFAULT;
                case(CL.CL_DEVICE_TYPE_CPU):
                    return CPU;
                case(CL.CL_DEVICE_TYPE_GPU):
                    return GPU;
                case(CL.CL_DEVICE_TYPE_ACCELERATOR):
                    return ACCELERATOR;
            }
            return null;
        }
    }
    
    private final CL cl;

    /**
     * OpenCL device id for this device.
     */
    public final long deviceID;

    CLDevice(CL cl, long id) {
        this.cl = cl;
        this.deviceID = id;
    }

    /**
     * Returns the name of this device.
     */
    public String getName() {
        return getInfoString(CL.CL_DEVICE_NAME);
    }

    /**
     * Returns the OpenCL profile of this device.
     */
    public String getProfile() {
        return getInfoString(CL.CL_DEVICE_PROFILE);
    }

    /**
     * Returns the vendor of this device.
     */
    public String getVendor() {
        return getInfoString(CL.CL_DEVICE_VENDOR);
    }

    /**
     * Returns the type of this device.
     */
    public Type getType() {
        return Type.valueOf((int)getInfoLong(CL.CL_DEVICE_TYPE));
    }

    /**
     * Returns the maximal number of compute units.
     */
    public int getMaxComputeUnits() {
        return (int) getInfoLong(CL.CL_DEVICE_MAX_COMPUTE_UNITS);
    }

    /**
     * Returns the maximal work group size.
     */
    public int getMaxWorkGroupSize() {
        return (int) getInfoLong(CL.CL_DEVICE_MAX_WORK_GROUP_SIZE);
    }

    /**
     * Returns the max clock frequency in Hz.
     */
    public int getMaxClockFrequency() {
        return (int) (getInfoLong(CL.CL_DEVICE_MAX_CLOCK_FREQUENCY));
    }

    /**
     * Returns the global memory size in Bytes.
     */
    public long getGlobalMemSize() {
        return getInfoLong(CL.CL_DEVICE_GLOBAL_MEM_SIZE);
    }

    /**
     * Returns the local memory size in Bytes.
     */
    public long getLocalMemSize() {
        return getInfoLong(CL.CL_DEVICE_LOCAL_MEM_SIZE);
    }

    /**
     * Returns all device extension names as unmodifiable Set.
     */
    public Set<String> getExtensions() {

        String ext = getInfoString(CL.CL_DEVICE_EXTENSIONS);

        Scanner scanner = new Scanner(ext);
        Set<String> extSet = new HashSet<String>();

        while(scanner.hasNext())
            extSet.add(scanner.next());

        return Collections.unmodifiableSet(extSet);
    }

    //TODO CL_DEVICE_IMAGE_SUPPORT
    //TODO CL_DEVICE_MAX_WORK_ITEM_SIZES


    private final long getInfoLong(int key) {

        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.order(ByteOrder.nativeOrder());

        int ret = cl.clGetDeviceInfo(deviceID, key, bb.capacity(), bb, null, 0);

        if(CL.CL_SUCCESS != ret)
            throw new CLException(ret, "can not receive device info");

        return bb.getLong();
    }

    public final String getInfoString(int key) {

        long[] longBuffer = new long[1];
        ByteBuffer bb = ByteBuffer.allocate(512);

        int ret = cl.clGetDeviceInfo(deviceID, key, bb.capacity(), bb, longBuffer, 0);
        
        if(CL.CL_SUCCESS != ret)
            throw new CLException(ret, "can not receive device info string");

        return new String(bb.array(), 0, (int)longBuffer[0]);
        
    }


    @Override
    public String toString() {
        return "CLPlatform [name:" + getName()
                        + " type:" + getType()
                        + " profile: " + getProfile()+"]";
    }

    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj instanceof CLDevice)
            return ((CLDevice)obj).deviceID == deviceID;
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + (int) (this.deviceID ^ (this.deviceID >>> 32));
        return hash;
    }

}
