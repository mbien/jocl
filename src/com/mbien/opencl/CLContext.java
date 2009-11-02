package com.mbien.opencl;

import com.mbien.opencl.CLBuffer.Mem;
import com.sun.gluegen.runtime.BufferFactory;
import com.sun.gluegen.runtime.CPU;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.mbien.opencl.CLException.*;

/**
 * CLContext is responsible for managing objects such as command-queues, memory,
 * program and kernel objects and for executing kernels on one or more devices
 * specified in the context.
 * @author Michael Bien
 */
public final class CLContext {

    final CL cl;
    public final long ID;

    private CLDevice[] devices;

    private final List<CLProgram> programs;
    private final List<CLBuffer<? extends Buffer>> buffers;
    private final Map<CLDevice, List<CLCommandQueue>> queuesMap;


    private CLContext(long contextID) {
        this.cl = CLPlatform.getLowLevelBinding();
        this.ID = contextID;
        this.programs = new ArrayList<CLProgram>();
        this.buffers = new ArrayList<CLBuffer<? extends Buffer>>();
        this.queuesMap = new HashMap<CLDevice, List<CLCommandQueue>>();
    }

    private final void initDevices() {
        
        if (devices == null) {

            int sizeofDeviceID = CPU.is32Bit() ? 4 : 8;
            long[] longBuffer = new long[1];

            int ret = cl.clGetContextInfo(ID, CL.CL_CONTEXT_DEVICES, 0, null, longBuffer, 0);
            checkForError(ret, "can not enumerate devices");

            ByteBuffer deviceIDs = ByteBuffer.allocate((int) longBuffer[0]).order(ByteOrder.nativeOrder());
            ret = cl.clGetContextInfo(ID, CL.CL_CONTEXT_DEVICES, deviceIDs.capacity(), deviceIDs, null, 0);
            checkForError(ret, "can not enumerate devices");

            devices = new CLDevice[deviceIDs.capacity() / sizeofDeviceID];
            for (int i = 0; i < devices.length; i++) {
                devices[i] = new CLDevice(this, CPU.is32Bit() ? deviceIDs.getInt() : deviceIDs.getLong());
            }
        }
    }

    /**
     * Creates a context on all available devices (CL_DEVICE_TYPE_ALL).
     * The platform to be used is implementation dependent.
     */
    public static final CLContext create() {
        return createContextFromType(null, CL.CL_DEVICE_TYPE_ALL);
    }

    /**
     * Creates a context on the specified device types.
     * The platform to be used is implementation dependent.
     */
    public static final CLContext create(CLDevice.Type... deviceTypes) {
        return create(null, deviceTypes);
    }

    /**
     * Creates a context on the specified devices.
     * The platform to be used is implementation dependent.
     */
    public static final CLContext create(CLDevice... devices) {
        return create(null, devices);
    }

    /**
     * Creates a context on the specified platform on all available devices (CL_DEVICE_TYPE_ALL).
     */
    public static final CLContext create(CLPlatform platform) {
        return create(platform, CLDevice.Type.ALL);
    }

    // TODO check if driver bug, otherwise find the reason why this is not working (INVALID_VALUE with NV driver)
    /**
     * Creates a context on the specified platform and with the specified
     * device types.
     */
    private static final CLContext create(CLPlatform platform, CLDevice.Type... deviceTypes) {

        long type = 0;
        if(deviceTypes != null) {
            for (int i = 0; i < deviceTypes.length; i++) {
                type |= deviceTypes[i].CL_TYPE;
            }
        }

        IntBuffer properties = null;
        if(platform != null) {
            properties = IntBuffer.allocate(3);
            properties.put(CL.CL_CONTEXT_PLATFORM).put((int)platform.ID).put(0); // TODO check if this has to be int or long
            properties.rewind();
        }

        return createContextFromType(properties, type);
    }

    /**
     * Creates a context on the specified platform and with the specified
     * devices.
     */
    private static final CLContext create(CLPlatform platform, CLDevice... devices) {

        long[] deviceIDs = new long[devices.length];

        for (int i = 0; i < devices.length; i++) {
            deviceIDs[i] = devices[i].ID;
        }

        IntBuffer properties = null;
        if(platform != null) {
            properties = IntBuffer.allocate(3);
            properties.put(CL.CL_CONTEXT_PLATFORM).put((int)platform.ID).put(0); // TODO check if this has to be int or long
            properties.rewind();
        }

        return createContext(properties, deviceIDs);
    }

    private static final CLContext createContextFromType(IntBuffer properties, long deviceType) {

        IntBuffer status = IntBuffer.allocate(1);
        long context = CLPlatform.getLowLevelBinding().clCreateContextFromType(properties, deviceType, null, null, status);

        checkForError(status.get(), "can not create CL context");

        return new CLContext(context);
    }

    private static final CLContext createContext(IntBuffer properties, long[] devices) {

        IntBuffer status = IntBuffer.allocate(1);
        long context = CLPlatform.getLowLevelBinding().clCreateContext(properties, devices, null, null, status);

        checkForError(status.get(), "can not create CL context");

        return new CLContext(context);
    }

