/*
void checkStatus(const char* msg, int status) {
    if (status != CL_SUCCESS) {
        printf("%s; error: %d \n", msg, status);
        exit(EXIT_FAILURE);
    }
}
*/

JavaVM * jvm;

jmethodID buildCB_mid;
jmethodID contextCB_mid;
jmethodID eventCB_mid;
jmethodID memObjCB_mid;


JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM * _jvm, void *reserved) {

    JNIEnv *env;
    jvm = _jvm;

    if ((*jvm)->GetEnv(_jvm, (void **)&env, JNI_VERSION_1_2)) {
        return JNI_ERR;
    }

    // throws ClassNotFoundException (or other reflection stuff)
    jclass buildCBClassID      = (*env)->FindClass(env, "com/jogamp/opencl/impl/BuildProgramCallback");
    jclass errorHandlerClassID = (*env)->FindClass(env, "com/jogamp/opencl/CLErrorHandler");
    jclass eventCBClassID      = (*env)->FindClass(env, "com/jogamp/opencl/impl/CLEventCallback");
    jclass memObjCBClassID     = (*env)->FindClass(env, "com/jogamp/opencl/impl/CLMemObjectDestructorCallback");

    // throws even more reflection Exceptions
    // IDs are unique and do not change
    if (buildCBClassID != NULL) {
        buildCB_mid   = (*env)->GetMethodID(env, buildCBClassID, "buildFinished", "(J)V");
    }
    if (errorHandlerClassID != NULL) {
        contextCB_mid = (*env)->GetMethodID(env, errorHandlerClassID, "onError", "(Ljava/lang/String;Ljava/nio/ByteBuffer;J)V");
    }
    if (eventCBClassID != NULL) {
        eventCB_mid = (*env)->GetMethodID(env, eventCBClassID, "eventStateChanged", "(JI)V");
    }
    if (memObjCBClassID != NULL) {
        memObjCB_mid = (*env)->GetMethodID(env, memObjCBClassID, "memoryDeallocated", "(J)V");
    }

    return JNI_VERSION_1_2;
}


// callbacks
typedef void (CL_CALLBACK * cccallback)(const char *, const void *, size_t, void *);
typedef void (CL_CALLBACK * bpcallback)(cl_program, void *);
typedef void (CL_CALLBACK * evcallback)(cl_event, cl_int, void *);
typedef void (CL_CALLBACK * mocallback)(cl_mem, void *);

CL_CALLBACK void buildProgramCallback(cl_program id, void * object) {

    JNIEnv *env;
    jobject obj = (jobject)object;

    (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL);

        (*env)->CallVoidMethod(env, obj, buildCB_mid, (jlong)(intptr_t)id);
        (*env)->DeleteGlobalRef(env, obj);

    (*jvm)->DetachCurrentThread(jvm);

}

CL_CALLBACK void createContextCallback(const char * errinfo, const void * private_info, size_t cb, void * object) {

    JNIEnv *env;
    jobject obj = (jobject)object;

    (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL);

        jstring errorString = (*env)->NewStringUTF(env, errinfo);

        //TODO private_info
        (*env)->CallVoidMethod(env, obj, contextCB_mid, errorString, NULL, 0);

    (*jvm)->DetachCurrentThread(jvm);

}

CL_CALLBACK void eventCallback(cl_event event, cl_int status, void * object) {

    JNIEnv *env;
    jobject obj = (jobject)object;

    (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL);

        (*env)->CallVoidMethod(env, obj, eventCB_mid, event, status);
        (*env)->DeleteGlobalRef(env, obj); // events can only fire once

    (*jvm)->DetachCurrentThread(jvm);
}

CL_CALLBACK void memObjDestructorCallback(cl_mem mem, void * object) {

    JNIEnv *env;
    jobject obj = (jobject)object;

    (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL);
    
        (*env)->CallVoidMethod(env, obj, memObjCB_mid, mem);
        (*env)->DeleteGlobalRef(env, obj);

    (*jvm)->DetachCurrentThread(jvm);
}


