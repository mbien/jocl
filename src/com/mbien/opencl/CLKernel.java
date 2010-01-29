package com.mbien.opencl;

import com.sun.gluegen.runtime.BufferFactory;
import com.sun.gluegen.runtime.CPU;
import com.sun.gluegen.runtime.PointerBuffer;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.mbien.opencl.CLException.*;
import static com.mbien.opencl.CL.*;

/**
 * High level abstraction for an OpenCL Kernel.
 * CLKernel is not threadsafe.
 * "A kernel is a function declared in a program. A kernel is identified by the <code>kernel</code> qualifier
 * applied to any function in a program. A kernel object encapsulates the specific <code>kernel</code>
 * function declared in a program and the argument values to be used when executing this
 * <code>kernel</code> function."
 * @author Michael Bien
 */
public class CLKernel implements CLResource/*, Cloneable*/ {

    public final long ID;
    public final String name;
    public final int numArgs;

    private final CLProgram program;
    private final CL cl;

    private final ByteBuffer buffer;

    private int argIndex;

    CLKernel(CLProgram program, long id) {
        this.ID = id;
        this.program = program;
        this.cl = program.context.cl;
        this.buffer = BufferFactory.newDirectByteBuffer(8);

        PointerBuffer pb = PointerBuffer.allocateDirect(1);

        // get function name
        int ret = cl.clGetKernelInfo(ID, CL_KERNEL_FUNCTION_NAME, 0, null, pb);
        checkForError(ret, "error while asking for kernel function name");

        ByteBuffer bb = ByteBuffer.allocateDirect((int)pb.get(0)).order(ByteOrder.nativeOrder());

        ret = cl.clGetKernelInfo(ID, CL_KERNEL_FUNCTION_NAME, bb.capacity(), bb, null);
        checkForError(ret, "error while asking for kernel function name");

        this.name = CLUtils.clString2JavaString(bb, bb.capacity());

        // get number of arguments
        ret = cl.clGetKernelInfo(ID, CL_KERNEL_NUM_ARGS, bb.capacity(), bb, null);
        checkForError(ret, "error while asking for number of function arguments.");

        numArgs = bb.getInt(0);

    }
    
    public CLKernel putArg(CLMemory<?> value) {
        setArg(argIndex++, value);
        return this;
    }

    public CLKernel putArg(int value) {
        setArg(argIndex++, value);
        return this;
    }

    public CLKernel putArg(long value) {
        setArg(argIndex++, value);
        return this;
    }

    public CLKernel putArg(float value) {
        setArg(argIndex++, value);
        return this;
    }

    public CLKernel putArg(double value) {
        setArg(argIndex++, value);
        return this;
    }

    public CLKernel putArgs(CLMemory<?>... values) {
        setArgs(argIndex, values);
        argIndex += values.length;
        return this;
    }

    public CLKernel setArg(int argumentIndex, CLMemory<?> value) {
        setArgument(argumentIndex, CPU.is32Bit()?4:8, wrap(value.ID));
        return this;
    }

    public CLKernel setArg(int argumentIndex, int value) {
        setArgument(argumentIndex, 4, wrap(value));
        return this;
    }

    public CLKernel setArg(int argumentIndex, long value) {
        setArgument(argumentIndex, 8, wrap(value));
        return this;
    }

    public CLKernel setArg(int argumentIndex, float value) {
        setArgument(argumentIndex, 4, wrap(value));
        return this;
    }

    public CLKernel setArg(int argumentIndex, double value) {
        setArgument(argumentIndex, 8, wrap(value));
        return this;
    }

    public CLKernel setArgs(CLMemory<?>... values) {
        setArgs(0, values);
        return this;
    }

    private final void setArgs(int startIndex, CLMemory<?>... values) {
        for (int i = 0; i < values.length; i++) {
            setArg(i+startIndex, values[i]);
        }
    }

    private final void setArgument(int argumentIndex, int size, Buffer value) {
        if(argumentIndex >= numArgs || argumentIndex < 0) {
            throw new IndexOutOfBoundsException("kernel "+ toString() +" has "+numArgs+
                    " arguments, can not set argument with index "+argumentIndex);
        }
        if(!program.isExecutable()) {
            throw new IllegalStateException("can not set program" +
                    " arguments for a not excecutable program. "+program);
        }

        int ret = cl.clSetKernelArg(ID, argumentIndex, size, value);
        checkForError(ret, "error on clSetKernelArg");
    }

    private final Buffer wrap(float value) {
        return buffer.putFloat(value).rewind();
    }

    private final Buffer wrap(double value) {
        return buffer.putDouble(value).rewind();
    }

    private final Buffer wrap(int value) {
        return buffer.putInt(value).rewind();
    }

    private final Buffer wrap(long value) {
        return buffer.putLong(value).rewind();
    }

    public CLKernel rewind() {
        argIndex = 0;
        return this;
    }

    /**
     * Releases all resources of this kernel from its context.
     */
    public void release() {
        int ret = cl.clReleaseKernel(ID);
        program.onKernelReleased(this);
        checkForError(ret, "can not release kernel");
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
     * Returns a new instance of this kernel.
     */
//    @Override
//    public CLKernel clone() {
//        return program.createCLKernel(name);
//    }

}
