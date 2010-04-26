package com.jogamp.opencl;

import java.nio.ByteBuffer;

/**
 *
 * @author Michael Bien
 */
public interface CreateContextCallback {

    public void onError(String errinfo, ByteBuffer private_info, long cb);

}
