/*
void checkStatus(const char* msg, int status) {
    if (status != CL_SUCCESS) {
        printf("%s; error: %d \n", msg, status);
        exit(EXIT_FAILURE);
    }
}
*/


/*
void createContextCallback(const char * c, const void * v, size_t s, void * o) {
    //TODO
}
*/

/*   Java->C glue code:
 *   Java package: com.mbien.opencl.impl.CLImpl
 *    Java method: long clCreateContextFromType(java.nio.IntBuffer props, long device_type, CreateContextCallback pfn_notify, Object userData, IntBuffer errcode_ret)
 *     C function: cl_context clCreateContextFromType(  cl_context_properties *  properties ,
 *                                                      cl_uint                  num_devices ,
 *                                                      const cl_device_id *     devices ,
 *                                                      void (*pfn_notify)(const char *, const void *, size_t, void *)  pfn_notify/,
 *                                                      void *                   user_data ,
 *                                                      cl_int *                 errcode_ret );
 */
JNIEXPORT jlong JNICALL
Java_com_mbien_opencl_impl_CLImpl_clCreateContextFromType0(JNIEnv *env, jobject _unused,
        jobject props, jint props_byte_offset, jobject device_type, jobject cb, jobject data, jobject errcode, jint errcode_byte_offset) {

    cl_context_properties* _props_ptr  = NULL;
    int32_t * _errcode_ptr = NULL;
    cl_context _ctx;

    if (props != NULL) {
        _props_ptr = (cl_context_properties*) (((char*) (*env)->GetDirectBufferAddress(env, props)) + props_byte_offset);
    }

    if (errcode != NULL) {
        _errcode_ptr = (void *) (((char*) (*env)->GetDirectBufferAddress(env, errcode)) + errcode_byte_offset);
    }

    //TODO callback; payload
    _ctx = clCreateContextFromType(_props_ptr, (uint64_t) device_type, NULL, NULL, (int32_t *) _errcode_ptr);

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
Java_com_mbien_opencl_impl_CLImpl_clCreateContext0(JNIEnv *env, jobject _unused,
        jobject props, jint props_byte_offset, jint numDevices, jobject deviceList, jint device_type_offset, jobject cb, jobject data, jobject errcode, jint errcode_byte_offset) {

    cl_context_properties* _props_ptr  = NULL;
    int32_t * _errcode_ptr = NULL;
    size_t * _deviceListPtr = NULL;

    cl_context _ctx;

    if (props != NULL) {
        _props_ptr = (cl_context_properties*) (((char*) (*env)->GetDirectBufferAddress(env, props)) + props_byte_offset);
    }
    if (deviceList != NULL) {
        _deviceListPtr = (void *) (((char*) (*env)->GetDirectBufferAddress(env, deviceList)) + device_type_offset);
    }
    if (errcode != NULL) {
        _errcode_ptr = (void *) (((char*) (*env)->GetDirectBufferAddress(env, errcode)) + errcode_byte_offset);
    }

    // TODO payload, callback...
    _ctx = clCreateContext(_props_ptr, numDevices, (cl_device_id *)_deviceListPtr, NULL, NULL, (int32_t *) _errcode_ptr);

    return (jlong) (intptr_t)_ctx;
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
Java_com_mbien_opencl_impl_CLImpl_clBuildProgram0(JNIEnv *env, jobject _unused,
        jlong program, jint deviceCount, jobject deviceList, jint device_type_offset, jstring options, jobject cb, jobject data) {

    const char* _strchars_options = NULL;
    cl_int _res;
    size_t * _deviceListPtr = NULL;

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

    // TODO payload, callback...
    _res = clBuildProgram((cl_program)program, (cl_uint)deviceCount, (cl_device_id *)_deviceListPtr, _strchars_options, NULL, NULL);

    if (options != NULL) {
        (*env)->ReleaseStringUTFChars(env, options, _strchars_options);
    }
    
    return (jint)_res;
}
