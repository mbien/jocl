package com.mbien.opencl;

import com.sun.gluegen.runtime.BufferFactory;
import com.sun.gluegen.runtime.CPU;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import static com.mbien.opencl.CLException.*;

/**
 * High level abstraction for an OpenCL Kernel.
 * "A kernel is a function declared in a program. A kernel is identified by the __kernel qualifier
 * applied to any function in a program. A kernel object encapsulates the specific __kernel
 * function declared in a program and the argument values to be used when executing this
 * __kernel function."
 * @author Michael Bien
 */
public class CLKernel implements CLResource {

    public final long ID;
    public final String name;
    public final int numArgs;

    private final CLProgram program;
    private final CL cl;

    CLKernel(CLProgram program, long id) {
        this.ID = id;
        this.program = program;
        this.cl = program.context.cl;

        long[] longArray = new long[1];

        // get function name
        int ret = cl.clGetKernelInfo(ID, CL.CL_KERNEL_FUNCTION_NAME, 0, null, longArray, 0);
        checkForError(ret, "error while asking for kernel function name");

        ByteBuffer bb = ByteBuffer.allocate((int)longArray[0]).order(ByteOrder.nativeOrder());

        ret = cl.clGetKernelInfo(ID, CL.CL_KERNEL_FUNCTION_NAME, bb.capacity(), bb, null, 0);
        checkForError(ret, "error while asking for kernel function name");

        this.name = new String(bb.array(), 0, bb.capacity()).trim();

        // get number of arguments
        ret = cl.clGetKernelInfo(ID, CL.CL_KERNEL_NUM_ARGS, 0, null, longArray, 0);
        checkForError(ret, "error while asking for number of function arguments.");

        numArgs = (int)longArray[0];

    }

    public CLKernel setArg(int argumentIndex, CLBuffer<?> value) {
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

    private final void setArgument(int argumentIndex, int size, Buffer value) {
        if(argumentIndex >= numArgs || argumentIndex < 0) {
            throw new IndexOutOfBoundsException("kernel "+ toString() +" has "+numArgs+
                    " arguments, can not set argument number "+argumentIndex);
        }
        if(!program.isExecutable()) {
            throw new IllegalStateException("can not set program" +
                    " arguments for a not excecutable program. "+program);
        }

        int ret = cl.clSetKernelArg(ID, argumentIndex, size, value);
        checkForError(ret, "error on clSetKernelArg");
    }

    private final Buffer wrap(float value) {
        return BufferFactory.newDirectByteBuffer(4).putFloat(value).rewind();
    }

    private final Buffer wrap(double value) {
        return BufferFactory.newDirectByteBuffer(8).putDouble(value).rewind();
    }

    private final Buffer wrap(int value) {
        return BufferFactory.newDirectByteBuffer(4).putInt(value).rewind();
    }

    private final Buffer wrap(long value) {
        return BufferFactory.newDirectByteBuffer(8).putLong(value).rewind();
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

}