    /**
     * Creates a program from the given sources, the program is not build yet.
     */
    public CLProgram createProgram(String src) {
        CLProgram program = new CLProgram(this, src, ID);
        programs.add(program);
        return program;
    }

    /**
     * Creates a program and reads the sources from stream, the program is not build yet.
     * @throws IOException when a IOException occurred while reading or closing the stream.
     */
    public CLProgram createProgram(InputStream sources) throws IOException {
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(sources));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null)
                sb.append(line).append("\n");
        } finally {
            sources.close();
        }

        return createProgram(sb.toString());
    }

    /**
     * Creates a CLBuffer with the specified flags. No flags creates a MEM.READ_WRITE buffer.
     */
    public <B extends Buffer> CLBuffer<B> createBuffer(B directBuffer, Mem... flags) {
        return createBuffer(directBuffer, Mem.flagsToInt(flags));
    }
    
    /**
     * Creates a CLBuffer with the specified flags and buffer size in bytes. No flags creates a MEM.READ_WRITE buffer.
     */
    public CLBuffer<ByteBuffer> createBuffer(int size, Mem... flags) {
        return createBuffer(size, Mem.flagsToInt(flags));
    }

    /**
     * Creates a CLBuffer with the specified flags and buffer size in bytes.
     */
    public CLBuffer<ByteBuffer> createBuffer(int size, int flags) {
        return createBuffer(BufferFactory.newDirectByteBuffer(size), flags);
    }

    /**
     * Creates a CLBuffer with the specified flags.
     */
    public <B extends Buffer> CLBuffer<B> createBuffer(B directBuffer, int flags) {
        CLBuffer<B> buffer = new CLBuffer<B>(this, directBuffer, flags);
        buffers.add(buffer);
        return buffer;
    }

    public <B extends Buffer> CLBuffer<B> createFromGLBuffer(B directBuffer, int glBuffer, Mem... flags) {
        return createFromGLBuffer(directBuffer, glBuffer, Mem.flagsToInt(flags));
    }
    
    public <B extends Buffer> CLBuffer<B> createFromGLBuffer(B directBuffer, int glBuffer, int flags) {
        CLBuffer<B> buffer = new CLBuffer<B>(this, directBuffer, glBuffer, flags);
        buffers.add(buffer);
        return buffer;
    }

    CLCommandQueue createCommandQueue(CLDevice device, long properties) {

        CLCommandQueue queue = new CLCommandQueue(this, device, properties);

        List<CLCommandQueue> list = queuesMap.get(device);
        if(list == null) {
            list = new ArrayList<CLCommandQueue>();
            queuesMap.put(device, list);
        }
        list.add(queue);

        return queue;
    }

    void onProgramReleased(CLProgram program) {
        programs.remove(program);
    }

    void onBufferReleased(CLBuffer<?> buffer) {
        buffers.remove(buffer);
    }

    void onCommandQueueReleased(CLDevice device, CLCommandQueue queue) {
        List<CLCommandQueue> list = queuesMap.get(device);
        list.remove(queue);
        // remove empty lists from map
        if(list.isEmpty())
            queuesMap.remove(device);
    }

    /**
     * Releases the context and all resources.
     */
    public void release() {

        //release all resources
        while(!programs.isEmpty())
            programs.get(0).release();

        while(!buffers.isEmpty())
            buffers.get(0).release();

        int ret = cl.clReleaseContext(ID);
        checkForError(ret, "error releasing context");
    }

    /**
     * Returns a read only view of all programs associated with this context.
     */
    public List<CLProgram> getCLPrograms() {
        return Collections.unmodifiableList(programs);
    }

    /**
     * Returns a read only view of all buffers associated with this context.
     */
    public List<CLBuffer<? extends Buffer>> getCLBuffers() {
        return Collections.unmodifiableList(buffers);
    }


    /**
     * Gets the device with maximal FLOPS from this context.
     * The device speed is estimated by calulating the product of
     * MAX_COMPUTE_UNITS and MAX_CLOCK_FREQUENCY.
     */
    public CLDevice getMaxFlopsDevice() {
        return CLPlatform.findMaxFlopsDevice(getCLDevices());
    }

    /**
     * Returns all devices associated with this CLContext.
     */
    public CLDevice[] getCLDevices() {
        initDevices();
        return devices;
    }

    CLDevice getCLDevice(long dID) {
        CLDevice[] deviceArray = getCLDevices();
        for (int i = 0; i < deviceArray.length; i++) {
            if(dID == deviceArray[i].ID)
                return deviceArray[i];
        }
        return null;
    }

    @Override
    public String toString() {
        return "CLContext [id: " + ID
                      + " #devices: " + getCLDevices().length
                      + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CLContext other = (CLContext) obj;
        if (this.ID != other.ID) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + (int) (this.ID ^ (this.ID >>> 32));
        return hash;
    }

}
