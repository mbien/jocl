package com.mbien.opencl;

import com.mbien.opencl.gl.CLGLI;
import com.sun.gluegen.runtime.BufferFactory;
import com.sun.gluegen.runtime.PointerBuffer;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static com.mbien.opencl.CLException.*;
import static com.mbien.opencl.gl.CLGLI.*;

/**
 * Common superclass for all OpenCL memory types.
 * @author Michael Bien
 */
public abstract class CLMemory <B extends Buffer> extends CLObject implements CLResource {
    
    B buffer;
    
    protected <Buffer> CLMemory(CLContext context, long id) {
        super(context, id);
    }
    
    protected CLMemory(CLContext context, B directBuffer, long id) {
        super(context, id);
        this.buffer = directBuffer;
    }

    /**
     * Returns true if a host pointer must be specified on mem object creation.
     */
    protected static boolean isHostPointerFlag(int flags) {
        return (flags & CL_MEM_COPY_HOST_PTR) != 0
            || (flags & CL_MEM_USE_HOST_PTR)  != 0;
    }

    protected static int sizeOfBufferElem(Buffer buffer) {
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

    protected static CL getCL(CLContext context) {
        return context.cl;
    }

    /**
     * Returns a new instance of CLMemory pointing to the same CLResource but using a different Buffer.
     */
    public abstract <T extends Buffer> CLMemory<T> cloneWith(T directBuffer);


    public CLMemory<B> use(B buffer) {
        if(this.buffer != null && buffer != null && this.buffer.getClass() != buffer.getClass()) {
            throw new IllegalArgumentException(
                    "expected a Buffer of class " + this.buffer.getClass()
                    +" but got " + buffer.getClass());
        }
        this.buffer = buffer;
        return this;
    }

    /**
     * Returns the optional NIO buffer for this memory object.
     */
    public B getBuffer() {
        return buffer;
    }

    /**
     * Returns the capacity of the wrapped direct buffer or 0 if no buffer available.
     */
    public int getCapacity() {
        if(buffer == null) {
            return 0;
        }
        return buffer.capacity();
    }

    /**
     * Returns the size of the wrapped direct buffer in byte or 0 if no buffer available.
     */
    public int getSize() {
        if(buffer == null) {
            return 0;
        }
        return sizeOfBufferElem(buffer) * buffer.capacity();
    }

    /**
     * Returns the size of the allocated OpenCL memory.
     */
    public long getCLSize() {
        PointerBuffer pb = PointerBuffer.allocateDirect(1);
        int ret = cl.clGetMemObjectInfo(ID, CL_MEM_SIZE, PointerBuffer.elementSize(), pb.getBuffer(), null);
        checkForError(ret, "can not obtain buffer info");
        return pb.get();
    }

    public void release() {
        int ret = cl.clReleaseMemObject(ID);
        context.onMemoryReleased(this);
        checkForError(ret, "can not release mem object");
    }

    public void close() {
        release();
    }

    // kept only for debugging purposes
    /**
     * Returns the OpenGL buffer type of this shared buffer.
     */
    /*public*/ final GLObjectType _getGLObjectType() {
        int[] array = new int[1];
        int ret = ((CLGLI)cl).clGetGLObjectInfo(ID, array, 0, null, 0);
        CLException.checkForError(ret, "error while asking for gl object info");
        return GLObjectType.valueOf(array[0]);
    }

    /**
     * Returns the OpenGL object id of this shared buffer.
     */
    /*public*/ final int _getGLObjectID() {
        int[] array = new int[1];
        int ret = ((CLGLI)cl).clGetGLObjectInfo(ID, null, 0, array, 0);
        CLException.checkForError(ret, "error while asking for gl object info");
        return array[0];
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CLMemory<?> other = (CLMemory<?>) obj;
        if (this.ID != other.ID) {
            return false;
        }
        if (this.context != other.context && (this.context == null || !this.context.equals(other.context))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + (int) (this.ID ^ (this.ID >>> 32));
        hash = 83 * hash + (this.context != null ? this.context.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "CLMemory [id: " + ID+"]";
    }

    /**
     * Memory settings for configuring CLMemory.
     */
    public enum Mem {

        /**
         * Enum representing CL_MEM_READ_WRITE.
         * This flag specifies that the memory object will be read and
         * written by a kernel.
         */
        READ_WRITE(CL_MEM_READ_WRITE),

        /**
         * Enum representing CL_MEM_WRITE_ONLY.
         * This flags specifies that the memory object will be written
         * but not read by a kernel.
         * Reading from a buffer or image object created with WRITE_ONLY
         * inside a kernel is undefined.
         */
        WRITE_ONLY(CL_MEM_WRITE_ONLY),

        /**
         * Enum representing CL_MEM_READ_ONLY.
         * This flag specifies that the memory object is a read-only memory
         * object when used inside a kernel. Writing to a buffer or image object
         * created withREAD_ONLY inside a kernel is undefined.
         */
        READ_ONLY(CL_MEM_READ_ONLY),

        /**
         * Enum representing CL_MEM_USE_HOST_PTR.
         * If specified, it indicates that the application wants the OpenCL
         * implementation to use memory referenced by host_ptr as the storage
         * bits for the memory object. OpenCL implementations are allowed
         * to cache the buffer contents pointed to by host_ptr in device memory.
         * This cached copy can be used when kernels are executed on a device.
         */
        USE_BUFFER(CL_MEM_USE_HOST_PTR),

        /**
         * Enum representing CL_MEM_ALLOC_HOST_PTR.
         * This flag specifies that the application wants the OpenCL implementation
         * to allocate memory from host accessible memory.
         * {@link #ALLOCATE_BUFFER} and {@link #USE_BUFFER} are mutually exclusive.
         */
        ALLOCATE_BUFFER(CL_MEM_ALLOC_HOST_PTR),

        /**
         * Enum representing CL_MEM_COPY_HOST_PTR.
         * If {@link #COPY_BUFFER} specified, it indicates that the application
         * wants the OpenCL implementation to allocate memory for the memory object
         * and copy the data from memory referenced by host_ptr.<br/>
         * {@link #COPY_BUFFER} and {@link #USE_BUFFER} are mutually exclusive.
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
            switch (bufferFlag) {
                case CL_MEM_READ_WRITE:
                    return Mem.READ_WRITE;
                case CL_MEM_READ_ONLY:
                    return Mem.READ_ONLY;
                case CL_MEM_WRITE_ONLY:
                    return Mem.WRITE_ONLY;
                case CL_MEM_USE_HOST_PTR:
                    return Mem.USE_BUFFER;
                case(CL_MEM_ALLOC_HOST_PTR):
                    return ALLOCATE_BUFFER;
                case CL_MEM_COPY_HOST_PTR:
                    return Mem.COPY_BUFFER;
            }
            return null;
        }

        public static int flagsToInt(Mem[] flags) {
            int clFlags = 0;
            if (flags != null) {
                for (int i = 0; i < flags.length; i++) {
                    clFlags |= flags[i].CONFIG;
                }
            }
            if (clFlags == 0) {
                clFlags = CL_MEM_READ_WRITE;
            }
            return clFlags;
        }
    }

    /**
     * Configures the mapping process.
     * @see com.mbien.opencl.CLCommandQueue#putMapBuffer(CLBuffer, com.mbien.opencl.CLMemory.Map, boolean).
     * @see com.mbien.opencl.CLCommandQueue#putMapImage(CLImage2d, com.mbien.opencl.CLMemory.Map, boolean)
     * @see com.mbien.opencl.CLCommandQueue#putMapImage(CLImage3d, com.mbien.opencl.CLMemory.Map, boolean)
     */
    public enum Map {

        /**
         * Enum representing CL_MAP_READ | CL_MAP_WRITE.
         * This flag specifies that the memory object will be mapped for read and write operation.
         */
        READ_WRITE(CL_MAP_READ | CL_MAP_WRITE),

        /**
         * Enum representing CL_MAP_WRITE.
         * This flag specifies that the memory object will be mapped for write operation.
         */
        WRITE(CL_MAP_WRITE),

        /**
         * Enum representing CL_MAP_READ.
         * This flag specifies that the memory object will be mapped for read operation.
         */
        READ(CL_MAP_READ);

        /**
         * Value of wrapped OpenCL flag.
         */
        public final int FLAGS;

        private Map(int flags) {
            this.FLAGS = flags;
        }

        public Map valueOf(int flag) {
            if(flag == WRITE.FLAGS)
                return WRITE;
            else if(flag == READ.FLAGS)
                return READ;
            else if(flag == READ_WRITE.FLAGS)
                return READ_WRITE;
            return null;
        }

    }

    public enum GLObjectType {

        GL_OBJECT_BUFFER(CL_GL_OBJECT_BUFFER),
        GL_OBJECT_TEXTURE2D(CL_GL_OBJECT_TEXTURE2D),
        GL_OBJECT_TEXTURE3D(CL_GL_OBJECT_TEXTURE3D),
        GL_OBJECT_RENDERBUFFER(CL_GL_OBJECT_RENDERBUFFER);

        public final int TYPE;

        private GLObjectType(int type) {
            this.TYPE = type;
        }

        public static GLObjectType valueOf(int type) {
            if(type == CL_GL_OBJECT_BUFFER)
                return GL_OBJECT_BUFFER;
            else if(type == CL_GL_OBJECT_TEXTURE2D)
                return GL_OBJECT_TEXTURE2D;
            else if(type == CL_GL_OBJECT_TEXTURE3D)
                return GL_OBJECT_TEXTURE3D;
            else if(type == CL_GL_OBJECT_RENDERBUFFER)
                return GL_OBJECT_RENDERBUFFER;
            return null;
        }
    }

}