/*   Java->C glue code:
 *   Java package: com.jogamp.opencl.impl.CLImpl
 *    Java method: long clCreateContextFromType(java.nio.IntBuffer props, long device_type, CreateContextCallback pfn_notify, Object userData, IntBuffer errcode_ret)
 *     C function: cl_context clCreateContextFromType(  const cl_context_properties *  properties ,
 *                                                      cl_device_type                 device_type ,
 *                                                      void (CL_CALLBACK *      pfn_notify )(const char *, const void *, size_t, void *),
 *                                                      void *                         user_data ,
 *                                                      cl_int *                       errcode_ret);
 */
JNIEXPORT jlong JNICALL
Java_com_jogamp_opencl_impl_CLImpl_clCreateContextFromType0(JNIEnv *env, jobject _unused,
        jobject props, jint props_byte_offset, jlong device_type, jobject cb, jobject global, jobject errcode, jint errcode_byte_offset, jlong procAddress) {

    cl_context_properties* _props_ptr  = NULL;
    cl_int * _errcode_ptr = NULL;
    cl_context _ctx = NULL;
    cccallback _pfn_notify = NULL;
    jobject globalCB = NULL;

    typedef cl_context (*function)(const cl_context_properties *, cl_device_type, void (*pfn_notify)(const char *, const void *, size_t, void *), void *, cl_int *);
    function clCreateContextFromType = (function)(intptr_t) procAddress;

    if (props != NULL) {
        _props_ptr = (cl_context_properties*) (((char*) (*env)->GetDirectBufferAddress(env, props)) + props_byte_offset);
    }

    if (errcode != NULL) {
        _errcode_ptr = (void *) (((char*) (*env)->GetDirectBufferAddress(env, errcode)) + errcode_byte_offset);
    }

    if (cb != NULL) {
        _pfn_notify = &createContextCallback;
        globalCB = (*env)->NewGlobalRef(env, cb);
    }

    _ctx = (*clCreateContextFromType)(_props_ptr, (uint64_t) device_type, _pfn_notify, globalCB, _errcode_ptr);

    if(globalCB != NULL) {
        jlong *g = (*env)->GetPrimitiveArrayCritical(env, global, NULL);
        // if something went wrong
        if(_ctx == NULL) {
            g[0] = 0;
            (*env)->DeleteGlobalRef(env, globalCB);
        }else{
            g[0] = (jlong)globalCB;
        }
        (*env)->ReleasePrimitiveArrayCritical(env, global, g, 0);
    }

    return (jlong) (intptr_t)_ctx;
}

/*
 * Entry point to C language function:
 *extern CL_API_ENTRY cl_context CL_API_CALL
 *clCreateContext(cl_context_properties *    properties   ,
 *                cl_uint                    num_devices   ,
 *                const cl_device_id *       devices   ,
 *                void (*pfn_notify)(const char *, const void *, size_t, void *)  pfn_notify ,
 *                void *                     user_data   ,
 *                cl_int *                   errcode_ret   ) CL_API_SUFFIX__VERSION_1_0;
 */
