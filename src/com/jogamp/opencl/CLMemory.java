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

import com.jogamp.opencl.gl.CLGLI;
import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.NativeSizeBuffer;
import com.jogamp.opencl.impl.CLMemObjectDestructorCallback;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.jogamp.opencl.CLException.*;
import static com.jogamp.opencl.gl.CLGLI.*;

/**
 * Common superclass for all OpenCL memory types.
 * Represents an OpenCL memory object and wraps an optional NIO buffer.
 * @author Michael Bien
 */
public abstract class CLMemory <B extends Buffer> extends CLObject implements CLResource {
    
    B buffer;
    protected final int FLAGS;
    protected long size;
    
    // depends on the nio buffer type
    protected int elementSize;
    protected int clCapacity;
    
    protected <Buffer> CLMemory(CLContext context, long size, long id, int flags) {
        this(context, null, size, id, flags);
    }
    
    protected CLMemory(CLContext context, B directBuffer, long size, long id, int flags) {
        super(context, id);
        this.buffer = directBuffer;
        this.FLAGS = flags;
        this.size = size;
        initElementSize();
        initCLCapacity();
    }

    private void initElementSize() {
        this.elementSize = (buffer==null) ? 1 : Buffers.sizeOfBufferElem(buffer);
    }

    protected final void initCLCapacity() {
        this.clCapacity  = (int) (size / elementSize);
    }

    /**
     * Returns true if a host pointer must be specified on mem object creation.
     */
    protected static boolean isHostPointerFlag(int flags) {
        return (flags & CL_MEM_COPY_HOST_PTR) != 0
            || (flags & CL_MEM_USE_HOST_PTR)  != 0;
    }

    protected static long getSizeImpl(CL cl, long id) {
        NativeSizeBuffer pb = NativeSizeBuffer.allocateDirect(1);
        int ret = cl.clGetMemObjectInfo(id, CL_MEM_SIZE, NativeSizeBuffer.elementSize(), pb.getBuffer(), null);
        checkForError(ret, "can not obtain buffer info");
        return pb.get();
    }

    protected static CL getCL(CLContext context) {
        return context.cl;
    }

    /**
     * Registers a callback which will be called by the OpenCL implementation
     * when the memory object is released.
     */
    public void registerDestructorCallback(final CLMemObjectListener listener) {
        cl.clSetMemObjectDestructorCallback(ID, new CLMemObjectDestructorCallback() {
            @Override
            public void memoryDeallocated(long memObjID) {
                listener.memoryDeallocated(CLMemory.this);
            }
        });
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
        initElementSize();
        initCLCapacity();
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
    public int getNIOCapacity() {
        if(buffer == null) {
            return 0;
        }
        return buffer.capacity();
    }

    /**
     * Returns the size of the wrapped direct buffer in byte or 0 if no buffer available.
     */
    public int getNIOSize() {
        if(buffer == null) {
            return 0;
        }
        return getElementSize() * buffer.capacity();
    }

    /**
     * Returns the size of the allocated OpenCL memory in bytes.
     */
    public long getCLSize() {
        return size;
    }

    /**
     * Returns the size in buffer elements of this memory object.
     */
    public int getCLCapacity() {
        return clCapacity;
    }
    
    /**
     * Returns the size in bytes of a single buffer element.
     * This method returns 1 if no buffer is available indicating regular byte access.
     */
    public int getElementSize() {
        return elementSize;
    }

    /**
     * Returns the configuration of this memory object.
     */
    public EnumSet<Mem> getConfig() {
        return Mem.valuesOf(FLAGS);
    }

    /**
     * Returns the number of buffer mappings. The map count returned should be considered immediately stale.
     * It is unsuitable for general use in applications. This feature is provided for debugging.
     */
    public int getMapCount() {
        IntBuffer value = Buffers.newDirectIntBuffer(1);
        int ret = cl.clGetMemObjectInfo(ID, CL_MEM_MAP_COUNT, 4, value, null);
        checkForError(ret, "can not obtain buffer map count.");
        return value.get();
    }

    /**
     * Returns true if this memory object was created with the {@link Mem#READ_ONLY} flag.
     */
    public boolean isReadOnly() {
        return (Mem.READ_ONLY.CONFIG & FLAGS) != 0;
    }

    /**
     * Returns true if this memory object was created with the {@link Mem#WRITE_ONLY} flag.
     */
    public boolean isWriteOnly() {
        return (Mem.WRITE_ONLY.CONFIG & FLAGS) != 0;
    }

    /**
     * Returns true if this memory object was created with the {@link Mem#READ_WRITE} flag.
     */
    public boolean isReadWrite() {
        return (Mem.READ_WRITE.CONFIG & FLAGS) != 0;
    }

    @Override
    public void release() {
        int ret = cl.clReleaseMemObject(ID);
        context.onMemoryReleased(this);
        if(ret != CL_SUCCESS) {
            throw newException(ret, "can not release "+this);
        }
    }

    // TODO kept only temporary for debugging purposes
    /**
     * Returns the OpenGL buffer type of this shared buffer.
     */
    @Deprecated
    /*public*/ final GLObjectType _getGLObjectType() {
        int[] array = new int[1];
        int ret = ((CLGLI)cl).clGetGLObjectInfo(ID, array, 0, null, 0);
        CLException.checkForError(ret, "error while asking for gl object info");
        return GLObjectType.valueOf(array[0]);
    }

    /**
     * Returns the OpenGL object id of this shared buffer.
     */
    @Deprecated
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
        return getClass().getSimpleName()+" [id: " + ID+" buffer: "+buffer+"]";
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

        public static EnumSet<Mem> valuesOf(int bitfield) {
            List<Mem> matching = new ArrayList<Mem>();
            Mem[] values = Mem.values();
            for (Mem value : values) {
                if((value.CONFIG & bitfield) != 0)
                    matching.add(value);
            }
            if(matching.isEmpty())
                return EnumSet.noneOf(Mem.class);
            else
                return EnumSet.copyOf(matching);
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
     * @see com.jogamp.opencl.CLCommandQueue#putMapBuffer(CLBuffer, com.jogamp.opencl.CLMemory.Map, boolean).
     * @see com.jogamp.opencl.CLCommandQueue#putMapImage(CLImage2d, com.jogamp.opencl.CLMemory.Map, boolean)
     * @see com.jogamp.opencl.CLCommandQueue#putMapImage(CLImage3d, com.jogamp.opencl.CLMemory.Map, boolean)
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
