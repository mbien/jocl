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

/*
 * Created on Monday, June 07 2010 at 04:25
 */
package com.jogamp.opencl.impl;

import com.jogamp.common.nio.NativeSizeBuffer;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.LongLongHashMap;
import com.jogamp.opencl.CLErrorHandler;
import com.jogamp.opencl.CLException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static com.jogamp.common.nio.Buffers.*;

/**
 * Java bindings to OpenCL, the Open Computing Language.
 * @author Michael Bien
 */
public class CLImpl extends CLAbstractImpl {

    //maps the context id to its error handler's global object pointer
    private final LongLongHashMap contextCallbackMap;

    public CLImpl(CLProcAddressTable addressTable) {
        super(addressTable);
        this.contextCallbackMap = new LongLongHashMap();
        this.contextCallbackMap.setKeyNotFoundValue(0);
    }

    public long clCreateContext(NativeSizeBuffer properties, NativeSizeBuffer devices, CLErrorHandler pfn_notify, IntBuffer errcode_ret) {

        if (properties != null && !properties.isDirect()) {
            throw new RuntimeException("Argument \"properties\" was not a direct buffer");
        }

        if (errcode_ret != null && !errcode_ret.isDirect()) {
            throw new RuntimeException("Argument \"errcode_ret\" was not a direct buffer");
        }

        final long address = addressTable._addressof_clCreateContext;
        if (address == 0) {
            throw new UnsupportedOperationException("Method not available");
        }

        long[] global = new long[1];
        long ctx = this.clCreateContext0(
                properties != null ? properties.getBuffer() : null, getDirectBufferByteOffset(properties),
                devices != null ? devices.remaining() : 0, devices != null ? devices.getBuffer() : null, getDirectBufferByteOffset(devices),
                pfn_notify, global, errcode_ret, getDirectBufferByteOffset(errcode_ret), address);

        if (pfn_notify != null && global[0] != 0) {
            synchronized (contextCallbackMap) {
                contextCallbackMap.put(ctx, global[0]);
            }
        }
        return ctx;
    }

    private native long clCreateContext0(Object cl_context_properties, int props_offset, int numDevices, Object devices, int devices_offset, Object pfn_notify, long[] global, Object errcode_ret, int err_offset, long address);

    public long clCreateContextFromType(NativeSizeBuffer properties, long device_type, CLErrorHandler pfn_notify, IntBuffer errcode_ret) {

        if (properties != null && !properties.isDirect()) {
            throw new RuntimeException("Argument \"properties\" was not a direct buffer");
        }

        if (errcode_ret != null && !errcode_ret.isDirect()) {
            throw new RuntimeException("Argument \"errcode_ret\" was not a direct buffer");
        }

        final long address = addressTable._addressof_clCreateContextFromType;
        if (address == 0) {
            throw new UnsupportedOperationException("Method not available");
        }

        long[] global = new long[1];
        long ctx = this.clCreateContextFromType0(
                properties != null ? properties.getBuffer() : null, getDirectBufferByteOffset(properties),
                device_type, pfn_notify, global, errcode_ret, getDirectBufferByteOffset(errcode_ret), address);

        if (pfn_notify != null && global[0] != 0) {
            synchronized (contextCallbackMap) {
                contextCallbackMap.put(ctx, global[0]);
            }
        }
        return ctx;
    }

    private native long clCreateContextFromType0(Object properties, int props_offset, long device_type, Object pfn_notify, long[] global, Object errcode_ret, int err_offset, long address);

    public int clReleaseContext(long context) {
        long global = 0;
        synchronized (contextCallbackMap) {
            global = contextCallbackMap.remove(context);
        }

        final long address = addressTable._addressof_clReleaseContext;
        if (address == 0) {
            throw new UnsupportedOperationException("Method not available");
        }
        return clReleaseContextImpl(context, global, address);
    }

    /** Interface to C language function: <br> <code> int32_t {@native clReleaseContext}(cl_context context); </code>    */
    public native int clReleaseContextImpl(long context, long global, long address);