JNIEXPORT jlong JNICALL
Java_com_jogamp_opencl_impl_CLImpl_clCreateContext0(JNIEnv *env, jobject _unused,
        jobject props, jint props_byte_offset, jint numDevices, jobject deviceList, jint device_type_offset, jobject cb, jobject global, jobject errcode, jint errcode_byte_offset, jlong procAddress) {

    cl_context_properties* _props_ptr  = NULL;
    cl_int * _errcode_ptr = NULL;
    cl_device_id * _deviceListPtr = NULL;
    cl_context _ctx = NULL;
    cccallback _pfn_notify = NULL;
    jobject globalCB = NULL;

    typedef cl_context (*function)(cl_context_properties *, cl_uint, const cl_device_id *, void (*pfn_notify)(const char *, const void *, size_t, void *), void *, cl_int *);
    function clCreateContext = (function)(intptr_t) procAddress;

    if (props != NULL) {
        _props_ptr = (cl_context_properties*) (((char*) (*env)->GetDirectBufferAddress(env, props)) + props_byte_offset);
    }
    if (deviceList != NULL) {
        _deviceListPtr = (void *) (((char*) (*env)->GetDirectBufferAddress(env, deviceList)) + device_type_offset);
    }
    if (errcode != NULL) {
        _errcode_ptr = (void *) (((char*) (*env)->GetDirectBufferAddress(env, errcode)) + errcode_byte_offset);
    }

    if (cb != NULL) {
        _pfn_notify = &createContextCallback;
        globalCB = (*env)->NewGlobalRef(env, cb);
    }

    _ctx = (*clCreateContext)(_props_ptr, numDevices, _deviceListPtr, _pfn_notify, globalCB, _errcode_ptr);

    if(globalCB != NULL) {
        jlong *g = (*env)->GetPrimitiveArrayCritical(env, global, NULL);
        // if something went wrong
        if(_ctx == NULL) {
            g[0] = 0;
            (*env)->DeleteGlobalRef(env, globalCB);
        }else{
            g[0] = (jlong)globalCB;
        }
        (*env)->ReleasePrimitiveArrayCritical(env, global, g, 0);
    }

    return (jlong) (intptr_t)_ctx;
}

/*   Java->C glue code:
 *   Java package: com.jogamp.opencl.impl.CLImpl
 *    Java method: int clReleaseContextImpl(long context)
 *     C function: int32_t clReleaseContextImpl(cl_context context);
 */
JNIEXPORT jint JNICALL
Java_com_jogamp_opencl_impl_CLImpl_clReleaseContextImpl(JNIEnv *env, jobject _unused, jlong context, jlong global, jlong procAddress) {

    int32_t _res;
    typedef int32_t (*function)(cl_context);
    function clReleaseContext = (function)(intptr_t) procAddress;

    _res = (*clReleaseContext)((cl_context) (intptr_t) context);
    // TODO deal with retains
    if (global != 0) {
        (*env)->DeleteGlobalRef(env, (jobject) global);
    }
    return _res;
}


/**
 * Entry point to C language function:
 * extern CL_API_ENTRY cl_int CL_API_CALL
 *clBuildProgram(cl_program              program   ,
 *               cl_uint                 num_devices   ,
 *               const cl_device_id *    device_list   ,
 *               const char *            options   ,
 *               void (*pfn_notify)(cl_program    program   , void *    user_data   ),
 *               void *                  user_data   ) CL_API_SUFFIX__VERSION_1_0;
 */
JNIEXPORT jint JNICALL
Java_com_jogamp_opencl_impl_CLImpl_clBuildProgram0(JNIEnv *env, jobject _unused,
        jlong program, jint deviceCount, jobject deviceList, jint device_type_offset, jstring options, jobject cb, jlong procAddress) {

    const char* _strchars_options = NULL;
    cl_int _res;
    cl_device_id * _deviceListPtr = NULL;
    bpcallback _pfn_notify = NULL;
    jobject globalCB = NULL;

    typedef cl_int (*function)(cl_program, cl_uint, const cl_device_id *, const char *, void (CL_CALLBACK *)(cl_program, void *), void *);
    function clBuildProgram = (function)(intptr_t)procAddress;

    if (options != NULL) {
        _strchars_options = (*env)->GetStringUTFChars(env, options, (jboolean*)NULL);
        if (_strchars_options == NULL) {
            (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"),
                           "Failed to get UTF-8 chars for argument \"options\" in native dispatcher for \"clBuildProgram\"");
            return CL_FALSE;
        }
    }

    if (deviceList != NULL) {
        _deviceListPtr = (void *) (((char*) (*env)->GetDirectBufferAddress(env, deviceList)) + device_type_offset);
    }

    if (cb != NULL) {
        _pfn_notify = &buildProgramCallback;
        globalCB = (*env)->NewGlobalRef(env, cb);
    }

    _res = (*clBuildProgram)((cl_program)(intptr_t)program, (cl_uint)deviceCount, (cl_device_id *)_deviceListPtr, _strchars_options, _pfn_notify, globalCB);

    // if something went wrong
    if(_res != CL_SUCCESS && globalCB != NULL) {
        (*env)->DeleteGlobalRef(env, globalCB);
    }

    if (options != NULL) {
        (*env)->ReleaseStringUTFChars(env, options, _strchars_options);
    }

    return (jint)_res;
}

