package com.mbien.opencl;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Michael Bien
 */
class CLUtils {

    public static String clString2JavaString(byte[] chars, int clLength) {
        return clLength==0 ? "" : new String(chars, 0, clLength-1);
    }

    public static String clString2JavaString(ByteBuffer chars, int clLength) {
        if (clLength==0) {
            return "";
        }else{
            byte[] array = new byte[clLength-1]; // last char is always null
            chars.get(array).rewind();
            return new String(array, 0, clLength-1);
        }
    }

    public static Map<String, String> obtainPlatformProperties(CLPlatform platform) {

        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("CL_PLATFORM_NAME",     platform.getName());
        map.put("CL_PLATFORM_PROFILE",  platform.getProfile());
        map.put("CL_PLATFORM_VERSION",  platform.getVersion());
        map.put("CL_PLATFORM_VENDOR",   platform.getVendor());
        map.put("CL_PLATFORM_EXTENSIONS",   platform.getExtensions().toString());
//        map.put("fastest device (estimated)", platform.getMaxFlopsDevice().toString());

        return map;
    }

    public static Map<String, String> obtainDeviceProperties(CLDevice dev) {
        
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("CL_DEVICE_NAME",       dev.getName());
        map.put("CL_DEVICE_PROFILE",    dev.getProfile());
        map.put("CL_DEVICE_VENDOR",     dev.getVendor());
        map.put("CL_DEVICE_VENDOR_ID",  dev.getVendorID()+"");
        map.put("CL_DEVICE_VERSION",    dev.getVersion());
        map.put("CL_DRIVER_VERSION",    dev.getDriverVersion());
        map.put("CL_DEVICE_TYPE",       dev.getType().toString());

        map.put("CL_DEVICE_GLOBAL_MEM_SIZE",    dev.getGlobalMemSize()/(1024*1024)+" MB");
        map.put("CL_DEVICE_MAX_MEM_ALLOC_SIZE", dev.getMaxMemAllocSize()/(1024*1024)+" MB");
        map.put("CL_DEVICE_MAX_PARAMETER_SIZE", dev.getMaxParameterSize()+" Byte");
        map.put("CL_DEVICE_LOCAL_MEM_SIZE",     dev.getLocalMemSize()/1024+" KB");
        map.put("CL_DEVICE_LOCAL_MEM_TYPE",     dev.getLocalMemType()+"");
        map.put("CL_DEVICE_GLOBAL_MEM_CACHE_SIZE", dev.getGlobalMemCacheSize()+"");
        map.put("CL_DEVICE_GLOBAL_MEM_CACHELINE_SIZE", dev.getGlobalMemCachelineSize()+"");
        map.put("CL_DEVICE_GLOBAL_MEM_CACHE_TYPE", dev.getGlobalMemCacheType()+"");
        map.put("CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE", dev.getMaxConstantBufferSize()+"");
        map.put("CL_DEVICE_MAX_CONSTANT_ARGS",        dev.getMaxConstantArgs()+"");
        map.put("CL_DEVICE_ERROR_CORRECTION_SUPPORT", dev.isErrorCorrectionSupported()+"");

        map.put("CL_DEVICE_MAX_CLOCK_FREQUENCY",        dev.getMaxClockFrequency()+" MHz");
        map.put("CL_DEVICE_PROFILING_TIMER_RESOLUTION", dev.getProfilingTimerResolution()+" ns");
        map.put("CL_DEVICE_QUEUE_PROPERTIES",           dev.getQueueProperties()+"");
        map.put("CL_DEVICE_MAX_WORK_GROUP_SIZE",    dev.getMaxWorkGroupSize()+"");
        map.put("CL_DEVICE_MAX_COMPUTE_UNITS",      dev.getMaxComputeUnits()+"");
        map.put("CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS", dev.getMaxWorkItemDimensions()+"");
        map.put("CL_DEVICE_MAX_WORK_ITEM_SIZES",    Arrays.toString(dev.getMaxWorkItemSizes()));
        map.put("CL_DEVICE_COMPILER_AVAILABLE",     dev.isCompilerAvailable()+"");

        map.put("CL_DEVICE_IMAGE_SUPPORT",          dev.isImageSupportAvailable()+"");
        map.put("CL_DEVICE_MAX_READ_IMAGE_ARGS",    dev.getMaxReadImageArgs()+"");
        map.put("CL_DEVICE_MAX_WRITE_IMAGE_ARGS",   dev.getMaxWriteImageArgs()+"");
        map.put("CL_DEVICE_IMAGE2D_MAX_DIMENSIONS", Arrays.asList(dev.getMaxImage2dWidth(), dev.getMaxImage2dHeight()).toString());
        map.put("CL_DEVICE_IMAGE3D_MAX_DIMENSIONS", Arrays.asList(dev.getMaxImage2dWidth(), dev.getMaxImage2dHeight(), dev.getMaxImage3dDepth()).toString());
        map.put("CL_DEVICE_MAX_SAMPLERS",           dev.getMaxSamplers()+"");

        map.put("CL_DEVICE_ADDRESS_BITS",   dev.getAddressBits()+"");
        map.put("cl_khr_fp16",              dev.isHalfFPAvailable()+"");
        map.put("cl_khr_fp64",              dev.isDoubleFPAvailable()+"");
        map.put("CL_DEVICE_ENDIAN_LITTLE",  dev.isLittleEndianAvailable()+"");
        map.put("CL_DEVICE_HALF_FP_CONFIG", dev.getHalfFPConfig()+"");
        map.put("CL_DEVICE_SINGLE_FP_CONFIG", dev.getSingleFPConfig()+"");
        map.put("CL_DEVICE_DOUBLE_FP_CONFIG", dev.getDoubleFPConfig()+"");
        map.put("CL_DEVICE_EXTENSIONS",     dev.getExtensions()+"");

        map.put("CL_DEVICE_PREFERRED_VECTOR_WIDTH_SHORT",   dev.getPreferedShortVectorWidth()+"");
        map.put("CL_DEVICE_PREFERRED_VECTOR_WIDTH_CHAR",    dev.getPreferedCharVectorWidth()+"");
        map.put("CL_DEVICE_PREFERRED_VECTOR_WIDTH_INT",     dev.getPreferedIntVectorWidth()+"");
        map.put("CL_DEVICE_PREFERRED_VECTOR_WIDTH_LONG",    dev.getPreferedLongVectorWidth()+"");
        map.put("CL_DEVICE_PREFERRED_VECTOR_WIDTH_FLOAT",   dev.getPreferedFloatVectorWidth()+"");
        map.put("CL_DEVICE_PREFERRED_VECTOR_WIDTH_DOUBLE",  dev.getPreferedDoubleVectorWidth()+"");

        //TODO device extensions -> properties

        return map;
    }

}
