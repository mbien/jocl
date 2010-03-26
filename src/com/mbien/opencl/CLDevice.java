package com.mbien.opencl;

import com.mbien.opencl.util.CLUtil;
import com.sun.gluegen.runtime.CPU;
import com.sun.gluegen.runtime.PointerBuffer;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import static com.mbien.opencl.CL.*;

/**
 * This object represents an OpenCL device.
 * @see CLPlatform#listCLDevices(com.mbien.opencl.CLDevice.Type...)
 * @see CLPlatform#getMaxFlopsDevice(com.mbien.opencl.CLDevice.Type...)
 * @see CLContext#getDevices()
 * @see CLContext#getMaxFlopsDevice(com.mbien.opencl.CLDevice.Type)
 * @author Michael Bien
 */
public final class CLDevice extends CLObject {

    private Set<String> extensions;
    
    private final CLDeviceInfoAccessor deviceInfo;

    CLDevice(CL cl, long id) {
        super(cl, id);
        this.deviceInfo = new CLDeviceInfoAccessor();
    }

    CLDevice(CLContext context, long id) {
        super(context, id);
        this.deviceInfo = new CLDeviceInfoAccessor();
    }

    public CLCommandQueue createCommandQueue() {
        return createCommandQueue(0);
    }

    public CLCommandQueue createCommandQueue(CLCommandQueue.Mode property) {
        return createCommandQueue(property.QUEUE_MODE);
    }

    public CLCommandQueue createCommandQueue(CLCommandQueue.Mode... properties) {
        int flags = 0;
        if(properties != null) {
            for (int i = 0; i < properties.length; i++) {
                flags |= properties[i].QUEUE_MODE;
            }
        }
        return createCommandQueue(flags);
    }
    
    public CLCommandQueue createCommandQueue(long properties) {
        if(context == null)
            throw new IllegalStateException("this device is not associated with a context");
        return context.createCommandQueue(this, properties);
    }
    
    /*keep this package private*/
    void setContext(CLContext context) {
        this.context = context;
    }

    /**
     * Returns the name of this device.
     */
    public String getName() {
        return deviceInfo.getString(CL_DEVICE_NAME);
    }

    /**
     * Returns the OpenCL profile of this device.
     */
    public String getProfile() {
        return deviceInfo.getString(CL_DEVICE_PROFILE);
    }

    /**
     * Returns the vendor of this device.
     */
    public String getVendor() {
        return deviceInfo.getString(CL_DEVICE_VENDOR);
    }

    /**
     * Returns the vendor id of this device.
     */
    public long getVendorID() {
        return deviceInfo.getLong(CL_DEVICE_VENDOR_ID);
    }

    /**
     * Returns OpenCL version string. Returns the OpenCL version supported by the device.
     * This version string has the following format:<br>
     * OpenCL[space][major_version.minor_version][space][vendor-specific information]
     */
    public String getVersion() {
        return deviceInfo.getString(CL_DEVICE_VERSION);
    }

    /**
     * Returns OpenCL software driver version string in the form major_number.minor_number.
     */
    public String getDriverVersion() {
        return deviceInfo.getString(CL_DRIVER_VERSION);
    }

    /**
     * Returns the type of this device.
     */
    public Type getType() {
        return Type.valueOf((int)deviceInfo.getLong(CL_DEVICE_TYPE));
    }

    /**
     * The default compute device address space size specified in bits.
     * Currently supported values are 32 or 64 bits.
     */
    public int getAddressBits() {
        return (int)deviceInfo.getLong(CL_DEVICE_ADDRESS_BITS);
    }

    /**
     * Preferred native vector width size for built-in short vectors.
     * The vector width is defined as the number of scalar elements that can be stored in the vector.
     */
    public int getPreferredShortVectorWidth() {
        return (int)deviceInfo.getLong(CL_DEVICE_PREFERRED_VECTOR_WIDTH_SHORT);
    }

