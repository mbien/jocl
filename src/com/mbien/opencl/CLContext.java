package com.mbien.opencl;

import com.sun.gluegen.runtime.BufferFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
 *
 * @author Michael Bien
 */
public final class CLContext {

    final CL cl;
    public final long ID;

    private CLDevice[] devices;

    private final List<CLProgram> programs;
    private final List<CLBuffer> buffers;
    private final Map<CLDevice, List<CLCommandQueue>> queuesMap;


    private CLContext(long contextID) {
        this.cl = CLPlatform.getLowLevelBinding();
        this.ID = contextID;
        this.programs = new ArrayList<CLProgram>();
        this.buffers = new ArrayList<CLBuffer>();
        this.queuesMap = new HashMap<CLDevice, List<CLCommandQueue>>();
    }

    /**
     * Creates a default context on all available devices.
     */
    public static final CLContext create() {
        return createContext(CL.CL_DEVICE_TYPE_ALL);
    }

    /**
     * Creates a default context on the specified device types.
     */
    public static final CLContext create(CLDevice.Type... deviceTypes) {

        int type = deviceTypes[0].CL_TYPE;
        for (int i = 1; i < deviceTypes.length; i++) {
            type |= deviceTypes[i].CL_TYPE;
        }

        return createContext(type);
    }

    private static final CLContext createContext(long deviceType) {

        IntBuffer status = IntBuffer.allocate(1);
        long context = CLPlatform.getLowLevelBinding().clCreateContextFromType(null, 0, deviceType, null, null, status, 0);

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

    public CLBuffer createBuffer(int flags, ByteBuffer directBuffer) {
        CLBuffer buffer = new CLBuffer(this, flags, directBuffer);
        buffers.add(buffer);
        return buffer;
    }

    public CLBuffer createBuffer(int flags, int size) {
        return createBuffer(flags, BufferFactory.newDirectByteBuffer(size));
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

    void programReleased(CLProgram program) {
        programs.remove(program);
    }

    void bufferReleased(CLBuffer buffer) {
        buffers.remove(buffer);
    }

    void commandQueueReleased(CLDevice device, CLCommandQueue queue) {
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
    public List<CLBuffer> getCLBuffers() {
        return Collections.unmodifiableList(buffers);
    }


    /**
     * Gets the device with maximal FLOPS from this context.
     */
    /*
    public CLDevice getMaxFlopsDevice() {

        long[] longBuffer = new long[1];
//        ByteBuffer bb = ByteBuffer.allocate(8);
//        bb.order(ByteOrder.nativeOrder());

        int ret = cl.clGetContextInfo(contextID, CL.CL_CONTEXT_DEVICES, 0, null, longBuffer, 0);
        if(CL.CL_SUCCESS != ret)
            throw new CLException(ret, "can not receive context info");

        System.out.println("#devices: "+longBuffer[0]);

        long[] deviceIDs = new long[(int)longBuffer[0]];
        ret = cl.clGetContextInfo(contextID, CL.CL_CONTEXT_DEVICES, 0, null, deviceIDs, 0);

        if(CL.CL_SUCCESS != ret)
            throw new CLException(ret, "can not receive context info");

        for (int i = 0; i < deviceIDs.length; i++) {
            long l = deviceIDs[i];
            System.out.println("device id"+l);
        }

            // get the list of GPU devices associated with context
//        ciErrNum = clGetContextInfo(cxGPUContext, CL_CONTEXT_DEVICES, 0, NULL, &dataBytes);
//        cl_device_id *cdDevices = (cl_device_id *)malloc(dataBytes);
//        ciErrNum |= clGetContextInfo(cxGPUContext, CL_CONTEXT_DEVICES, dataBytes, cdDevices, NULL);
//        shrCheckError(ciErrNum, CL_SUCCESS);

        return null;
    }
*/

    /**
     * Returns all devices associated with this CLContext.
     */
    public CLDevice[] getCLDevices() {

        if(devices == null) {

            int sizeofDeviceID = 8; // TODO doublechek deviceID size on 32 bit systems

            long[] longBuffer = new long[1];

            int ret;
            ret = cl.clGetContextInfo(ID, CL.CL_CONTEXT_DEVICES, 0, null, longBuffer, 0);
            checkForError(ret, "can not enumerate devices");

            ByteBuffer deviceIDs = ByteBuffer.allocate((int)longBuffer[0]).order(ByteOrder.nativeOrder());

            ret = cl.clGetContextInfo(ID, CL.CL_CONTEXT_DEVICES, deviceIDs.capacity(), deviceIDs, null, 0);
            checkForError(ret, "can not enumerate devices");

            devices = new CLDevice[deviceIDs.capacity()/sizeofDeviceID];
            for (int i = 0; i < devices.length; i++)
                devices[i] = new CLDevice(this, deviceIDs.getLong()); // TODO doublechek deviceID size on 32 bit systems

        }

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
