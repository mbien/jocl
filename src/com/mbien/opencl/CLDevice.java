package com.mbien.opencl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import static com.mbien.opencl.CLException.*;

/**
 * 
 * @author Michael Bien
 */
public final class CLDevice {

    private final CL cl;
    private CLContext context;

    /**
     * OpenCL device id for this device.
     */
    public final long ID;

    CLDevice(CL cl, long id) {
        this.cl = cl;
        this.ID = id;
    }

    CLDevice(CLContext context, long id) {
        this.context = context;
        this.cl = context.cl;
        this.ID = id;
    }

    public CLCommandQueue createCommandQueue() {
        return createCommandQueue(0);
    }

    public CLCommandQueue createCommandQueue(CLCommandQueue.Mode property) {
        return createCommandQueue(property.CL_QUEUE_MODE);
    }

    public CLCommandQueue createCommandQueue(CLCommandQueue.Mode... properties) {
        int flags = 0;
        if(properties != null) {
            for (int i = 0; i < properties.length; i++) {
                flags |= properties[i].CL_QUEUE_MODE;
            }
        }
        return createCommandQueue(flags);
    }
    
    public CLCommandQueue createCommandQueue(long properties) {
        if(context == null)
            throw new IllegalStateException("this device is not associated with a context");
        return context.createCommandQueue(this, properties);
    }