    /**
     * Preferred native vector width size for built-in char vectors.
     * The vector width is defined as the number of scalar elements that can be stored in the vector.
     */
    public int getPreferredCharVectorWidth() {
        return (int)deviceInfo.getLong(CL_DEVICE_PREFERRED_VECTOR_WIDTH_CHAR);
    }

    /**
     * Preferred native vector width size for built-in int vectors.
     * The vector width is defined as the number of scalar elements that can be stored in the vector.
     */
    public int getPreferredIntVectorWidth() {
        return (int)deviceInfo.getLong(CL_DEVICE_PREFERRED_VECTOR_WIDTH_INT);
    }

    /**
     * Preferred native vector width size for built-in long vectors.
     * The vector width is defined as the number of scalar elements that can be stored in the vector.
     */
    public int getPreferredLongVectorWidth() {
        return (int)deviceInfo.getLong(CL_DEVICE_PREFERRED_VECTOR_WIDTH_LONG);
    }

    /**
     * Preferred native vector width size for built-in float vectors.
     * The vector width is defined as the number of scalar elements that can be stored in the vector.
     */
    public int getPreferredFloatVectorWidth() {
        return (int)deviceInfo.getLong(CL_DEVICE_PREFERRED_VECTOR_WIDTH_FLOAT);
    }

    /**
     * Preferred native vector width size for built-in double vectors.
     * The vector width is defined as the number of scalar elements that can be stored in the vector.
     */
    public int getPreferredDoubleVectorWidth() {
        return (int)deviceInfo.getLong(CL_DEVICE_PREFERRED_VECTOR_WIDTH_DOUBLE);
    }

    /**
     * Returns the number of parallel compute cores on the OpenCL device.
     * The minimum value is 1.
     */
    public int getMaxComputeUnits() {
        return (int) deviceInfo.getLong(CL_DEVICE_MAX_COMPUTE_UNITS);
    }

    /**
     * Returns the maximum number of work-items in a work-group executing
     * a kernel using the data parallel execution model.
     * The minimum value is 1.
     */
    public int getMaxWorkGroupSize() {
        return (int) deviceInfo.getLong(CL_DEVICE_MAX_WORK_GROUP_SIZE);
    }

    /**
     * Returns the maximum configured clock frequency of the device in MHz.
     */
    public int getMaxClockFrequency() {
        return (int) (deviceInfo.getLong(CL_DEVICE_MAX_CLOCK_FREQUENCY));
    }

    /**
     * Returns the maximum dimensions that specify the global and local work-item
     * IDs used by the data parallel execution model.
     * The minimum value is 3.
     */
    public int getMaxWorkItemDimensions() {
        return (int) deviceInfo.getLong(CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS);
    }

    /**
     * Returns the maximum number of work-items that can be specified in each
     * dimension of the work-group.
     * The minimum value is (1, 1, 1).
     */
    public int[] getMaxWorkItemSizes() {
        int n = (int) deviceInfo.getLong(CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS);
        return deviceInfo.getInts(n, CL_DEVICE_MAX_WORK_ITEM_SIZES);
    }

    /**
     * Returns the max size in bytes of the arguments that can be passed to a kernel.
     * The minimum value is 256.
     */
    public long getMaxParameterSize() {
        return deviceInfo.getLong(CL_DEVICE_MAX_PARAMETER_SIZE);
    }

    /**
     * Returns the maximal allocatable memory on this device.
     */
    public long getMaxMemAllocSize() {
        return deviceInfo.getLong(CL_DEVICE_MAX_MEM_ALLOC_SIZE);
    }

    /**
     * Returns the global memory size in bytes.
     */
    public long getGlobalMemSize() {
        return deviceInfo.getLong(CL_DEVICE_GLOBAL_MEM_SIZE);
    }

    /**
     * Returns the local memory size in bytes.
     */
    public long getLocalMemSize() {
        return deviceInfo.getLong(CL_DEVICE_LOCAL_MEM_SIZE);
    }

    /**
     * Returns the max size in bytes of a constant buffer allocation.
     * The minimum value is 64 KB.
     */
    public long getMaxConstantBufferSize() {
        return deviceInfo.getLong(CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE);
    }

