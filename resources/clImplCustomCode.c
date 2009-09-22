
/*
void createContextCallback(const char * c, const void * v, size_t s, void * o) {
    //TODO
}
*/

/*   Java->C glue code:
 *   Java package: com.mbien.opencl.impl.CLImpl
 *    Java method: long clCreateContextFromType(java.nio.IntBuffer arg0, long device_type, CreateContextCallback pfn_notify, Object userData, IntBuffer errcode_ret)
 *     C function: cl_context clCreateContextFromType(  cl_context_properties *  properties ,
 *                                                      cl_uint                  num_devices ,
 *                                                      const cl_device_id *     devices ,
 *                                                      void (*pfn_notify)(const char *, const void *, size_t, void *)  pfn_notify/,
 *                                                      void *                   user_data ,
 *                                                      cl_int *                 errcode_ret );
 */
//Ljava/nio/IntBuffer;JLjava/lang/Object;Ljava/lang/Object;Ljava/nio/IntBuffer;
//Ljava_lang_Object_2I      J            Ljava_lang_Object_2     Ljava_lang_Object_2   Ljava_lang_Object_2I
//IntBuffer arg0,    long device_type,   Object pfn_notify,        Object userData,     IntBuffer errcode_ret
JNIEXPORT jlong JNICALL
Java_com_mbien_opencl_impl_CLImpl_clCreateContextFromType0__Ljava_lang_Object_2IJLjava_lang_Object_2Ljava_lang_Object_2Ljava_lang_Object_2I(JNIEnv *env, jobject _unused,
        jobject arg0, jint arg0_byte_offset, jlong device_type, jobject cb, jobject data, jobject errcode, jint errcode_byte_offset) {

  printf("%s", "function entry");

  intptr_t * _ptr0 = NULL;
  int32_t * _ptr2 = NULL;
  
  cl_context _res;

    if (arg0 != NULL) {
        _ptr0 = (intptr_t *) (((char*) (*env)->GetDirectBufferAddress(env, arg0)) + arg0_byte_offset);
    }
    if (errcode != NULL) {
        _ptr2 = (int32_t *) (((char*) (*env)->GetDirectBufferAddress(env, errcode)) + errcode_byte_offset);
    }

  printf("%s", "pre call");
  _res = clCreateContextFromType((intptr_t *) _ptr0, (uint64_t) device_type, NULL, NULL, (int32_t *) _ptr2);
  printf("%s", "post call");
  
  return (jlong) (intptr_t) _res;
}