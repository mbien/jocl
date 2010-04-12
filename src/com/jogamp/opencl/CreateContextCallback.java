package com.jogamp.opencl;

import java.nio.ByteBuffer;

/**
 *
 * @author Michael Bien
 */
// TODO implement callbacks
public interface CreateContextCallback {

    public void createContextCallback(String errinfo, ByteBuffer private_info, long cb, Object user_data);

}
