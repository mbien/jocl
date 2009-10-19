package com.mbien.opencl;

import com.sun.gluegen.runtime.BufferFactory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import static com.mbien.opencl.CLException.*;

/**
 *
 * @author Michael Bien
 */
public class CLKernel {

    public final long kernelID;
    public final String name;

    private final CLProgram program;
    private final CL cl;

    CLKernel(CLProgram program, long id) {
        this.kernelID = id;
        this.program = program;
        this.cl = program.context.cl;

        long[] longArray = new long[1];

        int ret = cl.clGetKernelInfo(kernelID, CL.CL_KERNEL_FUNCTION_NAME, 0, null, longArray, 0);
        checkForError(ret, "error while asking for kernel function name");

        ByteBuffer bb = ByteBuffer.allocate((int)longArray[0]).order(ByteOrder.nativeOrder());

        ret = cl.clGetKernelInfo(kernelID, CL.CL_KERNEL_FUNCTION_NAME, bb.capacity(), bb, null, 0);
        checkForError(ret, "error while asking for kernel function name");

        this.name = new String(bb.array(), 0, (int)longArray[0]).trim();

    }

    public CLKernel setArg(int argumentIndex, int argumentSize, CLBuffer value) {
        int ret = cl.clSetKernelArg(kernelID, argumentIndex, argumentSize, wrapLong(value.bufferID));
        checkForError(ret, "error on clSetKernelArg");
        return this;
    }

    public CLKernel setArg(int argumentIndex, int argumentSize, long value) {
        int ret = cl.clSetKernelArg(kernelID, argumentIndex, argumentSize, wrapLong(value));
        checkForError(ret, "error on clSetKernelArg");
        return this;
    }

    private final ByteBuffer wrapLong(long value) {
        return (ByteBuffer) BufferFactory.newDirectByteBuffer(8).putLong(value).rewind();
    }

    public CLKernel release() {
        cl.clReleaseKernel(kernelID);
        program.kernelReleased(this);
        return this;
    }

    @Override
    public String toString() {
        return "CLKernel [id: " + kernelID
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
        if (this.kernelID != other.kernelID) {
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
        hash = 43 * hash + (int) (this.kernelID ^ (this.kernelID >>> 32));
        hash = 43 * hash + (this.program != null ? this.program.hashCode() : 0);
        return hash;
    }

}