    /*keep this package private for now, may be null*/
    CLContext getContext() {
        return context;
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
     * Returns the number of parallel compute cores on the OpenCL device.
     * The minimum value is 1.
     */
    public int getMaxComputeUnits() {
        return (int) getInfoLong(CL.CL_DEVICE_MAX_COMPUTE_UNITS);
    }

    /**
     * Returns the maximum number of work-items in a work-group executing
     * a kernel using the data parallel execution model.
     * The minimum value is 1.
     */
    public int getMaxWorkGroupSize() {
        return (int) getInfoLong(CL.CL_DEVICE_MAX_WORK_GROUP_SIZE);
    }

    /**
     * Returns the maximum configured clock frequency of the device in MHz.
     */
    public int getMaxClockFrequency() {
        return (int) (getInfoLong(CL.CL_DEVICE_MAX_CLOCK_FREQUENCY));
    }

    /**
     * Returns the maximum dimensions that specify the global and local work-item
     * IDs used by the data parallel execution model.
     * The minimum value is 3.
     */
    public int getMaxWorkItemDimensions() {
        return (int) getInfoLong(CL.CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS);
    }

    /**
     * Returns the global memory size in bytes.
     */
    public long getGlobalMemSize() {
        return getInfoLong(CL.CL_DEVICE_GLOBAL_MEM_SIZE);
    }

    /**
     * Returns the local memory size in bytes.
     */
    public long getLocalMemSize() {
        return getInfoLong(CL.CL_DEVICE_LOCAL_MEM_SIZE);
    }

    /**
     * Returns the max size in bytes of a constant buffer allocation.
     * The minimum value is 64 KB.
     */
    public long getMaxConstantBufferSize() {
        return getInfoLong(CL.CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE);
    }

    /**
     * Returns the size of global memory cache line in bytes.
     */
    public long getGlobalMemCachlineSize() {
        return getInfoLong(CL.CL_DEVICE_GLOBAL_MEM_CACHELINE_SIZE);
    }

    /**
     * Returns the size of global memory cache in bytes.
     */
    public long getGlobalMemCachSize() {
        return getInfoLong(CL.CL_DEVICE_GLOBAL_MEM_CACHE_SIZE);
    }

    /**
     * Returns true if images are supported by the OpenCL device and false otherwise.
     */
    public boolean isImageSupportAvailable() {
        return getInfoLong(CL.CL_DEVICE_IMAGE_SUPPORT) == CL.CL_TRUE;
    }

    /**
     * Returns the max number of simultaneous image objects that can be read by a kernel.
     * The minimum value is 128 if image support is available.
     */
    public int getMaxReadImageArgs() {
        return (int)getInfoLong(CL.CL_DEVICE_MAX_READ_IMAGE_ARGS);
    }

    /**
     * Returns the max number of simultaneous image objects that can be written by a kernel.
     * The minimum value is 8 if image support is available.
     */
    public int getMaxWriteImageArgs() {
        return (int)getInfoLong(CL.CL_DEVICE_MAX_WRITE_IMAGE_ARGS);
    }

    /**
     * Returns the max width of 2D image in pixels. The minimum value is 8192 if
     * image support is available.
     */
    public int getMaxImage2dWidth() {
        return (int)getInfoLong(CL.CL_DEVICE_IMAGE2D_MAX_WIDTH);
    }

    /**
     * Returns the max height of 2D image in pixels. The minimum value is 8192 if
     * image support is available.
     */
    public int getMaxImage2dHeight() {
        return (int)getInfoLong(CL.CL_DEVICE_IMAGE2D_MAX_HEIGHT);
    }

    /**
     * Returns the max width of 3D image in pixels. The minimum value is 2048 if
     * image support is available.
     */
    public int getMaxImage3dWidth() {
        return (int)getInfoLong(CL.CL_DEVICE_IMAGE3D_MAX_WIDTH);
    }

    /**
     * Returns the max height of 3D image in pixels. The minimum value is 2048 if
     * image support is available.
     */
    public int getMaxImage3dHeight() {
        return (int)getInfoLong(CL.CL_DEVICE_IMAGE3D_MAX_HEIGHT);
    }

    /**
     * Returns the max depth of 3D image in pixels. The minimum value is 2048 if
     * image support is available.
     */
    public int getMaxImage3dDepth() {
        return (int)getInfoLong(CL.CL_DEVICE_IMAGE3D_MAX_DEPTH);
    }

    /**
     * Returns the maximum number of samplers that can be used in a kernel. The
     * minimum value is 16 if image support is available.
     */
    public int getMaxSamplers() {
        return (int)getInfoLong(CL.CL_DEVICE_MAX_SAMPLERS);
    }

    /**
     * Returns the resolution of device timer. This is measured in nanoseconds.
     */
    public long getProfilingTimerResolution() {
        return getInfoLong(CL.CL_DEVICE_PROFILING_TIMER_RESOLUTION);
    }

    /**
     * Returns the single precision floating-point capability of the device.
     */
    public EnumSet<SingleFPConfig> getSingleFPConfig() {
        return SingleFPConfig.valuesOf((int)getInfoLong(CL.CL_DEVICE_SINGLE_FP_CONFIG));
    }

    /**
     * Returns the local memory type.
     */
    public LocalMemType getLocalMemType() {
        return LocalMemType.valueOf((int)getInfoLong(CL.CL_DEVICE_LOCAL_MEM_TYPE));
    }

    /**
     * Returns the type of global memory cache supported.
     */
    public GlobalMemCacheType getGlobalMemCacheType() {
        return GlobalMemCacheType.valueOf((int)getInfoLong(CL.CL_DEVICE_GLOBAL_MEM_CACHE_TYPE));
    }

    /**
     * Returns the command-queue properties properties supported by the device.
     */
    public EnumSet<CLCommandQueue.Mode> getQueueProperties() {
        return CLCommandQueue.Mode.valuesOf((int)getInfoLong(CL.CL_DEVICE_QUEUE_PROPERTIES));
    }

    /**
     * Returns true if this device is available.
     */
    public boolean isAvailable() {
        return getInfoLong(CL.CL_DEVICE_AVAILABLE) == CL.CL_TRUE;
    }

    /**
     * Returns false if the implementation does not have a compiler available to
     * compile the program source. Is true if the compiler is available.
     * This can be false for the OpenCL ES profile only.
     */
    public boolean isCompilerAvailable() {
        return getInfoLong(CL.CL_DEVICE_COMPILER_AVAILABLE) == CL.CL_TRUE;
    }

    /**
     * Returns true if the OpenCL device is a little endian device and false otherwise.
     */
    public boolean isLittleEndianAvailable() {
        return getInfoLong(CL.CL_DEVICE_ENDIAN_LITTLE) == CL.CL_TRUE;
    }

    /**
     * Returns true if the device implements error correction for the memories,
     * caches, registers etc. in the device. Is false if the device does not
     * implement error correction.
     */
    public boolean isErrorCorrectionSupported() {
        return getInfoLong(CL.CL_DEVICE_ERROR_CORRECTION_SUPPORT) == CL.CL_TRUE;
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

    private final long getInfoLong(int key) {

        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.order(ByteOrder.nativeOrder());

        int ret = cl.clGetDeviceInfo(ID, key, bb.capacity(), bb, null, 0);

        checkForError(ret, "can not receive device info");

        return bb.getLong();
    }

    public final String getInfoString(int key) {

        long[] longBuffer = new long[1];
        ByteBuffer bb = ByteBuffer.allocate(512); // TODO use a cache

        int ret = cl.clGetDeviceInfo(ID, key, bb.capacity(), bb, longBuffer, 0);
        
        checkForError(ret, "can not receive device info string");

        return CLUtils.clString2JavaString(bb.array(), (int)longBuffer[0]);
        
    }


    @Override
    public String toString() {
        return "CLDevice [id: " + ID
                      + " name: " + getName()
                      + " type: " + getType()
                      + " profile: " + getProfile()+"]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CLDevice other = (CLDevice) obj;
        if (this.ID != other.ID) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + (int) (this.ID ^ (this.ID >>> 32));
        return hash;
    }

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
         * CL_DEVICE_TYPE_DEFAULT. This type can be used for creating a context on
         * the default device, a single device can never have this type.
         */
        DEFAULT(CL.CL_DEVICE_TYPE_DEFAULT),
        /**
         * CL_DEVICE_TYPE_ALL. This type can be used for creating a context on
         * all devices, a single device can never have this type.
         */
        ALL(CL.CL_DEVICE_TYPE_ALL);

        /**
         * Value of wrapped OpenCL device type.
         */
        public final long CL_TYPE;

        private Type(long CL_TYPE) {
            this.CL_TYPE = CL_TYPE;
        }

        public static Type valueOf(long clDeviceType) {

            if(clDeviceType == CL.CL_DEVICE_TYPE_ALL)
                return ALL;

            switch((int)clDeviceType) {
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

    /**
     * Describes single precision floating-point capability of the device.
     * One or more values are possible.
     */
    public enum SingleFPConfig {

        /**
         * denorms are supported.
         */
        DENORM(CL.CL_FP_DENORM),

        /**
         * INF and quiet NaNs are supported.
         */
        INF_NAN(CL.CL_FP_INF_NAN),

        /**
         * round to nearest rounding mode supported.
         */
        ROUND_TO_NEAREST(CL.CL_FP_ROUND_TO_NEAREST),

        /**
         * round to +ve and –ve infinity rounding modes supported.
         */
        ROUND_TO_INF(CL.CL_FP_ROUND_TO_INF),

        /**
         * round to zero rounding mode supported.
         */
        ROUND_TO_ZERO(CL.CL_FP_ROUND_TO_ZERO),

        /**
         * IEEE754-2008 fused multiply-add is supported.
         */
        FMA(CL.CL_FP_FMA);


        /**
         * Value of wrapped OpenCL bitfield.
         */
        public final int CL_VALUE;

        private SingleFPConfig(int CL_VALUE) {
            this.CL_VALUE = CL_VALUE;
        }

        /**
         * Returns a EnumSet for the given bitfield.
         */
        public static EnumSet<SingleFPConfig> valuesOf(int bitfield) {
            List<SingleFPConfig> matching = new ArrayList<SingleFPConfig>();
            SingleFPConfig[] values = SingleFPConfig.values();
            for (SingleFPConfig value : values) {
                if((value.CL_VALUE & bitfield) != 0)
                    matching.add(value);
            }
            if(matching.isEmpty())
                return EnumSet.noneOf(SingleFPConfig.class);
            else
                return EnumSet.copyOf(matching);
        }

    }

    /**
     * Type of global memory cache supported.
     */
    public enum GlobalMemCacheType {

        /**
         * Global memory cache not supported.
         */
        NONE(CL.CL_NONE),

        /**
         * Read only cache.
         */
        READ_ONLY(CL.CL_READ_ONLY_CACHE),

        /**
         * Read-write cache.
         */
        READ_WRITE(CL.CL_READ_WRITE_CACHE);
        

        /**
         * Value of wrapped OpenCL value.
         */
        public final int CL_VALUE;

        private GlobalMemCacheType(int CL_VALUE) {
            this.CL_VALUE = CL_VALUE;
        }

        /**
         * Returns the matching GlobalMemCacheType for the given cl type.
         */
        public static GlobalMemCacheType valueOf(int bitfield) {
            GlobalMemCacheType[] values = GlobalMemCacheType.values();
            for (GlobalMemCacheType value : values) {
                if(value.CL_VALUE == bitfield)
                    return value;
            }
            return null;
        }
    }

    /**
     * Type of local memory cache supported.
     */
    public enum LocalMemType {

        /**
         * GLOBAL implies that no dedicated memory storage is available (global mem is used instead).
         */
        GLOBAL(CL.CL_GLOBAL),

        /**
         * LOCAL implies dedicated local memory storage such as SRAM.
         */
        LOCAL(CL.CL_LOCAL);

        /**
         * Value of wrapped OpenCL value.
         */
        public final int CL_VALUE;

        private LocalMemType(int CL_VALUE) {
            this.CL_VALUE = CL_VALUE;
        }

        /**
         * Returns the matching LocalMemCacheType for the given cl type.
         */
        public static LocalMemType valueOf(int clLocalCacheType) {
            if(clLocalCacheType == CL.CL_GLOBAL)
                return LOCAL;
            else if(clLocalCacheType == CL.CL_LOCAL)
                return GLOBAL;
            return null;
        }

    }

}
