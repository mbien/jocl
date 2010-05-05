/* NVIDIA */
#define CL_DEVICE_COMPUTE_CAPABILITY_MAJOR_NV       0x4000
#define CL_DEVICE_COMPUTE_CAPABILITY_MINOR_NV       0x4001
#define CL_DEVICE_REGISTERS_PER_BLOCK_NV            0x4002
#define CL_DEVICE_WARP_SIZE_NV                      0x4003
#define CL_DEVICE_GPU_OVERLAP_NV                    0x4004
#define CL_DEVICE_KERNEL_EXEC_TIMEOUT_NV            0x4005
#define CL_DEVICE_INTEGRATED_MEMORY_NV              0x4006

/* AMD */
//temporary: pasted from AMD's cl_ext.h, TODO remove when included in khronos headers
/******************************************************************************/
/* AMD Device attribute query extension */

#define cl_amd_device_attribute_query 1

#define CL_DEVICE_PROFILING_TIMER_OFFSET_AMD        0x403F

/******************************************************************************/
/* Device Fission Extension */

#define cl_ext_device_fission 1

/******************************************************************************/

typedef cl_uint cl_device_partition_property_ext;

/******************************************************************************/

/* Error Codes */
#define CL_INVALID_PROPERTY_EXT                     -1018
#define CL_DEVICE_PARTITION_FAILED_EXT              -1019
#define CL_INVALID_PARTITION_COUNT_EXT              -1020

/* cl_device_info */
#define CL_DEVICE_PARENT_DEVICE_EXT                 0x4030
#define CL_DEVICE_PARTITION_STYLE_EXT               0x4031

/* cl_device_partition_property_ext */
#define CL_DEVICE_PARTITION_EQUALLY_EXT             0x4032
#define CL_DEVICE_PARTITION_BY_COUNTS_EXT           0x4033
#define CL_DEVICE_PARTITION_BY_AFFINITY_DOMAIN_EXT  0x4034

/* cl_affinity_domain_ext */
#define CL_AFFINITY_DOMAIN_NUMA_EXT                 0x1
#define CL_AFFINITY_DOMAIN_L4_CACHE_EXT             0x2
#define CL_AFFINITY_DOMAIN_L3_CACHE_EXT             0x3
#define CL_AFFINITY_DOMAIN_L2_CACHE_EXT             0x4
#define CL_AFFINITY_DOMAIN_L1_CACHE_EXT             0x5
#define CL_AFFINITY_DOMAIN_NEXT_FISSIONABLE_EXT     0x6

/* Device APIs */
typedef CL_API_ENTRY cl_int (CL_API_CALL * clCreateSubDevicesEXT_fn)(
    cl_device_id     /* in_device */,
    const cl_device_partition_property_ext * /* partition_properties */,
    cl_uint          /* num_entries */,
    cl_device_id *   /* out_devices */,
    cl_uint *        /* num_devices */);

typedef CL_API_ENTRY cl_int (CL_API_CALL * clRetainDeviceEXT_fn)(
    cl_device_id     /* device */) CL_API_SUFFIX__VERSION_1_0;

typedef CL_API_ENTRY cl_int (CL_API_CALL * clReleaseDeviceEXT_fn)(
    cl_device_id     /* device */) CL_API_SUFFIX__VERSION_1_0;
