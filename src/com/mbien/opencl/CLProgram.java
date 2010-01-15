package com.mbien.opencl;

import com.sun.gluegen.runtime.CPU;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.mbien.opencl.CLException.*;
import static com.mbien.opencl.CL.*;

/**
 *
 * @author Michael Bien
 */
public class CLProgram implements CLResource {
    
    public final CLContext context;
    public final long ID;
    
    private final CL cl;

    private Map<String, CLKernel> kernels;
    private Map<CLDevice, Status> buildStatusMap;

    private boolean executable;

    CLProgram(CLContext context, String src) {
        
        this.cl = context.cl;
        this.context = context;

        int[] intArray = new int[1];
        // Create the program
        ID = cl.clCreateProgramWithSource(context.ID, 1, new String[] {src}, new long[]{src.length()}, 0, intArray, 0);
        checkForError(intArray[0], "can not create program with source");
    }

    private final void initKernels() {

        if(kernels == null) {

            int[] numKernels = new int[1];
            int ret = cl.clCreateKernelsInProgram(ID, 0, null, 0, numKernels, 0);
            checkForError(ret, "can not create kernels for program");

            if(numKernels[0] > 0) {
                HashMap<String, CLKernel> map = new HashMap<String, CLKernel>();

                long[] kernelIDs = new long[numKernels[0]];
                ret = cl.clCreateKernelsInProgram(ID, kernelIDs.length, kernelIDs, 0, null, 0);
                checkForError(ret, "can not create kernels for program");

                for (int i = 0; i < kernelIDs.length; i++) {
                    CLKernel kernel = new CLKernel(this, kernelIDs[i]);
                    map.put(kernel.name, kernel);
                }
                this.kernels = map;
            }else{
                initBuildStatus();
                if(!isExecutable()) {
                    // It is illegal to create kernels from a not executable program.
                    // For consistency between AMD and NVIDIA drivers throw an exception at this point.
                    throw new CLException(CL_INVALID_PROGRAM_EXECUTABLE,
                            "can not initialize kernels, program is not executable. status: "+buildStatusMap);
                }
            }
        }
    }

    private final void initBuildStatus() {

        if(buildStatusMap == null) {
            Map<CLDevice, Status> map = new HashMap<CLDevice, Status>();
            CLDevice[] devices = getCLDevices();
            for (CLDevice device : devices) {
                Status status = getBuildStatus(device);
                if(status == Status.BUILD_SUCCESS) {
                    executable = true;
                }
                map.put(device, status);
            }
            this.buildStatusMap = Collections.unmodifiableMap(map);
        }
    }

    // TODO serialization, program build options

    private final String getBuildInfoString(long device, int flag) {

        long[] longArray = new long[1];

        int ret = cl.clGetProgramBuildInfo(ID, device, flag, 0, null, longArray, 0);
        checkForError(ret, "on clGetProgramBuildInfo");

        ByteBuffer bb = ByteBuffer.allocate((int)longArray[0]).order(ByteOrder.nativeOrder());

        ret = cl.clGetProgramBuildInfo(ID, device, flag, bb.capacity(), bb, null, 0);
        checkForError(ret, "on clGetProgramBuildInfo");

        return CLUtils.clString2JavaString(bb.array(), (int)longArray[0]);
    }

    private final String getProgramInfoString(int flag) {

        long[] longArray = new long[1];

        int ret = cl.clGetProgramInfo(ID, flag, 0, null, longArray, 0);
        checkForError(ret, "on clGetProgramInfo");

        ByteBuffer bb = ByteBuffer.allocate((int)longArray[0]).order(ByteOrder.nativeOrder());

        ret = cl.clGetProgramInfo(ID, flag, bb.capacity(), bb, null, 0);
        checkForError(ret, "on clGetProgramInfo");

        return CLUtils.clString2JavaString(bb.array(), (int)longArray[0]);
    }

//    private int getProgramInfoInt(int flag) {
//
//        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
//
//        int ret = cl.clGetProgramInfo(programID, flag, bb.capacity(), bb, null, 0);
//        checkForError(ret, "");
//
//        return bb.getInt();
//    }

    private int getBuildInfoInt(long device, int flag) {

        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());

        int ret = cl.clGetProgramBuildInfo(ID, device, flag, bb.capacity(), bb, null, 0);
        checkForError(ret, "error on clGetProgramBuildInfo");

