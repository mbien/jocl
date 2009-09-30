package com.mbien.opencl;

import java.nio.ByteBuffer;

/**
 *
 * @author Michael Bien
 */
public class CLDevice {
    
    private final CL cl;
    private final long deviceID;

    CLDevice(CL cl, long id) {
        this.cl = cl;
        this.deviceID = id;
    }

    /**
     * Returns the name of this device.
     */
    public String getName() {
        return getInfoString(CL.CL_DEVICE_NAME);
    }

    public String getInfoString(int key) {

        long[] longBuffer = new long[1];
        ByteBuffer bb = ByteBuffer.allocate(512);

        int ret = cl.clGetDeviceInfo(deviceID, key, bb.capacity(), bb, longBuffer, 0);
        
        if(CL.CL_SUCCESS != ret)
            throw new CLException(ret, "can not receive info string");

        return new String(bb.array(), 0, (int)longBuffer[0]);
        
    }

//   ret = cl.clGetDeviceInfo(device, CL.CL_DEVICE_TYPE, bb.capacity(), bb, longBuffer, 0);
//   assertEquals(CL.CL_SUCCESS, ret);
}