    /**
     * Returns the size of global memory cache line in bytes.
     */
    public long getGlobalMemCachelineSize() {
        return deviceInfo.getLong(CL_DEVICE_GLOBAL_MEM_CACHELINE_SIZE);
    }

    /**
     * Returns the size of global memory cache in bytes.
     */
    public long getGlobalMemCacheSize() {
        return deviceInfo.getLong(CL_DEVICE_GLOBAL_MEM_CACHE_SIZE);
    }

    /**
     * Returns the max number of arguments declared with the <code>constant</code>
     * qualifier in a kernel. The minimum value is 8.
     */
    public long getMaxConstantArgs() {
        return deviceInfo.getLong(CL_DEVICE_MAX_CONSTANT_ARGS);
    }

    /**
     * Returns true if images are supported by the OpenCL device and false otherwise.
     */
    public boolean isImageSupportAvailable() {
        return deviceInfo.getLong(CL_DEVICE_IMAGE_SUPPORT) == CL_TRUE;
    }

    /**
     * Returns the max number of simultaneous image objects that can be read by a kernel.
     * The minimum value is 128 if image support is available.
     */
    public int getMaxReadImageArgs() {
        return (int)deviceInfo.getLong(CL_DEVICE_MAX_READ_IMAGE_ARGS);
    }

    /**
     * Returns the max number of simultaneous image objects that can be written by a kernel.
     * The minimum value is 8 if image support is available.
     */
    public int getMaxWriteImageArgs() {
        return (int)deviceInfo.getLong(CL_DEVICE_MAX_WRITE_IMAGE_ARGS);
    }

    /**
     * Returns the max width of 2D image in pixels. The minimum value is 8192 if
     * image support is available.
     */
    public int getMaxImage2dWidth() {
        return (int)deviceInfo.getLong(CL_DEVICE_IMAGE2D_MAX_WIDTH);
    }

    /**
     * Returns the max height of 2D image in pixels. The minimum value is 8192 if
     * image support is available.
     */
    public int getMaxImage2dHeight() {
        return (int)deviceInfo.getLong(CL_DEVICE_IMAGE2D_MAX_HEIGHT);
    }

    /**
     * Returns the max width of 3D image in pixels. The minimum value is 2048 if
     * image support is available.
     */
    public int getMaxImage3dWidth() {
        return (int)deviceInfo.getLong(CL_DEVICE_IMAGE3D_MAX_WIDTH);
    }

    /**
     * Returns the max height of 3D image in pixels. The minimum value is 2048 if
     * image support is available.
     */
    public int getMaxImage3dHeight() {
        return (int)deviceInfo.getLong(CL_DEVICE_IMAGE3D_MAX_HEIGHT);
    }

    /**
     * Returns the max depth of 3D image in pixels. The minimum value is 2048 if
     * image support is available.
     */
    public int getMaxImage3dDepth() {
        return (int)deviceInfo.getLong(CL_DEVICE_IMAGE3D_MAX_DEPTH);
    }

    /**
     * Returns the maximum number of samplers that can be used in a kernel. The
     * minimum value is 16 if image support is available.
     */
    public int getMaxSamplers() {
        return (int)deviceInfo.getLong(CL_DEVICE_MAX_SAMPLERS);
    }

    /**
     * Returns the resolution of device timer. This is measured in nanoseconds.
     */
    public long getProfilingTimerResolution() {
        return deviceInfo.getLong(CL_DEVICE_PROFILING_TIMER_RESOLUTION);
    }

    /**
     * Returns the execution capabilities as EnumSet.
     */
    public EnumSet<Capabilities> getExecutionCapabilities() {
        return Capabilities.valuesOf((int)deviceInfo.getLong(CL_DEVICE_EXECUTION_CAPABILITIES));
    }

