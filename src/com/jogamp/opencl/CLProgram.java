/*
 * Copyright 2009 - 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package com.jogamp.opencl;

import com.jogamp.common.nio.CachedBufferFactory;
import com.jogamp.opencl.util.CLProgramConfiguration;
import com.jogamp.opencl.util.CLUtil;
import com.jogamp.common.os.Platform;
import com.jogamp.common.nio.NativeSizeBuffer;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.opencl.impl.BuildProgramCallback;
import com.jogamp.opencl.util.CLBuildListener;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static com.jogamp.opencl.CLException.*;
import static com.jogamp.opencl.CL.*;
import static com.jogamp.common.nio.Buffers.*;

/**
 * Represents a OpenCL program executed on one or more {@link CLDevice}s.
 * A CLProgram must be build using one of the build methods before creating {@link CLKernel}s.
 * @see CLContext#createProgram(java.io.InputStream)
 * @see CLContext#createProgram(java.lang.String)
 * @see CLContext#createProgram(java.util.Map)
 * @author Michael Bien
 */
public class CLProgram extends CLObject implements CLResource {

    private final static ReentrantLock buildLock = new ReentrantLock();
    
    private final Set<CLKernel> kernels;
    private Map<CLDevice, Status> buildStatusMap;

    private boolean executable;
    private boolean released;

    private CLProgram(CLContext context, long id) {
        super(context, id);
        this.kernels = new HashSet<CLKernel>();
    }
    
    static CLProgram create(CLContext context, String src) {

        IntBuffer status = newDirectIntBuffer(1);
        
        NativeSizeBuffer length = NativeSizeBuffer.allocateDirect(1).put(0, src.length());
        String[] srcArray = new String[] {src};
        
        // Create the program
        long id = context.cl.clCreateProgramWithSource(context.ID, 1, srcArray, length, status);

        int err = status.get();
        if(err != CL_SUCCESS) {
            throw newException(err, "can not create program with source on "+context);
        }
        
        return new CLProgram(context, id);
    }

    static CLProgram create(CLContext context, Map<CLDevice, byte[]> binaries) {

        Set<Entry<CLDevice, byte[]>> entries = binaries.entrySet();
        
        // calculate buffer size
        int binarySize = 0;
        for (Map.Entry<CLDevice, byte[]> entry : entries) {
            binarySize += entry.getValue().length;
        }

        int pbSize = NativeSizeBuffer.elementSize();
        int deviceCount = binaries.size();
        
        CachedBufferFactory bf = CachedBufferFactory.create(binarySize + pbSize*deviceCount*3 + 4, true);
        NativeSizeBuffer devices  = NativeSizeBuffer.wrap(bf.newDirectByteBuffer(deviceCount*pbSize));
        PointerBuffer codeBuffers = PointerBuffer.wrap(bf.newDirectByteBuffer(deviceCount*pbSize));
        NativeSizeBuffer lengths  = NativeSizeBuffer.wrap(bf.newDirectByteBuffer(deviceCount*pbSize));
        
        int i = 0;
        for (Map.Entry<CLDevice, byte[]> entry : entries) {

            byte[] bytes = entry.getValue();
            CLDevice device = entry.getKey();

            devices.put(device.ID);
            lengths.put(bytes.length);

            codeBuffers.referenceBuffer(i, bf.newDirectByteBuffer(bytes));
            i++;
        }
        devices.rewind();
        lengths.rewind();

        IntBuffer errBuffer = bf.newDirectIntBuffer(1);
//        IntBuffer status = newDirectByteBuffer(binaries.size()*4).asIntBuffer();
        long id = context.cl.clCreateProgramWithBinary(context.ID, devices.capacity(), devices, lengths, codeBuffers, /*status*/null, errBuffer);

//        while(status.remaining() != 0) {
//            checkForError(status.get(), "unable to load binaries on all devices");
//        }

        int err = errBuffer.get();
        if(err != CL_SUCCESS) {
            throw newException(err, "can not create program on "+context +" with binaries "+binaries);
        }

        return new CLProgram(context, id);
    }

    private void initBuildStatus() {

        if(buildStatusMap == null) {
//            synchronized(buildLock) {
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
//            }
        }
    }

