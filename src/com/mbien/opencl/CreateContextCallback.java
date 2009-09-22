package com.mbien.opencl;

import java.nio.ByteBuffer;

/**
 *
 * @author Michael Bien
 */
public interface CreateContextCallback {

    public void createContextCallback(String errinfo, ByteBuffer private_info, long cb, Object user_data);

}