    /** Interface to C language function: <br> <code> int32_t clBuildProgram(cl_program, uint32_t, cl_device_id * , const char * , void * ); </code>    */
    public int clBuildProgram(long program, int deviceCount, NativeSizeBuffer deviceList, String options, BuildProgramCallback cb) {

        if (deviceList != null && !deviceList.isDirect()) {
            throw new RuntimeException("Argument \"properties\" was not a direct buffer");
        }

        final long address = addressTable._addressof_clBuildProgram;
        if (address == 0) {
            throw new UnsupportedOperationException("Method not available");
        }
        return clBuildProgram0(program, deviceCount, deviceList != null ? deviceList.getBuffer() : null,
                getDirectBufferByteOffset(deviceList), options, cb, address);
    }

    /** Entry point to C language function: <code> int32_t clBuildProgram(cl_program, uint32_t, cl_device_id * , const char * , void * ); </code>    */
    private native int clBuildProgram0(long program, int deviceCount, Object deviceList, int deviceListOffset, String options, BuildProgramCallback cb, long address);


    public int clSetEventCallback(long event, int trigger, CLEventCallback callback) {
        final long address = addressTable._addressof_clSetEventCallback;
        if (address == 0) {
            throw new UnsupportedOperationException("Method not available");
        }
        return clSetEventCallback0(event, trigger, callback, address);
    }

    private native int clSetEventCallback0(long event, int type, CLEventCallback cb, long address);


    public int clSetMemObjectDestructorCallback(long memObjID, CLMemObjectDestructorCallback cb) {
        final long address = addressTable._addressof_clSetMemObjectDestructorCallback;
        if (address == 0) {
            throw new UnsupportedOperationException("Method not available");
        }
        return clSetMemObjectDestructorCallback0(memObjID, cb, address);
    }

    private native int clSetMemObjectDestructorCallback0(long memObjID, CLMemObjectDestructorCallback cb, long address);


    /** Interface to C language function: <br> <code> void *  {@native clEnqueueMapImage}(cl_command_queue command_queue, cl_mem image, uint32_t blocking_map, uint64_t map_flags, const size_t * , const size_t * , size_t *  image_row_pitch, size_t *  image_slice_pitch, uint32_t num_events_in_wait_list, cl_event *  event_wait_list, cl_event *  event, int32_t *  errcode_ret); </code>
    @param origin a direct {@link com.jogamp.gluegen.common.nio.NativeSizeBuffer}
    @param range a direct {@link com.jogamp.gluegen.common.nio.NativeSizeBuffer}
    @param image_row_pitch a direct {@link com.jogamp.gluegen.common.nio.NativeSizeBuffer}
    @param image_slice_pitch a direct {@link com.jogamp.gluegen.common.nio.NativeSizeBuffer}
    @param event_wait_list a direct {@link com.jogamp.gluegen.common.nio.NativeSizeBuffer}
    @param event a direct {@link com.jogamp.gluegen.common.nio.NativeSizeBuffer}
    @param errcode_ret a direct {@link java.nio.IntBuffer}   */
    public ByteBuffer clEnqueueMapImage(long command_queue, long image, int blocking_map, long map_flags,
            NativeSizeBuffer origin, NativeSizeBuffer range,
            NativeSizeBuffer image_row_pitch, NativeSizeBuffer image_slice_pitch,
            int num_events_in_wait_list,
            NativeSizeBuffer event_wait_list, NativeSizeBuffer event, IntBuffer errcode_ret) {

        if (origin != null && !origin.isDirect()) {
            throw new CLException("Argument \"origin\" was not a direct buffer");
        }
        if (range != null && !range.isDirect()) {
            throw new CLException("Argument \"range\" was not a direct buffer");
        }
        if (image_row_pitch != null && !image_row_pitch.isDirect()) {
            throw new CLException("Argument \"image_row_pitch\" was not a direct buffer");
        }
        if (image_slice_pitch != null && !image_slice_pitch.isDirect()) {
            throw new CLException("Argument \"image_slice_pitch\" was not a direct buffer");
        }
        if (event_wait_list != null && !event_wait_list.isDirect()) {
            throw new CLException("Argument \"event_wait_list\" was not a direct buffer");
        }
        if (event != null && !event.isDirect()) {
            throw new CLException("Argument \"event\" was not a direct buffer");
        }
        if (errcode_ret != null && !errcode_ret.isDirect()) {
            throw new CLException("Argument \"errcode_ret\" was not a direct buffer");
        }

        final long mapImageAddress = addressTable._addressof_clEnqueueMapImage;
        if (mapImageAddress == 0) {
            throw new UnsupportedOperationException("Method not available");
        }
        final long getImageInfoAddress = addressTable._addressof_clGetImageInfo;
        if (getImageInfoAddress == 0) {
            throw new UnsupportedOperationException("Method not available");
        }
        ByteBuffer _res;
        _res = clEnqueueMapImage0(command_queue, image, blocking_map, map_flags, origin != null ? origin.getBuffer() : null,
                getDirectBufferByteOffset(origin), range != null ? range.getBuffer() : null,
                getDirectBufferByteOffset(range), image_row_pitch != null ? image_row_pitch.getBuffer() : null,
                getDirectBufferByteOffset(image_row_pitch), image_slice_pitch != null ? image_slice_pitch.getBuffer() : null,
                getDirectBufferByteOffset(image_slice_pitch), num_events_in_wait_list,
                event_wait_list != null ? event_wait_list.getBuffer() : null, getDirectBufferByteOffset(event_wait_list),
                event != null ? event.getBuffer() : null, getDirectBufferByteOffset(event), errcode_ret,
                getDirectBufferByteOffset(errcode_ret));
        if (_res == null) {
            return null;
        }
        nativeOrder(_res);
        return _res;
    }

