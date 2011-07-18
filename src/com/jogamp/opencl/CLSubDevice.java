/*
 * Created on Wednesday, July 13 2011 00:50
 */
package com.jogamp.opencl;

import com.jogamp.opencl.llb.CL;

import static com.jogamp.opencl.llb.CL.*;

/**
 * A subdevice created through device fission.
 * A subdevice can be used like any other device but must be released if no longer needed.
 * <p>
 * possible usecases for device fission:
 * <ul>
 * <li> To reserve part of the device for use for high priority / latency-sensitive tasks </li>
 * <li> To more directly control the assignment of work to individual compute units </li>
 * <li> To subdivide compute devices along some shared hardware feature like a cache </li>
 * </ul>
 * </p>
 * @see CLDevice#createSubDevicesEqually(int)
 * @see CLDevice#createSubDevicesByCount(int[])
 * @see CLDevice#createSubDeviceByIndex(int[])
 * @see CLDevice#createSubDevicesByDomain(com.jogamp.opencl.CLSubDevice.AffinityDomain)
 * @author Michael Bien
 */
public class CLSubDevice extends CLDevice implements CLResource {

    private volatile boolean released;

    private final CLDevice parent;

    private CLSubDevice(CLDevice parent, CLContext context, long id) {
        super(context, id);
        this.parent = parent;
    }

    private CLSubDevice(CLDevice parent, CLPlatform platform, long id) {
        super(platform, id);
        this.parent = parent;
    }

    static CLSubDevice createSubDevice(CLDevice device, long id) {
        if(device.context == null) {
            return new CLSubDevice(device, device.getPlatform(), id);
        }else{
            return new CLSubDevice(device, device.getContext(), id);
        }
    }

    /**
     * Returns the parent device which may be a CLDevice or another CLSubDevice.
     */
    public CLDevice getParent() {
        return parent;
    }

    @Override
    public void release() {
        if(released) {
            throw new RuntimeException("already released");
        }
        released = true;
        CL cl = CLPlatform.getLowLevelCLInterface();
        int ret = cl.clReleaseDeviceEXT(ID);
        CLException.checkForError(ret, "release failed");
    }

    @Override
    public boolean isReleased() {
        return released;
    }

    @Override
    public boolean isSubDevice() {
        return true;
    }


    /**
     * Sub device affinity domains.
     * @see CLDevice#createSubDevicesByDomain(com.jogamp.opencl.CLSubDevice.AffinityDomain) 
     */
    public enum AffinityDomain {

        L1_CACHE(CL_AFFINITY_DOMAIN_L1_CACHE_EXT),

        L2_CACHE(CL_AFFINITY_DOMAIN_L2_CACHE_EXT),

        L3_CACHE(CL_AFFINITY_DOMAIN_L3_CACHE_EXT),

        L4_CACHE(CL_AFFINITY_DOMAIN_L4_CACHE_EXT),

        NUMA(CL_AFFINITY_DOMAIN_NUMA_EXT),

        NEXT_FISSIONABLE(CL_AFFINITY_DOMAIN_NEXT_FISSIONABLE_EXT);

        /**
         * Value of wrapped OpenCL value.
         */
        public final int TYPE;

        private AffinityDomain(int type) {
            this.TYPE = type;
        }

        /**
         * Returns the matching AffinityDomain for the given cl flag.
         */
        public static AffinityDomain valueOf(int domain) {
            AffinityDomain[] values = AffinityDomain.values();
            for (AffinityDomain value : values) {
                if(value.TYPE == domain)
                    return value;
            }
            return null;
        }

    }

    /**
     * Sub device partition styles.
     * @see CLDevice#getPartitionTypes()
     */
    public enum Partition {

        /**
         * @see CLDevice#createSubDevicesEqually(int)
         */
        EQUALLY(CL_DEVICE_PARTITION_EQUALLY_EXT),

        /**
         * @see CLDevice#createSubDevicesByCount(int[])
         */
        COUNTS(CL_DEVICE_PARTITION_BY_COUNTS_EXT),

        /**
         * @see CLDevice#createSubDeviceByIndex(int[])
         */
        NAMES(CL_DEVICE_PARTITION_BY_NAMES_EXT),

        /**
         * @see CLDevice#createSubDevicesByDomain(com.jogamp.opencl.CLSubDevice.AffinityDomain)
         */
        DOMAIN(CL_DEVICE_PARTITION_BY_AFFINITY_DOMAIN_EXT);

        /**
         * Value of wrapped OpenCL value.
         */
        public final int FLAG;

        private Partition(int type) {
            this.FLAG = type;
        }

        /**
         * Returns the matching AffinityDomain for the given cl flag.
         */
        public static Partition valueOf(int domain) {
            Partition[] values = Partition.values();
            for (Partition value : values) {
                if(value.FLAG == domain)
                    return value;
            }
            return null;
        }

    }

}
