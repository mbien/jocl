package com.mbien.opencl;

import com.sun.gluegen.runtime.BufferFactory;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static com.mbien.opencl.CLException.*;
import static com.mbien.opencl.CL.*;

/**
 *
 * @author Michael Bien
 */
public class CLBuffer<B extends Buffer> implements CLResource {

    public final B buffer;
    public final long ID;
    
    private final CLContext context;
    private final CL cl;

    CLBuffer(CLContext context, B directBuffer, int flags) {
        this(context, directBuffer, 0, flags);
    }

    CLBuffer(CLContext context, B directBuffer, int glBuffer, int flags) {

        if(!directBuffer.isDirect())
            throw new IllegalArgumentException("buffer is not a direct buffer");

        this.buffer = directBuffer;
        this.context = context;
        this.cl = context.cl;

        int[] result = new int[1];

        if(glBuffer == 0) {
            B host_ptr = null;
            if(isHostPointerFlag(flags)) {
                host_ptr = directBuffer;
            }
            this.ID = cl.clCreateBuffer(context.ID, flags,
                    sizeOfBufferElem(directBuffer)*directBuffer.capacity(), host_ptr, result, 0);
        }else{
            if(isHostPointerFlag(flags)) {
                throw new IllegalArgumentException(
                        "CL_MEM_COPY_HOST_PTR or CL_MEM_USE_HOST_PTR can not be used with OpenGL Buffers.");
            }
            CLGLI clgli = (CLGLI)cl;
            this.ID = clgli.clCreateFromGLBuffer(context.ID, flags, glBuffer, result, 0);
        }
        checkForError(result[0], "can not create cl buffer");

    }

    private final boolean isHostPointerFlag(int flags) {
        return (flags & CL_MEM_COPY_HOST_PTR) != 0 || (flags & CL_MEM_USE_HOST_PTR) != 0;
    }

    public void release() {
        int ret = cl.clReleaseMemObject(ID);
        context.onBufferReleased(this);
        checkForError(ret, "can not release mem object");
    }

    //stolen from JOGL project... think about merging
    private final int sizeOfBufferElem(Buffer buffer) {

        if (buffer instanceof ByteBuffer) {
            return BufferFactory.SIZEOF_BYTE;
        } else if (buffer instanceof IntBuffer) {
            return BufferFactory.SIZEOF_INT;
        } else if (buffer instanceof ShortBuffer) {
            return BufferFactory.SIZEOF_SHORT;
        } else if (buffer instanceof FloatBuffer) {
            return BufferFactory.SIZEOF_FLOAT;
        } else if (buffer instanceof DoubleBuffer) {
            return BufferFactory.SIZEOF_DOUBLE;
        }
        throw new RuntimeException("Unexpected buffer type " + buffer.getClass().getName());
    }

    int getSizeInBytes() {
        return sizeOfBufferElem(buffer)*buffer.capacity();
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CLBuffer<?> other = (CLBuffer<?>) obj;
        if (this.buffer != other.buffer && (this.buffer == null || !this.buffer.equals(other.buffer))) {
            return false;
        }
        if (this.context.ID != other.context.ID) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + (this.buffer != null ? this.buffer.hashCode() : 0);
        hash = 29 * hash + (int) (this.context.ID ^ (this.context.ID >>> 32));
        return hash;
    }

    /**
     * Memory settings for configuring CLBuffers.
     */
    public enum Mem {

        /**
         * This flag specifies that the memory object will be read and
         * written by a kernel.
         */
        READ_WRITE(CL_MEM_READ_WRITE),

        /**
         * This flags specifies that the memory object will be written
         * but not read by a kernel.
         * Reading from a buffer or image object created with WRITE_ONLY
         * inside a kernel is undefined.
         */
        WRITE_ONLY(CL_MEM_WRITE_ONLY),

        /**
         * This flag specifies that the memory object is a read-only memory
         * object when used inside a kernel. Writing to a buffer or image object
         * created withREAD_ONLY inside a kernel is undefined.
         */
        READ_ONLY(CL_MEM_READ_ONLY),

        /**
         * Enum representing CL.CL_MEM_USE_HOST_PTR.
         * If specified, it indicates that the application wants the OpenCL
         * implementation to use memory referenced by host_ptr as the storage
         * bits for the memory object. OpenCL implementations are allowed
         * to cache the buffer contents pointed to by host_ptr in device memory.
         * This cached copy can be used when kernels are executed on a device.
         */
        USE_BUFFER(CL_MEM_USE_HOST_PTR),

//        ALLOC_HOST_PTR(CL_MEM_ALLOC_HOST_PTR), // this is the default in java world anyway

        /**
         * Enum representing CL.CL_MEM_COPY_HOST_PTR.
         * If CL_MEM_COPY_HOST_PTR specified, it indicates that the application
         * wants the OpenCL implementation to allocate memory for the memory object
         * and copy the data from memory referenced by host_ptr.<br/>
         * COPY_HOST_PTR and USE_HOST_PTR are mutually exclusive.
         */
        COPY_BUFFER(CL_MEM_COPY_HOST_PTR);

        /**
         * Value of wrapped OpenCL flag.
         */
        public final int CONFIG;

        private Mem(int config) {
            this.CONFIG = config;
        }

        public static Mem valueOf(int bufferFlag) {
            switch(bufferFlag) {
                case(CL_MEM_READ_WRITE):
                    return READ_WRITE;
                case(CL_MEM_READ_ONLY):
                    return READ_ONLY;
                case(CL_MEM_USE_HOST_PTR):
                    return USE_BUFFER;
//                case(CL_MEM_ALLOC_HOST_PTR):
//                    return ALLOC_HOST_PTR;
                case(CL_MEM_COPY_HOST_PTR):
                    return COPY_BUFFER;
            }
            return null;
        }

        static int flagsToInt(Mem[] flags) {
            int clFlags = 0;
            if(flags != null) {
                for (int i = 0; i < flags.length; i++) {
                    clFlags |= flags[i].CONFIG;
                }
            }
            if(clFlags == 0)
                clFlags = CL_MEM_READ_WRITE;
            return clFlags;
        }

    }

}
