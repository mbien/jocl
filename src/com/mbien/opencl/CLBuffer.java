package com.mbien.opencl;

import java.nio.ByteBuffer;
import static com.mbien.opencl.CLException.*;

/**
 *
 * @author Michael Bien
 */
public class CLBuffer {

    public enum MEM {

        /**
         * This flag specifies that the memory object will be read and
         * written by a kernel.
         */
        READ_WRITE(CL.CL_MEM_READ_WRITE),

        /**
         * This flags specifies that the memory object will be written
         * but not read by a kernel.
         * Reading from a buffer or image object created with WRITE_ONLY
         * inside a kernel is undefined.
         */
        WRITE_ONLY(CL.CL_MEM_WRITE_ONLY),

        /**
         * This flag specifies that the memory object is a read-only memory
         * object when used inside a kernel. Writing to a buffer or image object
         * created withREAD_ONLY inside a kernel is undefined.
         */
        READ_ONLY(CL.CL_MEM_READ_ONLY);

        /**
         * If specified, it indicates that the application wants the OpenCL
         * implementation to use memory referenced by host_ptr as the storage
         * bits for the memory object. OpenCL implementations are allowed
         * to cache the buffer contents pointed to by host_ptr in device memory.
         * This cached copy can be used when kernels are executed on a device.
         */
//        USE_HOST_PTR(CL.CL_MEM_USE_HOST_PTR),

//        ALLOC_HOST_PTR(CL.CL_MEM_ALLOC_HOST_PTR), // this is the default in java world anyway

        /**
         * If CL_MEM_COPY_HOST_PTR specified, it indicates that the application
         * wants the OpenCL implementation to allocate memory for the memory object
         * and copy the data from memory referenced by host_ptr.<br/>
         * COPY_HOST_PTR and USE_HOST_PTR are mutually exclusive.
         */
//        COPY_HOST_PTR(CL.CL_MEM_COPY_HOST_PTR);

        /**
         * Value of wrapped OpenCL flag.
         */
        public final int CL_FLAG;

        private MEM(int CL_TYPE) {
            this.CL_FLAG = CL_TYPE;
        }

        public static MEM valueOf(int bufferFlag) {
            switch(bufferFlag) {
                case(CL.CL_MEM_READ_WRITE):
                    return READ_WRITE;
                case(CL.CL_MEM_READ_ONLY):
                    return READ_ONLY;
//                case(CL.CL_MEM_USE_HOST_PTR):
//                    return USE_HOST_PTR;
//                case(CL.CL_MEM_ALLOC_HOST_PTR):
//                    return ALLOC_HOST_PTR;
//                case(CL.CL_MEM_COPY_HOST_PTR):
//                    return COPY_HOST_PTR;
            }
            return null;
        }

        static int flagsToInt(MEM[] flags) {
            int clFlags = CL.CL_MEM_READ_WRITE;
            if(flags != null) {
                for (int i = 0; i < flags.length; i++) {
                    clFlags |= flags[i].CL_FLAG;
                }
            }
            return clFlags;
        }

    }


    public final ByteBuffer buffer;
    public final long ID;
    
    private final CLContext context;
    private final CL cl;

    CLBuffer(CLContext context, ByteBuffer directBuffer, int flags) {
        
        if(!directBuffer.isDirect())
            throw new IllegalArgumentException("buffer is not a direct buffer");

        this.buffer = directBuffer;
        this.context = context;
        this.cl = context.cl;

        int[] intArray = new int[1];

        this.ID = cl.clCreateBuffer(context.ID, flags, directBuffer.capacity(), null, intArray, 0);

        checkForError(intArray[0], "can not create cl buffer");
        
    }

    public void release() {
        int ret = cl.clReleaseMemObject(ID);
        context.onBufferReleased(this);
        checkForError(ret, "can not release mem object");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CLBuffer other = (CLBuffer) obj;
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




}
