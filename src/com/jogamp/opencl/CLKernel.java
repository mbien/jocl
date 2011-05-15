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

import com.jogamp.opencl.util.CLUtil;
import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.NativeSizeBuffer;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import static com.jogamp.opencl.CLException.*;
import static com.jogamp.opencl.CL.*;
import static com.jogamp.common.os.Platform.*;

/**
 * High level abstraction for an OpenCL Kernel.
 * A kernel is a function declared in a program. A kernel is identified by the <code>kernel</code> qualifier
 * applied to any function in a program. A kernel object encapsulates the specific <code>kernel</code>
 * function declared in a program and the argument values to be used when executing this
 * <code>kernel</code> function.
 * CLKernel is not threadsafe.
 * @see CLProgram#createCLKernel(java.lang.String)
 * @see CLProgram#createCLKernels()
 * @author Michael Bien
 */
public class CLKernel extends CLObject implements CLResource, Cloneable {

    public final String name;
    public final int numArgs;

    private final CLProgram program;

    private final ByteBuffer buffer;

    private int argIndex;
    private boolean force32BitArgs;

    CLKernel(CLProgram program, long id) {
        this(program, null, id);
    }

    CLKernel(CLProgram program, String name, long id) {
        super(program.getContext(), id);

        this.program = program;
        this.buffer = Buffers.newDirectByteBuffer((is32Bit()?4:8)*3);

        if(name == null) {
            // get function name
            NativeSizeBuffer size = NativeSizeBuffer.wrap(buffer);
            int ret = cl.clGetKernelInfo(ID, CL_KERNEL_FUNCTION_NAME, 0, null, size);
            checkForError(ret, "error while asking for kernel function name");

            ByteBuffer bb = Buffers.newDirectByteBuffer((int)size.get(0));

            ret = cl.clGetKernelInfo(ID, CL_KERNEL_FUNCTION_NAME, bb.capacity(), bb, null);
            checkForError(ret, "error while asking for kernel function name");
            
            this.name = CLUtil.clString2JavaString(bb, bb.capacity());
        }else{
            this.name = name;
        }

        // get number of arguments
        int ret = cl.clGetKernelInfo(ID, CL_KERNEL_NUM_ARGS, buffer.capacity(), buffer, null);
        checkForError(ret, "error while asking for number of function arguments.");

        numArgs = buffer.getInt(0);

    }

//    public CLKernel putArg(Buffer value) {
//        setArg(argIndex++, value);
//        return this;
//    }
    
    public CLKernel putArg(CLMemory<?> value) {
        setArg(argIndex, value);
        argIndex++;
        return this;
    }

    public CLKernel putArg(int value) {
        setArg(argIndex, value);
        argIndex++;
        return this;
    }

    public CLKernel putArg(long value) {
        setArg(argIndex, value);
        argIndex++;
        return this;
    }

    public CLKernel putArg(float value) {
        setArg(argIndex, value);
        argIndex++;
        return this;
    }

    public CLKernel putArg(double value) {
        setArg(argIndex, value);
        argIndex++;
        return this;
    }

    public CLKernel putNullArg(int size) {
        setNullArg(argIndex, size);
        argIndex++;
        return this;
    }

    public CLKernel putArgs(CLMemory<?>... values) {
        setArgs(argIndex, values);
        argIndex += values.length;
        return this;
    }

    /**
     * Resets the argument index to 0.
     */
    public CLKernel rewind() {
        argIndex = 0;
        return this;
    }

    /**
     * Returns the argument index used in the relative putArt(...) methods.
     */
    public int position() {
        return argIndex;
    }

//    public CLKernel setArg(int argumentIndex, Buffer value) {
//        setArgument(argumentIndex, CLMemory.sizeOfBufferElem(value)*value.capacity(), value);
//        return this;
//    }

    public CLKernel setArg(int argumentIndex, CLMemory<?> value) {
        setArgument(argumentIndex, is32Bit()?4:8, wrap(value.ID));
        return this;
    }

    public CLKernel setArg(int argumentIndex, int value) {
        setArgument(argumentIndex, 4, wrap(value));
        return this;
    }

    public CLKernel setArg(int argumentIndex, long value) {
        if(force32BitArgs) {
            setArgument(argumentIndex, 4, wrap((int)value));
        }else{
            setArgument(argumentIndex, 8, wrap(value));
        }
        return this;
    }

    public CLKernel setArg(int argumentIndex, float value) {
        setArgument(argumentIndex, 4, wrap(value));
        return this;
    }

    public CLKernel setArg(int argumentIndex, double value) {
        if(force32BitArgs) {
            setArgument(argumentIndex, 4, wrap((float)value));
        }else{
            setArgument(argumentIndex, 8, wrap(value));
        }
        return this;
    }

    public CLKernel setNullArg(int argumentIndex, int size) {
        setArgument(argumentIndex, size, null);
        return this;
    }

    public CLKernel setArgs(CLMemory<?>... values) {
        setArgs(0, values);
        return this;
    }

