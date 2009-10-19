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

    public final long ID;
    public final String name;

    private final CLProgram program;
    private final CL cl;

    CLKernel(CLProgram program, long id) {
        this.ID = id;
        this.program = program;
        this.cl = program.context.cl;

        long[] longArray = new long[1];

        int ret = cl.clGetKernelInfo(ID, CL.CL_KERNEL_FUNCTION_NAME, 0, null, longArray, 0);
        checkForError(ret, "error while asking for kernel function name");

        ByteBuffer bb = ByteBuffer.allocate((int)longArray[0]).order(ByteOrder.nativeOrder());

        ret = cl.clGetKernelInfo(ID, CL.CL_KERNEL_FUNCTION_NAME, bb.capacity(), bb, null, 0);
        checkForError(ret, "error while asking for kernel function name");

        this.name = new String(bb.array(), 0, (int)longArray[0]).trim();

    }

    public CLKernel setArg(int argumentIndex, int argumentSize, CLBuffer value) {
        int ret = cl.clSetKernelArg(ID, argumentIndex, argumentSize, wrapLong(value.ID));
        checkForError(ret, "error on clSetKernelArg");
        return this;
    }

    public CLKernel setArg(int argumentIndex, int argumentSize, long value) {
        int ret = cl.clSetKernelArg(ID, argumentIndex, argumentSize, wrapLong(value));
        checkForError(ret, "error on clSetKernelArg");
        return this;
    }

    private final ByteBuffer wrapLong(long value) {
        return (ByteBuffer) BufferFactory.newDirectByteBuffer(8).putLong(value).rewind();
    }

    public void release() {
        int ret = cl.clReleaseKernel(ID);
        program.kernelReleased(this);
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