    /**
     * Returns the optional half precision floating-point capability of the device.
     * The required minimum half precision floating-point capabilities as implemented by this
     * extension are {@link FPConfig#ROUND_TO_ZERO}, {@link FPConfig#ROUND_TO_INF}
     * and {@link FPConfig#INF_NAN}.
     * @return An EnumSet containing the extensions, never null.
     */
    public EnumSet<FPConfig> getHalfFPConfig() {
        if(isHalfFPAvailable())
            return FPConfig.valuesOf((int)deviceInfo.getLong(CL_DEVICE_HALF_FP_CONFIG));
        else
            return EnumSet.noneOf(FPConfig.class);
    }

    /**
     * Returns the single precision floating-point capability of the device.
     * The mandated minimum floating-point capabilities are {@link FPConfig#ROUND_TO_NEAREST} and
     * {@link FPConfig#INF_NAN}.
     * @return An EnumSet containing the extensions, never null.
     */
    public EnumSet<FPConfig> getSingleFPConfig() {
        return FPConfig.valuesOf((int)deviceInfo.getLong(CL_DEVICE_SINGLE_FP_CONFIG));
    }

    /**
     * Returns the optional double precision floating-point capability of the device.
     * The mandated minimum double precision floating-point capabilities are {@link FPConfig#FMA},
     * {@link FPConfig#ROUND_TO_NEAREST}, {@link FPConfig#ROUND_TO_ZERO},
     * {@link FPConfig#ROUND_TO_INF}, {@link FPConfig#INF_NAN}, and {@link FPConfig#DENORM}.
     * @return An EnumSet containing the extensions, never null.
     */
    public EnumSet<FPConfig> getDoubleFPConfig() {
        if(isDoubleFPAvailable())
            return FPConfig.valuesOf((int)deviceInfo.getLong(CL_DEVICE_DOUBLE_FP_CONFIG));
        else
            return EnumSet.noneOf(FPConfig.class);
    }

    /**
     * Returns the local memory type.
     */
    public LocalMemType getLocalMemType() {
        return LocalMemType.valueOf((int)deviceInfo.getLong(CL_DEVICE_LOCAL_MEM_TYPE));
    }

    /**
     * Returns the type of global memory cache supported.
     */
    public GlobalMemCacheType getGlobalMemCacheType() {
        return GlobalMemCacheType.valueOf((int)deviceInfo.getLong(CL_DEVICE_GLOBAL_MEM_CACHE_TYPE));
    }

    /**
     * Returns the command-queue properties supported by the device.
     */
    public EnumSet<CLCommandQueue.Mode> getQueueProperties() {
        return CLCommandQueue.Mode.valuesOf((int)deviceInfo.getLong(CL_DEVICE_QUEUE_PROPERTIES));
    }

    /**
     * Returns true if this device is available.
     */
    public boolean isAvailable() {
        return deviceInfo.getLong(CL_DEVICE_AVAILABLE) == CL_TRUE;
    }

    /**
     * Returns false if the implementation does not have a compiler available to
     * compile the program source. Is true if the compiler is available.
     * This can be false for the OpenCL ES profile only.
     */
    public boolean isCompilerAvailable() {
        return deviceInfo.getLong(CL_DEVICE_COMPILER_AVAILABLE) == CL_TRUE;
    }

    /**
     * Returns true if the OpenCL device is a little endian device and false otherwise.
     */
    public boolean isLittleEndian() {
        return deviceInfo.getLong(CL_DEVICE_ENDIAN_LITTLE) == CL_TRUE;
    }

    /**
     * Returns true if the device implements error correction for the memories,
     * caches, registers etc. in the device. Is false if the device does not
     * implement error correction.
     */
    public boolean isErrorCorrectionSupported() {
        return deviceInfo.getLong(CL_DEVICE_ERROR_CORRECTION_SUPPORT) == CL_TRUE;
    }

    /**
     * Returns {@link #isExtensionAvailable}("cl_khr_fp16").
     * @see #getExtensions()
     */
    public boolean isHalfFPAvailable() {
        return isExtensionAvailable("cl_khr_fp16");
    }

    /**
     * Returns {@link #isExtensionAvailable}("cl_khr_fp64").
     * @see #getExtensions()
     */
    public boolean isDoubleFPAvailable() {
        return isExtensionAvailable("cl_khr_fp64");
    }