        return bb.getInt();
    }


    /**
     * Builds this program for all devices associated with the context and implementation specific build options.
     * @return this
     */
    public CLProgram build() {
        build(null, (CLDevice[])null);
        return this;
    }

    /**
     * Builds this program for all devices associated with the context using the specified build options.
     * @return this
     */
    public CLProgram build(String options) {
        build(options, (CLDevice[])null);
        return this;
    }

    /**
     * Builds this program for all devices associated with the context using the specified build options.
     * @return this
     */
    public CLProgram build(Option... options) {
        build(Option.optionsOf(options), (CLDevice[])null);
        return this;
    }

    /**
     * Builds this program for the given devices and with the specified build options. In case this program was
     * already built and there are kernels associated with this program they will be released first before rebuild.
     * @return this
     * @param devices A list of devices this program should be build on or null for all devices of its context.
     */
    public CLProgram build(String options, CLDevice... devices) {

        if(kernels != null) {
            //No changes to the program executable are allowed while there are
            //kernel objects associated with a program object.
            releaseKernels();
        }

        long[] deviceIDs = null;
        if(devices != null) {
            deviceIDs = new long[devices.length];
            for (int i = 0; i < deviceIDs.length; i++) {
                deviceIDs[i] = devices[i].ID;
            }
        }

        // invalidate build status
        buildStatusMap = null;
        executable = false;

        // Build the program
        int ret = cl.clBuildProgram(ID, deviceIDs, options, null, null);

        if(ret != CL_SUCCESS) {
            throw new CLException(ret, "\n"+getBuildLog());
        }

        return this;
    }

    void onKernelReleased(CLKernel kernel) {
        this.kernels.remove(kernel.name);
    }

    /**
     * Releases this program with its kernels.
     */
    public void release() {

        releaseKernels();

        int ret = cl.clReleaseProgram(ID);
        context.onProgramReleased(this);
        checkForError(ret, "can not release program");
    }

    private void releaseKernels() {
        if(kernels != null) {
            String[] names = kernels.keySet().toArray(new String[kernels.size()]);
            for (String name : names) {
                kernels.get(name).release();
            }
            kernels = null;
        }
    }

    /**
     * Returns the kernel with the specified name.
     * @throws IllegalArgumentException when no kernel with the specified name exists in this program.
     */
    public CLKernel getCLKernel(String kernelName) {
        initKernels();
        final CLKernel kernel = kernels.get(kernelName);
        if(kernel == null) {
            throw new IllegalArgumentException(
                    this+" does not contain a kernel with the name '"+kernelName+"'");
        }
        return kernel;
    }


    /**
     * Returns all kernels of this program in a unmodifiable view of a map
     * with the kernel function names as keys.
     */
    public Map<String, CLKernel> getCLKernels() {
        initKernels();
        return Collections.unmodifiableMap(kernels);
    }

    /**
     * Returns all devices associated with this program.
     */
    public CLDevice[] getCLDevices() {

        long[] longArray = new long[1];
        int ret = cl.clGetProgramInfo(ID, CL_PROGRAM_DEVICES, 0, null, longArray, 0);
        checkForError(ret, "on clGetProgramInfo");

        ByteBuffer bb = ByteBuffer.allocate((int) longArray[0]).order(ByteOrder.nativeOrder());
        ret = cl.clGetProgramInfo(ID, CL_PROGRAM_DEVICES, bb.capacity(), bb, null, 0);
        checkForError(ret, "on clGetProgramInfo");

        int count = bb.capacity() / (CPU.is32Bit()?4:8);
        CLDevice[] devices = new CLDevice[count];
        for (int i = 0; i < count; i++) {
            devices[i] = context.getCLDevice(CPU.is32Bit()?bb.getInt():bb.getLong());
        }

        return devices;

    }

    /**
     * Returns the build log of this program on all devices. The contents of the log are
     * implementation dependent.
     */
    public String getBuildLog() {
        StringBuilder sb = new StringBuilder();
        CLDevice[] devices = getCLDevices();
        for (int i = 0; i < devices.length; i++) {
            CLDevice device = devices[i];
            sb.append(device).append(" build log:\n");
            String log = getBuildLog(device).trim();
            sb.append(log.isEmpty()?"    <empty>":log);
            if(i != devices.length-1)
                sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Returns the build status enum of this program for each device as Map.
     */
    public Map<CLDevice,Status> getBuildStatus() {
        initBuildStatus();
        return buildStatusMap;
    }

    /**
     * Returns true if the build status 'BUILD_SUCCESS' for at least one device
     * of this program exists.
     */
    public boolean isExecutable() {
        initBuildStatus();
        return executable;
    }

    /**
     * Returns the build log for this program on the specified device. The contents
     * of the log are implementation dependent log can be an empty String.
     */
    public String getBuildLog(CLDevice device) {
        return getBuildInfoString(device.ID, CL_PROGRAM_BUILD_LOG);
    }

    /**
     * Returns the build status enum for this program on the specified device.
     */
    public Status getBuildStatus(CLDevice device) {
        int clStatus = getBuildInfoInt(device.ID, CL_PROGRAM_BUILD_STATUS);
        return Status.valueOf(clStatus);
    }

    /**
     * Returns the source code of this program. Note: sources are not cached,
     * each call of this method calls into Open
     */
    public String getSource() {
        return getProgramInfoString(CL_PROGRAM_SOURCE);
    }

    /**
     * Returns the binaries for this program in a map containing the device as key
     * and the byte array as value.
     */
    public Map<CLDevice, byte[]> getBinaries() {

        CLDevice[] devices = getCLDevices();

        ByteBuffer sizes = ByteBuffer.allocate(8*devices.length).order(ByteOrder.nativeOrder());
        int ret = cl.clGetProgramInfo(ID, CL_PROGRAM_BINARY_SIZES, sizes.capacity(), sizes, null, 0);
        checkForError(ret, "on clGetProgramInfo");

        int binarySize = 0;
        while(sizes.remaining() != 0)
            binarySize += (int)sizes.getLong();

        ByteBuffer binaries = ByteBuffer.allocate(binarySize).order(ByteOrder.nativeOrder());
        ret = cl.clGetProgramInfo(ID, CL_PROGRAM_BINARIES, binaries.capacity(), binaries, null, 0); // TODO crash, driver bug?
        checkForError(ret, "on clGetProgramInfo");

        Map<CLDevice, byte[]> map = new HashMap<CLDevice, byte[]>();

        for (int i = 0; i < devices.length; i++) {
            byte[] bytes = new byte[(int)sizes.getLong()];
            binaries.get(bytes);
            map.put(devices[i], bytes);
        }

        return map;
    }

    @Override
    public String toString() {
        return "CLProgram [id: " + ID
                       + " status: "+getBuildStatus()+"]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CLProgram other = (CLProgram) obj;
        if (this.ID != other.ID) {
            return false;
        }
        if (!this.context.equals(other.context)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.context != null ? this.context.hashCode() : 0);
        hash = 37 * hash + (int) (this.ID ^ (this.ID >>> 32));
        return hash;
    }
    
    public enum Status {

        BUILD_SUCCESS(CL_BUILD_SUCCESS),
        BUILD_NONE(CL_BUILD_NONE),
        BUILD_IN_PROGRESS(CL_BUILD_IN_PROGRESS),
        BUILD_ERROR(CL_BUILD_ERROR);

        /**
         * Value of wrapped OpenCL device type.
         */
        public final int STATUS;

        private Status(int status) {
            this.STATUS = status;
        }

        public static Status valueOf(int clBuildStatus) {
            switch(clBuildStatus) {
                case(CL_BUILD_SUCCESS):
                    return BUILD_SUCCESS;
                case(CL_BUILD_NONE):
                    return BUILD_NONE;
                case(CL_BUILD_IN_PROGRESS):
                    return BUILD_IN_PROGRESS;
                case(CL_BUILD_ERROR):
                    return BUILD_ERROR;
// is this a standard state?
//              case (CL_BUILD_PROGRAM_FAILURE):
//                    return BUILD_PROGRAM_FAILURE;
            }
            return null;
        }
    }

    /**
     * Common compiler optons.
     */
    public enum Option {

        /**
         * Treat double precision floating-point constant as single precision constant.
         */
        SINGLE_PRECISION_CONSTANTS("-cl-single-precision-constant"),

        /**
         * This option controls how single precision and double precision denormalized numbers are handled.
         * If specified as a build option, the single precision denormalized numbers may be flushed to zero
         * and if the optional extension for double precision is supported, double precision denormalized numbers
         * may also be flushed to zero. This is intended to be a performance hint and the OpenCL compiler can choose
         * not to flush denorms to zero if the device supports single precision (or double precision) denormalized numbers.<br>
         * This option is ignored for single precision numbers if the device does not support single precision denormalized
         * numbers i.e. CL_FP_DENORM bit is not set in CL_DEVICE_SINGLE_FP_CONFIG<br>
         * This option is ignored for double precision numbers if the device does not support double precision or if it does support
         * double precison but CL_FP_DENORM bit is not set in CL_DEVICE_DOUBLE_FP_CONFIG.<br>
         * This flag only applies for scalar and vector single precision floating-point variables and computations on
         * these floating-point variables inside a program. It does not apply to reading from or writing to image objects.
         */
        DENORMS_ARE_ZERO("-cl-denorms-are-zero"),

        /**
         * This option disables all optimizations. The default is optimizations are enabled.
         */
        DISABLE_OPT("-cl-opt-disable"),

        /**
         * This option allows the compiler to assume the strictest aliasing rules.
         */
        STRICT_ALIASING("-cl-strict-aliasing"),

        /**
         * Allow a * b + c to be replaced by a mad. The mad computes a * b + c with reduced accuracy.
         * For example, some OpenCL devices implement mad as truncate the result of a * b before adding it to c.
         */
        ENABLE_MAD("-cl-mad-enable"),

        /**
         * Allow optimizations for floating-point arithmetic that ignore the signedness of zero.
         * IEEE 754 arithmetic specifies the behavior of distinct +0.0 and -0.0 values, which then prohibits
         * simplification of expressions such as x+0.0 or 0.0*x (even with -cl-finite-math-only ({@link #FINITE_MATH_ONLY})).
         * This option implies that the sign of a zero result isn't significant.
         */
        NO_SIGNED_ZEROS("-cl-no-signed-zeros"),

        /**
         * Allow optimizations for floating-point arithmetic that<br>
         * (a) assume that arguments and results are valid,<br>
         * (b) may violate IEEE 754 standard and<br>
         * (c) may violate the OpenCL numerical compliance requirements as defined in section
         * 7.4 for single-precision floating-point, section 9.3.9 for double-precision floating-point,
         * and edge case behavior in section 7.5.<br>
         * This option includes the -cl-no-signed-zeros ({@link #NO_SIGNED_ZEROS})
         * and -cl-mad-enable ({@link #ENABLE_MAD}) options.
         */
        UNSAFE_MATH("-cl-unsafe-math-optimizations"),

        /**
         * Allow optimizations for floating-point arithmetic that assume that arguments and results are not NaNs or ±∞.
         * This option may violate the OpenCL numerical compliance requirements defined in in section 7.4 for
         * single-precision floating-point, section 9.3.9 for double-precision floating-point, and edge case behavior in section 7.5.
         */
        FINITE_MATH_ONLY("-cl-finite-math-only"),

        /**
         * Sets the optimization options -cl-finite-math-only ({@link #FINITE_MATH_ONLY}) and -cl-unsafe-math-optimizations ({@link #UNSAFE_MATH}).
         * This allows optimizations for floating-point arithmetic that may violate the IEEE 754
         * standard and the OpenCL numerical compliance requirements defined in the specification
         * in section 7.4 for single-precision floating-point, section 9.3.9 for double-precision
         * floating-point, and edge case behavior in section 7.5. This option causes the preprocessor
         * macro __FAST_RELAXED_MATH__ to be defined in the OpenCL program.
         */
        FAST_RELAXED_MATH("-cl-fast-relaxed-math"),

        /**
         * Inhibit all warning messages.
         */
        DISABLE_WARNINGS("-w"),

        /**
         * Make all warnings into errors.
         */
        WARNINGS_ARE_ERRORS("-Werror");

        private final String option;

        private Option(String option) {
            this.option = option;
        }

        public final static String optionsOf(Option... options) {
            StringBuilder sb = new StringBuilder(options.length * 24);
            for (int i = 0; i < options.length; i++) {
                sb.append(options[i].option);
                if(i!= options.length-1)
                    sb.append(" ");
            }
            return sb.toString();
        }

    }

}
