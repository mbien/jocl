
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
//__Ljava_lang_Object_2IJLjava_lang_Object_2Ljava_lang_Object_2Ljava_lang_Object_2I
JNIEXPORT jlong JNICALL
Java_com_mbien_opencl_impl_CLImpl_clCreateContextFromType0(JNIEnv *env, jobject _unused,
        jobject props, jint props_byte_offset, jlong device_type, jobject cb, jobject data, jobject errcode, jint errcode_byte_offset) {

    intptr_t * _props_ptr  = NULL;
    int32_t * _errcode_ptr = NULL;

/*
    printf("jlong: %zu \n", sizeof(jlong)   );
    printf("intptr_t: %zu \n", sizeof(intptr_t));
    printf("size_t: %zu \n", sizeof(size_t));
*/

    cl_context _res;

    if (props != NULL) {
        _props_ptr = (intptr_t *) (((char*) (*env)->GetDirectBufferAddress(env, props)) + props_byte_offset);
    }
    if (errcode != NULL) {
        _errcode_ptr = (int32_t *) (((char*) (*env)->GetDirectBufferAddress(env, errcode)) + errcode_byte_offset);
    }

    //TODO callback; payload
    _res = clCreateContextFromType((intptr_t *) _props_ptr, (uint64_t) device_type, NULL, NULL, (int32_t *) _errcode_ptr);

    return (jlong) (intptr_t) _res;
}


/*
JNIEXPORT jlong JNICALL
Java_com_mbien_opencl_impl_CLImpl_clCreateContext0(JNIEnv *env, jobject _unused,
        jobject props, jint props_byte_offset, jlong device_type, jobject cb, jobject data, jobject errcode, jint errcode_byte_offset) {

    intptr_t * _props_ptr  = NULL;
    int32_t * _errcode_ptr = NULL;
    void * _device_ptr = NULL;

    cl_context _res;

    if (props != NULL) {
        _props_ptr = (intptr_t *) (((char*) (*env)->GetDirectBufferAddress(env, props)) + props_byte_offset);
    }

   if (device_type != NULL) {
       _device_ptr = (void *) (((char*) (*env)->GetPrimitiveArrayCritical(env, device_type, NULL)));
   }

   _res = clCreateContext();

   if (device_type != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, device_type, _device_ptr, 0);
   }
   return (jlong) (intptr_t) _res;
}
*/