    /**
     * Returns {@link #isExtensionAvailable}("cl_khr_gl_sharing") || {@link #isExtensionAvailable}("cl_apple_gl_sharing").
     * @see #getExtensions()
     */
    public boolean isGLMemorySharingSupported() {
        return isExtensionAvailable("cl_khr_gl_sharing") || isExtensionAvailable("cl_apple_gl_sharing");
    }

    /**
     * Returns true if the extension is supported on this device.
     * @see #getExtensions()
     */
    public boolean isExtensionAvailable(String extension) {
        return getExtensions().contains(extension);
    }

    /**
     * Returns all device extension names as unmodifiable Set.
     */
    public Set<String> getExtensions() {

        if(extensions == null) {
            extensions = new HashSet<String>();
            String ext = deviceInfo.getString(CL_DEVICE_EXTENSIONS);
            Scanner scanner = new Scanner(ext);

            while(scanner.hasNext())
                extensions.add(scanner.next());

            extensions = Collections.unmodifiableSet(extensions);
        }

        return extensions;
    }

    /**
     * Returns a Map of device properties with the enum names as keys.
     * @see CLUtil#obtainDeviceProperties(com.mbien.opencl.CLDevice)
     */
    public Map<String, String> getProperties() {
        return CLUtil.obtainDeviceProperties(this);
    }

    private final class CLDeviceInfoAccessor extends CLInfoAccessor {

        @Override
        protected int getInfo(int name, long valueSize, Buffer value, PointerBuffer valueSizeRet) {
            return cl.clGetDeviceInfo(ID, name, valueSize, value, valueSizeRet);
        }

        private int[] getInts(int n, int key) {

            ByteBuffer buffer = localBB.get();
            int ret = getInfo(key, buffer.capacity(), buffer, null);
            CLException.checkForError(ret, "error while asking device for infos");

            int[] array = new int[n];
            for(int i = 0; i < array.length; i++) {
                if(CPU.is32Bit()) {
                    array[i] = buffer.getInt();
                }else{
                    array[i] = (int)buffer.getLong();
                }
            }
            buffer.rewind();
            
            return array;
        }

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
     * Enumeration for the execution capabilities of the device.
     */
    public enum Capabilities {

        /**
         * The OpenCL device can execute OpenCL kernels.
         */
        EXEC_KERNEL(CL_EXEC_KERNEL),

        /**
         * The OpenCL device can execute native kernels.
         */
        EXEC_NATIVE_KERNEL(CL_EXEC_NATIVE_KERNEL);

        /**
         * Value of wrapped OpenCL device type.
         */
        public final int CAPS;

        private Capabilities(int type) {
            this.CAPS = type;
        }

        public static Capabilities valueOf(int caps) {
            switch(caps) {
                case(CL_EXEC_KERNEL):
                    return EXEC_KERNEL;
                case(CL_EXEC_NATIVE_KERNEL):
                    return EXEC_NATIVE_KERNEL;
            }
            return null;
        }

        public static EnumSet<Capabilities> valuesOf(int bitfield) {
            if((EXEC_KERNEL.CAPS & bitfield) != 0) {
                if((EXEC_NATIVE_KERNEL.CAPS & bitfield) != 0) {
                    return EnumSet.of(EXEC_KERNEL, EXEC_NATIVE_KERNEL);
                }else{
                    return EnumSet.of(EXEC_KERNEL);
                }
            }else if((EXEC_NATIVE_KERNEL.CAPS & bitfield) != 0){
                return EnumSet.of(EXEC_NATIVE_KERNEL);
            }
            return null;
        }

    }

    /**
     * Enumeration for the type of a device.
     */
    public enum Type {
        /**
         * CL_DEVICE_TYPE_CPU
         */
        CPU(CL_DEVICE_TYPE_CPU),
        /**
         * CL_DEVICE_TYPE_GPU
         */
        GPU(CL_DEVICE_TYPE_GPU),
        /**
         * CL_DEVICE_TYPE_ACCELERATOR
         */
        ACCELERATOR(CL_DEVICE_TYPE_ACCELERATOR),
        /**
         * CL_DEVICE_TYPE_DEFAULT. This type can be used for creating a context on
         * the default device, a single device can never have this type.
         */
        DEFAULT(CL_DEVICE_TYPE_DEFAULT),
        /**
         * CL_DEVICE_TYPE_ALL. This type can be used for creating a context on
         * all devices, a single device can never have this type.
         */
        ALL(CL_DEVICE_TYPE_ALL);

