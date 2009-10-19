package com.mbien.opencl;

import com.mbien.opencl.impl.CLImpl;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static com.mbien.opencl.CLException.*;

/**
 *
 * @author Michael Bien
 */
public final class CLContext {

    final static CL cl;
    public final long contextID;

    private CLDevice[] devices;

    private final List<CLProgram> programs;

    static{
        System.loadLibrary("gluegen-rt");
        System.loadLibrary("jocl");
        cl = new CLImpl();
    }

    private CLContext(long contextID) {
        this.contextID = contextID;
        this.programs = new ArrayList<CLProgram>();
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

        IntBuffer error = IntBuffer.allocate(1);
        long context = cl.clCreateContextFromType(null, 0, deviceType, null, null, error, 0);

        checkForError(error.get(), "can not create CL context");

        return new CLContext(context);
    }
    
    public CLProgram createProgram(String src) {
        CLProgram program = new CLProgram(this, src, contextID);
        programs.add(program);
        return program;
    }

    void programReleased(CLProgram program) {
        programs.remove(program);
    }

    /**
     * Releases the context and all resources.
     */
    public CLContext release() {
        int ret = cl.clReleaseContext(contextID);
        checkForError(ret, "error releasing context");
        return this;
    }

    /**
     * Returns a read only view of all programs associated with this context.
     */
    public List<CLProgram> getPrograms() {
        return Collections.unmodifiableList(programs);
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
            ret = cl.clGetContextInfo(contextID, CL.CL_CONTEXT_DEVICES, 0, null, longBuffer, 0);
            checkForError(ret, "can not enumerate devices");

            ByteBuffer deviceIDs = ByteBuffer.allocate((int)longBuffer[0]).order(ByteOrder.nativeOrder());

            ret = cl.clGetContextInfo(contextID, CL.CL_CONTEXT_DEVICES, deviceIDs.capacity(), deviceIDs, null, 0);
            checkForError(ret, "can not enumerate devices");

            devices = new CLDevice[deviceIDs.capacity()/sizeofDeviceID];
            for (int i = 0; i < devices.length; i++)
                devices[i] = new CLDevice(cl, deviceIDs.getLong()); // TODO doublechek deviceID size on 32 bit systems

        }

        return devices;
    }

    CLDevice getCLDevices(long dID) {
        CLDevice[] deviceArray = getCLDevices();
        for (int i = 0; i < deviceArray.length; i++) {
            if(dID == deviceArray[i].deviceID)
                return deviceArray[i];
        }
        return null;
    }

    /**
     * Lists all available OpenCL implementaitons.
     * @throws CLException if something went wrong initializing OpenCL
     */
    public static CLPlatform[] listCLPlatforms() {

        int[] intBuffer = new int[1];
        // find all available OpenCL platforms
        int ret = cl.clGetPlatformIDs(0, null, 0, intBuffer, 0);
        checkForError(ret, "can not enumerate platforms");

        // receive platform ids
        long[] platformId = new long[intBuffer[0]];
        ret = cl.clGetPlatformIDs(platformId.length, platformId, 0, null, 0);
        checkForError(ret, "can not enumerate platforms");

        CLPlatform[] platforms = new CLPlatform[platformId.length];

        for (int i = 0; i < platformId.length; i++)
            platforms[i] = new CLPlatform(cl, platformId[i]);

        return platforms;
    }

    /**
     * Returns the low level binding interface to the OpenCL APIs.
     */
    public static CL getLowLevelBinding() {
        return cl;
    }


}