    /** Entry point to C language function: <code> void *  {@native clEnqueueMapImage}(cl_command_queue command_queue, cl_mem image, uint32_t blocking_map, uint64_t map_flags, const size_t * , const size_t * , size_t *  image_row_pitch, size_t *  image_slice_pitch, uint32_t num_events_in_wait_list, cl_event *  event_wait_list, cl_event *  event, int32_t *  errcode_ret); </code>
    @param origin a direct {@link com.jogamp.gluegen.common.nio.NativeSizeBuffer}
    @param range a direct {@link com.jogamp.gluegen.common.nio.NativeSizeBuffer}
    @param image_row_pitch a direct {@link com.jogamp.gluegen.common.nio.NativeSizeBuffer}
    @param image_slice_pitch a direct {@link com.jogamp.gluegen.common.nio.NativeSizeBuffer}
    @param event_wait_list a direct {@link com.jogamp.gluegen.common.nio.NativeSizeBuffer}
    @param event a direct {@link com.jogamp.gluegen.common.nio.NativeSizeBuffer}
    @param errcode_ret a direct {@link java.nio.IntBuffer}   */
    private native ByteBuffer clEnqueueMapImage0(long command_queue, long image, int blocking_map, long map_flags,
            Object origin, int origin_byte_offset, Object range, int range_byte_offset, Object image_row_pitch,
            int image_row_pitch_byte_offset, Object image_slice_pitch, int image_slice_pitch_byte_offset,
            int num_events_in_wait_list, Object event_wait_list, int event_wait_list_byte_offset, Object event,
            int event_byte_offset, Object errcode_ret, int errcode_ret_byte_offset);

    /**
     * Returns the extension function address for the given function name.
     */
    public long clGetExtensionFunctionAddress(String name) {
        ByteBuffer res = super.clGetExtensionFunctionAddressImpl(name);
        if(res == null) {
            return 0;
        }else if (Platform.is32Bit()) {
            return res.getInt();
        } else {
            return res.getLong();
        }
    }

    public CLProcAddressTable getAddressTable() {
        return addressTable;
    }

    /*
    private static void convert32To64(long[] values) {
    if (values.length % 2 == 1) {
    values[values.length - 1] = values[values.length / 2] >>> 32;
    }
    for (int i = values.length - 1 - values.length % 2; i >= 0; i -= 2) {
    long temp = values[i / 2];
    values[i - 1] = temp >>> 32;
    values[i] = temp & 0x00000000FFFFFFFFL;
    }
    }
     */



}