        /**
         * Value of wrapped OpenCL device type.
         */
        public final long TYPE;

        private Type(long type) {
            this.TYPE = type;
        }

        public static Type valueOf(long clDeviceType) {

            if(clDeviceType == CL_DEVICE_TYPE_ALL)
                return ALL;

            switch((int)clDeviceType) {
                case(CL_DEVICE_TYPE_DEFAULT):
                    return DEFAULT;
                case(CL_DEVICE_TYPE_CPU):
                    return CPU;
                case(CL_DEVICE_TYPE_GPU):
                    return GPU;
                case(CL_DEVICE_TYPE_ACCELERATOR):
                    return ACCELERATOR;
            }
            return null;
        }
    }

    /**
     * Describes floating-point capability of the device.
     * Zero or more values are possible.
     */
    public enum FPConfig {

        /**
         * denorms are supported.
         */
        DENORM(CL_FP_DENORM),

        /**
         * INF and quiet NaNs are supported.
         */
        INF_NAN(CL_FP_INF_NAN),

        /**
         * round to nearest rounding mode supported.
         */
        ROUND_TO_NEAREST(CL_FP_ROUND_TO_NEAREST),

        /**
         * round to +ve and –ve infinity rounding modes supported.
         */
        ROUND_TO_INF(CL_FP_ROUND_TO_INF),

        /**
         * round to zero rounding mode supported.
         */
        ROUND_TO_ZERO(CL_FP_ROUND_TO_ZERO),

        /**
         * IEEE754-2008 fused multiply-add is supported.
         */
        FMA(CL_FP_FMA);


        /**
         * Value of wrapped OpenCL bitfield.
         */
        public final int CONFIG;

        private FPConfig(int config) {
            this.CONFIG = config;
        }

        /**
         * Returns a EnumSet for the given bitfield.
         */
        public static EnumSet<FPConfig> valuesOf(int bitfield) {
            List<FPConfig> matching = new ArrayList<FPConfig>();
            FPConfig[] values = FPConfig.values();
            for (FPConfig value : values) {
                if((value.CONFIG & bitfield) != 0)
                    matching.add(value);
            }
            if(matching.isEmpty())
                return EnumSet.noneOf(FPConfig.class);
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
        NONE(CL_NONE),

        /**
         * Read only cache.
         */
        READ_ONLY(CL_READ_ONLY_CACHE),

        /**
         * Read-write cache.
         */
        READ_WRITE(CL_READ_WRITE_CACHE);
        

        /**
         * Value of wrapped OpenCL value.
         */
        public final int TYPE;

        private GlobalMemCacheType(int type) {
            this.TYPE = type;
        }

        /**
         * Returns the matching GlobalMemCacheType for the given cl type.
         */
        public static GlobalMemCacheType valueOf(int bitfield) {
            GlobalMemCacheType[] values = GlobalMemCacheType.values();
            for (GlobalMemCacheType value : values) {
                if(value.TYPE == bitfield)
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
        GLOBAL(CL_GLOBAL),

        /**
         * LOCAL implies dedicated local memory storage such as SRAM.
         */
        LOCAL(CL_LOCAL);

        /**
         * Value of wrapped OpenCL value.
         */
        public final int TYPE;

        private LocalMemType(int type) {
            this.TYPE = type;
        }

        /**
         * Returns the matching LocalMemCacheType for the given cl type.
         */
        public static LocalMemType valueOf(int clLocalCacheType) {
            if(clLocalCacheType == CL_GLOBAL)
                return GLOBAL;
            else if(clLocalCacheType == CL_LOCAL)
                return LOCAL;
            return null;
        }

    }

}