/*   Java->C glue code:
 *   Java package: com.jogamp.opencl.impl.CLImpl
 *    Java method: java.nio.ByteBuffer clEnqueueMapImage(long command_queue, long image, int blocking_map, long map_flags, com.jogamp.gluegen.common.nio.NativeSizeBuffer origin, com.jogamp.gluegen.common.nio.NativeSizeBuffer range, com.jogamp.gluegen.common.nio.NativeSizeBuffer image_row_pitch, com.jogamp.gluegen.common.nio.NativeSizeBuffer image_slice_pitch, int num_events_in_wait_list, com.jogamp.gluegen.common.nio.NativeSizeBuffer event_wait_list, com.jogamp.gluegen.common.nio.NativeSizeBuffer event, java.nio.IntBuffer errcode_ret)
 *     C function: void *  clEnqueueMapImage(cl_command_queue command_queue, cl_mem image, uint32_t blocking_map, uint64_t map_flags, const size_t * , const size_t * , size_t *  image_row_pitch, size_t *  image_slice_pitch, uint32_t num_events_in_wait_list, cl_event *  event_wait_list, cl_event *  event, int32_t *  errcode_ret);
 */
JNIEXPORT jobject JNICALL
Java_com_jogamp_opencl_impl_CLImpl_clEnqueueMapImage0__JJIJLjava_lang_Object_2ILjava_lang_Object_2ILjava_lang_Object_2ILjava_lang_Object_2IILjava_lang_Object_2ILjava_lang_Object_2ILjava_lang_Object_2I(JNIEnv *env, jobject _unused,
        jlong command_queue, jlong image, jint blocking_map, jlong map_flags,
        jobject origin, jint origin_byte_offset, jobject range, jint range_byte_offset,
        jobject image_row_pitch, jint image_row_pitch_byte_offset, jobject image_slice_pitch,
        jint image_slice_pitch_byte_offset, jint num_events_in_wait_list, jobject event_wait_list,
        jint event_wait_list_byte_offset, jobject event, jint event_byte_offset, jobject errcode_ret, jint errcode_ret_byte_offset,
        jlong imageInfoAddress, jlong mapImageAddress) {

    size_t * _origin_ptr = NULL;
    size_t * _range_ptr = NULL;
    size_t * _image_row_pitch_ptr = NULL;
    size_t * _image_slice_pitch_ptr = NULL;
    cl_event * _event_wait_list_ptr = NULL;
    cl_event * _event_ptr = NULL;
    int32_t * _errcode_ret_ptr = NULL;
    size_t * elements = NULL;
    size_t * depth = NULL;
    size_t pixels;
    cl_int status;

    typedef int32_t (*imageInfoFunctionType)(cl_mem, uint32_t, size_t, void *, size_t *);
    imageInfoFunctionType clGetImageInfo;

    typedef void* (*mapInfoFunctionType)(cl_command_queue, cl_mem, uint32_t, uint64_t, const size_t *,
                const size_t *, size_t *, size_t *, uint32_t, cl_event *, cl_event *, int32_t *);
    mapInfoFunctionType clEnqueueMapImage;

    void * _res;

    if (origin != NULL) {
        _origin_ptr = (size_t *) (((char*) (*env)->GetDirectBufferAddress(env, origin)) + origin_byte_offset);
    }
    if (range != NULL) {
        _range_ptr = (size_t *) (((char*) (*env)->GetDirectBufferAddress(env, range)) + range_byte_offset);
    }
    if (image_row_pitch != NULL) {
        _image_row_pitch_ptr = (size_t *) (((char*) (*env)->GetDirectBufferAddress(env, image_row_pitch)) + image_row_pitch_byte_offset);
    }
    if (image_slice_pitch != NULL) {
        _image_slice_pitch_ptr = (size_t *) (((char*) (*env)->GetDirectBufferAddress(env, image_slice_pitch)) + image_slice_pitch_byte_offset);
    }
    if (event_wait_list != NULL) {
        _event_wait_list_ptr = (cl_event *) (((char*) (*env)->GetDirectBufferAddress(env, event_wait_list)) + event_wait_list_byte_offset);
    }
    if (event != NULL) {
        _event_ptr = (cl_event *) (((char*) (*env)->GetDirectBufferAddress(env, event)) + event_byte_offset);
    }
    if (errcode_ret != NULL) {
        _errcode_ret_ptr = (int32_t *) (((char*) (*env)->GetDirectBufferAddress(env, errcode_ret)) + errcode_ret_byte_offset);
    }

    _res = (*clEnqueueMapImage)((cl_command_queue) (intptr_t) command_queue, (cl_mem) (intptr_t) image,
               (uint32_t) blocking_map, (uint64_t) map_flags, (size_t *) _origin_ptr, (size_t *) _range_ptr,
               (size_t *) _image_row_pitch_ptr, (size_t *) _image_slice_pitch_ptr, (uint32_t) num_events_in_wait_list,
               (cl_event *) _event_wait_list_ptr, (cl_event *) _event_ptr, (int32_t *) _errcode_ret_ptr);
    if (_res == NULL) return NULL;

    // calculate buffer size
    status  = (*clGetImageInfo)((cl_mem) (intptr_t) image, CL_IMAGE_ELEMENT_SIZE, sizeof(size_t), (void *) elements, NULL);
    status |= (*clGetImageInfo)((cl_mem) (intptr_t) image, CL_IMAGE_DEPTH, sizeof(size_t), (void *) depth, NULL);

    if(status != CL_SUCCESS) {
        return NULL;
    }

    if(*depth == 0) { // 2D
        pixels = (*_image_row_pitch_ptr)   * _range_ptr[1] + _range_ptr[0];
    }else{            // 3D
        pixels = (*_image_slice_pitch_ptr) * _range_ptr[2]
               + (*_image_row_pitch_ptr)   * _range_ptr[1] + _range_ptr[0];
    }

  return (*env)->NewDirectByteBuffer(env, _res, pixels * (*elements));

}