    private void setArgs(int startIndex, CLMemory<?>... values) {
        for (int i = 0; i < values.length; i++) {
            setArg(i+startIndex, values[i]);
        }
    }

    private void setArgument(int argumentIndex, int size, Buffer value) {
        if(argumentIndex >= numArgs || argumentIndex < 0) {
            throw new IndexOutOfBoundsException("kernel "+ this +" has "+numArgs+
                    " arguments, can not set argument with index "+argumentIndex);
        }
        if(!program.isExecutable()) {
            throw new IllegalStateException("can not set program" +
                    " arguments for a not executable program. "+program);
        }

        int ret = cl.clSetKernelArg(ID, argumentIndex, size, value);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "error setting arg "+argumentIndex+" to value "+value+" of size "+size+" of "+this);
        }
    }

    /**
     * Forces double and long arguments to be passed as float and int to the OpenCL kernel.
     * This can be used in applications which want to mix kernels with different floating point precision.
     */
    public CLKernel setForce32BitArgs(boolean force) {
        this.force32BitArgs = force;
        return this;
    }
    
    public CLProgram getProgram() {
        return program;
    }

    /**
     * @see #setForce32BitArgs(boolean) 
     */
    public boolean isForce32BitArgsEnabled() {
        return force32BitArgs;
    }

    private Buffer wrap(float value) {
        return buffer.putFloat(0, value);
    }

    private Buffer wrap(double value) {
        return buffer.putDouble(0, value);
    }

    private Buffer wrap(int value) {
        return buffer.putInt(0, value);
    }

    private Buffer wrap(long value) {
        return buffer.putLong(0, value);
    }

    /**
     * Returns the amount of local memory in bytes being used by a kernel.
     * This includes local memory that may be needed by an implementation to execute the kernel,
     * variables declared inside the kernel with the <code>__local</code> address qualifier and local memory
     * to be allocated for arguments to the kernel declared as pointers with the <code>__local</code> address
     * qualifier and whose size is specified with clSetKernelArg.
     * If the local memory size, for any pointer argument to the kernel declared with
     * the <code>__local</code> address qualifier, is not specified, its size is assumed to be 0.
     */
    public long getLocalMemorySize(CLDevice device) {
        return getWorkGroupInfo(device, CL_KERNEL_LOCAL_MEM_SIZE);
    }

    /**
     * Returns the work group size for this kernel on the given device.
     * This provides a mechanism for the application to query the work-group size
     * that can be used to execute a kernel on a specific device given by device.
     * The OpenCL implementation uses the resource requirements of the kernel
     * (register usage etc.) to determine what this work-group size should be. 
     */
    public long getWorkGroupSize(CLDevice device) {
        return getWorkGroupInfo(device, CL_KERNEL_WORK_GROUP_SIZE);
    }

    /**
     * Returns the work-group size specified by the <code>__attribute__((reqd_work_group_size(X, Y, Z)))</code> qualifier in kernel sources.
     * If the work-group size is not specified using the above attribute qualifier <code>new long[]{(0, 0, 0)}</code> is returned.
     * The returned array has always three elements.
     */
    public long[] getCompileWorkGroupSize(CLDevice device) {
        int ret = cl.clGetKernelWorkGroupInfo(ID, device.ID, CL_KERNEL_COMPILE_WORK_GROUP_SIZE, (is32Bit()?4:8)*3, buffer, null);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "error while asking for CL_KERNEL_COMPILE_WORK_GROUP_SIZE of "+this+" on "+device);
        }

        if(is32Bit()) {
            return new long[] { buffer.getInt(0), buffer.getInt(4), buffer.getInt(8) };
        }else {
            return new long[] { buffer.getLong(0), buffer.getLong(8), buffer.getLong(16) };
        }
    }

    private long getWorkGroupInfo(CLDevice device, int flag) {
        int ret = cl.clGetKernelWorkGroupInfo(ID, device.ID, flag, 8, buffer, null);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "error while asking for clGetKernelWorkGroupInfo of "+this+" on "+device);
        }
        return buffer.getLong(0);
    }

    /**
     * Releases all resources of this kernel from its context.
     */
    public void release() {
        int ret = cl.clReleaseKernel(ID);
        program.onKernelReleased(this);
        if(ret != CL.CL_SUCCESS) {
            throw newException(ret, "can not release "+this);
        }
    }

    @Override
    public String toString() {
        return "CLKernel [id: " + ID
                      + " name: " + name+"]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CLKernel other = (CLKernel) obj;
        if (this.ID != other.ID) {
            return false;
        }
        if (!this.program.equals(other.program)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + (int) (this.ID ^ (this.ID >>> 32));
        hash = 43 * hash + (this.program != null ? this.program.hashCode() : 0);
        return hash;
    }

    /**
     * Returns a new instance of this kernel with uninitialized arguments.
     */
    @Override
    public CLKernel clone() {
        return program.createCLKernel(name).setForce32BitArgs(force32BitArgs);
    }

}