    private String getBuildInfoString(CLDevice device, int flag) {

        if(released) {
            return "";
        }

        NativeSizeBuffer size = NativeSizeBuffer.allocateDirect(1);

        int ret = cl.clGetProgramBuildInfo(ID, device.ID, flag, 0, null, size);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "on clGetProgramBuildInfo with "+device);
        }

        ByteBuffer buffer = newDirectByteBuffer((int)size.get(0));

        ret = cl.clGetProgramBuildInfo(ID, device.ID, flag, buffer.capacity(), buffer, null);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "on clGetProgramBuildInfo with "+device);
        }

        return CLUtil.clString2JavaString(buffer, (int)size.get(0));
    }

    private String getProgramInfoString(int flag) {

        if(released) {
            return "";
        }

        NativeSizeBuffer size = NativeSizeBuffer.allocateDirect(1);

        int ret = cl.clGetProgramInfo(ID, flag, 0, null, size);
        checkForError(ret, "on clGetProgramInfo");

        ByteBuffer buffer = newDirectByteBuffer((int)size.get(0));

        ret = cl.clGetProgramInfo(ID, flag, buffer.capacity(), buffer, null);
        checkForError(ret, "on clGetProgramInfo");

        return CLUtil.clString2JavaString(buffer, (int)size.get(0));
    }

    private int getBuildInfoInt(CLDevice device, int flag) {

        ByteBuffer buffer = newDirectByteBuffer(4);

        int ret = cl.clGetProgramBuildInfo(ID, device.ID, flag, buffer.capacity(), buffer, null);
        checkForError(ret, "error on clGetProgramBuildInfo");

        return buffer.getInt();
    }


    /**
     * Builds this program for all devices associated with the context.
     * @return this
     */
    public CLProgram build() {
        build(null, (String)null, (CLDevice[]) null);
        return this;
    }

    /**
     * Builds this program for all devices associated with the context.
     * @see CLBuildListener
     * @param listener A listener who is notified when the program was built.
     * @return this
     */
    public CLProgram build(CLBuildListener listener) {
        build(listener, null, (CLDevice[])null);
        return this;
    }
    
    /**
     * Builds this program for the given devices.
     * @param devices A list of devices this program should be build on or null for all devices of its context.
     * @return this
     */
    public CLProgram build(CLDevice... devices) {
        build(null, (String) null, devices);
        return this;
    }

    /**
     * Builds this program for the given devices.
     * @see CLBuildListener
     * @param listener A listener who is notified when the program was built.
     * @param devices A list of devices this program should be build on or null for all devices of its context.
     * @return this
     */
    public CLProgram build(CLBuildListener listener, CLDevice... devices) {
        build(listener,null, devices);
        return this;
    }

    /**
     * Builds this program for all devices associated with the context using the specified build options.
     * @see CompilerOptions
     * @return this
     */
    public CLProgram build(String options) {
        build(null, options, (CLDevice[])null);
        return this;
    }

    /**
     * Builds this program for all devices associated with the context using the specified build options.
     * @see CompilerOptions
     * @see CLBuildListener
     * @param listener A listener who is notified when the program was built.
     * @return this
     */
    public CLProgram build(CLBuildListener listener, String options) {
        build(listener, options, (CLDevice[])null);
        return this;
    }

    /**
     * Builds this program for all devices associated with the context using the specified build options.
     * @see CompilerOptions
     */
    public CLProgram build(String... options) {
        build(null, optionsOf(options), (CLDevice[])null);
        return this;
    }

    /**
     * Builds this program for all devices associated with the context using the specified build options.
     * @see CompilerOptions
     * @see CLBuildListener
     * @param listener A listener who is notified when the program was built.
     */
    public CLProgram build(CLBuildListener listener, String... options) {
        build(listener, optionsOf(options), (CLDevice[])null);
        return this;
    }

    /**
     * Builds this program for the given devices and with the specified build options. In case this program was
     * already built and there are kernels associated with this program they will be released first before rebuild.
     * @see CompilerOptions
     * @param devices A list of devices this program should be build on or null for all devices of its context.
     * @return this
     */
    public CLProgram build(String options, CLDevice... devices) {
        build(null, options, devices);
        return this;
    }

    /**
     * Builds this program for the given devices and with the specified build options. In case this program was
     * already built and there are kernels associated with this program they will be released first before rebuild.
     * @see CompilerOptions
     * @see CLBuildListener
     * @return this
     * @param devices A list of devices this program should be build on or null for all devices of its context.
     * @param listener A listener who is notified when the program was built.
     */
    public CLProgram build(final CLBuildListener listener, String options, CLDevice... devices) {

        if(released) {
            throw new CLException("can not build a released program");
        }

        if(!kernels.isEmpty()) {
            //No changes to the program executable are allowed while there are
            //kernel objects associated with a program object.
            releaseKernels();
        }

        NativeSizeBuffer deviceIDs = null;
        int count = 0;
        if(devices != null && devices.length != 0) {
            deviceIDs = NativeSizeBuffer.allocateDirect(devices.length);
            for (int i = 0; i < devices.length; i++) {
                deviceIDs.put(i, devices[i].ID);
            }
            deviceIDs.rewind();
            count = devices.length;
        }

        // nvidia driver doesn't like empty strings
        if(options != null && options.trim().isEmpty()) {
            options = null;
        }

        // invalidate build status
        buildStatusMap = null;
        executable = false;

        BuildProgramCallback callback = null;
        if(listener != null) {
            callback = new BuildProgramCallback() {
                @Override
                public void buildFinished(long cl_program) {
                    buildLock.unlock();
                    listener.buildFinished(CLProgram.this);
                }
            };
        }

        // Build the program
        int ret = 0;

        // spec: building programs is not threadsafe, we are locking the API call to
        // make sure only one thread calls it at a time until it completes (asynchronous or synchronously).
        {
            buildLock.lock();
            boolean exception = true;
            try{
                ret = cl.clBuildProgram(ID, count, deviceIDs, options, callback);
                exception = false;
            }finally{
                if(callback == null || exception) {
                    buildLock.unlock();
                }
            }
        }

        if(ret != CL_SUCCESS) {
            throw newException(ret, "\n"+getBuildLog());
        }

        return this;
    }

    /**
     * Prepares the build for this program by returning a new {@link CLProgramConfiguration}.
     */
    public CLProgramConfiguration prepare() {
        return CLProgramBuilder.createConfiguration(this);
    }

    /**
     * Creates a kernel with the specified kernel name.
     */
    public CLKernel createCLKernel(String kernelName) {

        if(released) {
            return null;
        }

        int[] err = new int[1];
        long id = cl.clCreateKernel(ID, kernelName, err, 0);
        if(err[0] != CL_SUCCESS) {
            throw newException(err[0], "unable to create Kernel with name: "+kernelName);
        }

        CLKernel kernel = new CLKernel(this, id);
        kernels.add(kernel);
        return kernel;
    }

    /**
     * Creates all kernels of this program and stores them a Map with the kernel name as key.
     */
    public Map<String, CLKernel> createCLKernels() {

        if(released) {
            return Collections.emptyMap();
        }

        HashMap<String, CLKernel> newKernels = new HashMap<String, CLKernel>();

        IntBuffer numKernels = newDirectByteBuffer(4).asIntBuffer();
        int ret = cl.clCreateKernelsInProgram(ID, 0, null, numKernels);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not create kernels for "+this);
        }

        if(numKernels.get(0) > 0) {

            NativeSizeBuffer kernelIDs = NativeSizeBuffer.allocateDirect(numKernels.get(0));
            ret = cl.clCreateKernelsInProgram(ID, kernelIDs.capacity(), kernelIDs, null);
            if(ret != CL_SUCCESS) {
                throw newException(ret, "can not create "+kernelIDs.capacity()+" kernels for "+this);
            }

            for (int i = 0; i < kernelIDs.capacity(); i++) {
                CLKernel kernel = new CLKernel(this, kernelIDs.get(i));
                kernels.add(kernel);
                newKernels.put(kernel.name, kernel);
            }
        }else{
            initBuildStatus();
            if(!isExecutable()) {
                // It is illegal to create kernels from a not executable program.
                // For consistency between AMD and NVIDIA drivers throw an exception at this point.
                throw newException(CL_INVALID_PROGRAM_EXECUTABLE,
                        "can not initialize kernels, program is not executable. status: "+buildStatusMap);
            }
        }

        return newKernels;
    }

    void onKernelReleased(CLKernel kernel) {
        this.kernels.remove(kernel);
    }

    /**
     * Releases this program with its kernels.
     */
    @Override
    public void release() {

        releaseKernels();

        executable = false;
        released = true;
        buildStatusMap = null;

        int ret = cl.clReleaseProgram(ID);
        context.onProgramReleased(this);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not release "+this);
        }
    }

    private void releaseKernels() {
        if(!kernels.isEmpty()) {
            // copy to array to prevent concurrent modification exception
            CLKernel[] array = kernels.toArray(new CLKernel[kernels.size()]);
            for (CLKernel kernel : array) {
                kernel.release();
            }
        }
    }

    /**
     * Returns all devices associated with this program.
     */
    public CLDevice[] getCLDevices() {
        if(released) {
            return new CLDevice[0];
        }
        NativeSizeBuffer size = NativeSizeBuffer.allocateDirect(1);
        int ret = cl.clGetProgramInfo(ID, CL_PROGRAM_DEVICES, 0, null, size);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "on clGetProgramInfo of "+this);
        }

        ByteBuffer bb = newDirectByteBuffer((int) size.get(0));
        ret = cl.clGetProgramInfo(ID, CL_PROGRAM_DEVICES, bb.capacity(), bb, null);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "on clGetProgramInfo of "+this);
        }

        int count = bb.capacity() / (Platform.is32Bit()?4:8);
        CLDevice[] devices = new CLDevice[count];
        for (int i = 0; i < count; i++) {
            devices[i] = context.getDevice(Platform.is32Bit()?bb.getInt():bb.getLong());
        }

        return devices;

    }

    /**
     * Returns the build log of this program on all devices. The contents of the log are
     * implementation dependent.
     */
    public String getBuildLog() {
        if(released) {
            return "";
        }
        StringBuilder sb = new StringBuilder(200);
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
        if(released) {
            return Collections.emptyMap();
        }
        initBuildStatus();
        return buildStatusMap;
    }

    /**
     * Returns true if the build status 'BUILD_SUCCESS' for at least one device
     * of this program exists.
     */
    public boolean isExecutable() {
        if(released) {
            return false;
        }
        initBuildStatus();
        return executable;
    }

    /**
     * Returns the build log for this program on the specified device. The contents
     * of the log are implementation dependent log can be an empty String.
     */
    public String getBuildLog(CLDevice device) {
        return getBuildInfoString(device, CL_PROGRAM_BUILD_LOG);
    }

    /**
     * Returns the build status enum for this program on the specified device.
     */
    public Status getBuildStatus(CLDevice device) {
        if(released) {
            return Status.BUILD_NONE;
        }
        int clStatus = getBuildInfoInt(device, CL_PROGRAM_BUILD_STATUS);
        return Status.valueOf(clStatus);
    }

    /**
     * Returns the source code of this program. Note: sources are not cached,
     * each call of this method calls into Open
     */
    public String getSource() {
        // some drivers return IVE codes if the program haven't been built from source.
        try{
            return getProgramInfoString(CL_PROGRAM_SOURCE);
        }catch(CLException.CLInvalidValueException ingore) {
            return "";
        }
    }

    /**
     * Returns the binaries for this program in an ordered Map containing the device as key
     * and the program binaries as value.
     */
    public Map<CLDevice, byte[]> getBinaries() {

        if(!isExecutable()) {
            return Collections.emptyMap();
        }
        
        CLDevice[] devices = getCLDevices();

        NativeSizeBuffer sizes = NativeSizeBuffer.allocateDirect(devices.length);
        int ret = cl.clGetProgramInfo(ID, CL_PROGRAM_BINARY_SIZES, sizes.capacity()*NativeSizeBuffer.elementSize(), sizes.getBuffer(), null);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "on clGetProgramInfo(CL_PROGRAM_BINARY_SIZES) of "+this);
        }

        int binariesSize = 0;
        while(sizes.remaining() != 0) {
            int size = (int) sizes.get();
            binariesSize += size;
        }
        ByteBuffer binaries = newDirectByteBuffer(binariesSize);

        
        long address = InternalBufferUtil.getDirectBufferAddress(binaries);
        NativeSizeBuffer addresses = NativeSizeBuffer.allocateDirect(sizes.capacity());
        sizes.rewind();
        while(sizes.remaining() != 0) {
            addresses.put(address);
            address += sizes.get();
        }
        addresses.rewind();
        
        ret = cl.clGetProgramInfo(ID, CL_PROGRAM_BINARIES, addresses.capacity()*NativeSizeBuffer.elementSize(), addresses.getBuffer(), null);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "on clGetProgramInfo(CL_PROGRAM_BINARIES) of "+this);
        }

        Map<CLDevice, byte[]> map = new LinkedHashMap<CLDevice, byte[]>();
        sizes.rewind();
        for (int i = 0; i < devices.length; i++) {
            byte[] bytes = new byte[(int)sizes.get()];
            binaries.get(bytes);
            map.put(devices[i], bytes);
        }

        return map;
    }

    /**
     * Utility method which builds a properly seperated option string.
     */
    public static String optionsOf(String... options) {
        StringBuilder sb = new StringBuilder(options.length * 24);
        for (int i = 0; i < options.length; i++) {
            sb.append(options[i]);
            if(i!= options.length-1)
                sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Utility method for defining macros as build options (Returns "-D name").
     */
    public static String define(String name) {
        return "-D "+name;
    }

    /**
     * Utility method for defining macros as build options (Returns "-D name=value").
     */
    public static String define(String name, Object value) {
        return "-D "+name+"="+value;
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
     * Common compiler options for the OpenCL compiler.
     */
    public interface CompilerOptions {

        /**
         * Treat double precision floating-point constant as single precision constant.
         */
        public final static String SINGLE_PRECISION_CONSTANTS = "-cl-single-precision-constant";

        /**
         * This option controls how single precision and double precision denormalized numbers are handled.
         * If specified as a build option, the single precision denormalized numbers may be flushed to zero
         * and if the optional extension for double precision is supported, double precision denormalized numbers
         * may also be flushed to zero. This is intended to be a performance hint and the OpenCL compiler can choose
         * not to flush denorms to zero if the device supports single precision (or double precision) denormalized numbers.<br>
         * This option is ignored for single precision numbers if the device does not support single precision denormalized
         * numbers i.e. {@link CLDevice.FPConfig#DENORM} is not present in the set returned by {@link CLDevice#getSingleFPConfig()}<br>
         * This option is ignored for double precision numbers if the device does not support double precision or if it does support
         * double precision but {@link CLDevice.FPConfig#DENORM} is not present in the set returned by {@link CLDevice#getDoubleFPConfig()}.<br>
         * This flag only applies for scalar and vector single precision floating-point variables and computations on
         * these floating-point variables inside a program. It does not apply to reading from or writing to image objects.
         */
        public final static String DENORMS_ARE_ZERO = "-cl-denorms-are-zero";

        /**
         * This option disables all optimizations. The default is optimizations are enabled.
         */
        public final static String DISABLE_OPT = "-cl-opt-disable";

        /**
         * This option allows the compiler to assume the strictest aliasing rules.
         */
        public final static String STRICT_ALIASING = "-cl-strict-aliasing";

        /**
         * Allow a * b + c to be replaced by a mad. The mad computes a * b + c with reduced accuracy.
         * For example, some OpenCL devices implement mad as truncate the result of a * b before adding it to c.
         */
        public final static String ENABLE_MAD = "-cl-mad-enable";

        /**
         * Allow optimizations for floating-point arithmetic that ignore the signedness of zero.
         * IEEE 754 arithmetic specifies the behavior of distinct +0.0 and -0.0 values, which then prohibits
         * simplification of expressions such as x+0.0 or 0.0*x (even with -cl-finite-math-only ({@link #FINITE_MATH_ONLY})).
         * This option implies that the sign of a zero result isn't significant.
         */
        public final static String NO_SIGNED_ZEROS = "-cl-no-signed-zeros";

        /**
         * Allow optimizations for floating-point arithmetic that<br>
         * (a) assume that arguments and results are valid,<br>
         * (b) may violate IEEE 754 standard and<br>
         * (c) may violate the OpenCL numerical compliance requirements as defined in section
         * 7.4 for single-precision floating-point, section 9.3.9 for double-precision floating-point,
         * and edge case behavior in section 7.5.
         * This option includes the -cl-no-signed-zeros ({@link #NO_SIGNED_ZEROS})
         * and -cl-mad-enable ({@link #ENABLE_MAD}) options.
         */
        public final static String UNSAFE_MATH = "-cl-unsafe-math-optimizations";

        /**
         * Allow optimizations for floating-point arithmetic that assume that arguments and results are not NaNs or ±∞.
         * This option may violate the OpenCL numerical compliance requirements defined in in section 7.4 for
         * single-precision floating-point, section 9.3.9 for double-precision floating-point, and edge case behavior in section 7.5.
         */
        public final static String FINITE_MATH_ONLY = "-cl-finite-math-only";

        /**
         * Sets the optimization options -cl-finite-math-only ({@link #FINITE_MATH_ONLY}) and -cl-unsafe-math-optimizations ({@link #UNSAFE_MATH}).
         * This allows optimizations for floating-point arithmetic that may violate the IEEE 754
         * standard and the OpenCL numerical compliance requirements defined in the specification
         * in section 7.4 for single-precision floating-point, section 9.3.9 for double-precision
         * floating-point, and edge case behavior in section 7.5. This option causes the preprocessor
         * macro __FAST_RELAXED_MATH__ to be defined in the OpenCL program.
         */
        public final static String FAST_RELAXED_MATH = "-cl-fast-relaxed-math";

        /**
         * Inhibit all warning messages.
         */
        public final static String DISABLE_WARNINGS = "-w";

        /**
         * Make all warnings into errors.
         */
        public final static String WARNINGS_ARE_ERRORS = "-Werror";

    }

}