JNIEXPORT jint JNICALL
Java_com_jogamp_opencl_impl_CLImpl_clSetEventCallback0(JNIEnv *env, jobject _unused,
        jlong event, jint trigger, jobject listener, jlong procAddress) {

    cl_event _event = event;
    cl_int _trigger = trigger;
    cl_int _res;
    typedef int32_t (*function)(cl_event, cl_int, void (*pfn_event_notify) (cl_event, cl_int, void *), void *);
    function clSetEventCallback = (function)(intptr_t) procAddress;

    jobject cb = (*env)->NewGlobalRef(env, listener);
    _res = (*clSetEventCallback)(_event, _trigger, &eventCallback, cb);

    return _res;
}

JNIEXPORT jint JNICALL
Java_com_jogamp_opencl_impl_CLImpl_clSetMemObjectDestructorCallback0(JNIEnv *env, jobject _unused,
        jlong mem, jobject listener, jlong procAddress) {

    cl_mem _mem = mem;
    cl_int _res;
    typedef int32_t (*function)(cl_mem, void (*pfn_event_notify) (cl_mem, void *), void *);
    function clSetMemObjectDestructorCallback = (function)(intptr_t) procAddress;

    jobject cb = (*env)->NewGlobalRef(env, listener);
    _res = (*clSetMemObjectDestructorCallback)(_mem, &memObjDestructorCallback, cb);

    return _res;
}
