
    private final CLProcAddressTable addressTable;

    //maps the context id to its error handler's global object pointer
    private final com.jogamp.common.util.LongLongHashMap contextCallbackMap;

    public CLImpl(CLProcAddressTable addressTable) {
        this.addressTable = addressTable;
        this.contextCallbackMap = new com.jogamp.common.util.LongLongHashMap();
        this.contextCallbackMap.setKeyNotFoundValue(0);
    }

    public long clCreateContext(PointerBuffer properties, PointerBuffer devices, CLErrorHandler pfn_notify, IntBuffer errcode_ret) {

        if(properties!=null && !properties.isDirect())
            throw new RuntimeException("Argument \"properties\" was not a direct buffer");

        long[] global = new long[1];
        long ctx = this.clCreateContext0(
                properties!=null?properties.getBuffer():null, Buffers.getDirectBufferByteOffset(properties),
                devices!=null?devices.remaining():0, devices!=null?devices.getBuffer():null, Buffers.getDirectBufferByteOffset(devices),
                pfn_notify, global, errcode_ret, Buffers.getDirectBufferByteOffset(errcode_ret) );

        if(pfn_notify != null && global[0] != 0) {
            synchronized(contextCallbackMap) {
                contextCallbackMap.put(ctx, global[0]);
            }
        }
        return ctx;
    }
    private native long clCreateContext0(Object cl_context_properties, int props_offset, int numDevices, Object devices, int devices_offset, CLErrorHandler pfn_notify, long[] global, Object errcode_ret, int err_offset);

        
    public long clCreateContextFromType(PointerBuffer properties, long device_type, CLErrorHandler pfn_notify, IntBuffer errcode_ret) {

        if(properties!=null && !properties.isDirect())
            throw new RuntimeException("Argument \"properties\" was not a direct buffer");

        long[] global = new long[1];
        long ctx = this.clCreateContextFromType0(
                properties!=null?properties.getBuffer():null, Buffers.getDirectBufferByteOffset(properties),
                device_type, pfn_notify, global, errcode_ret, Buffers.getDirectBufferByteOffset(errcode_ret) );

        if(pfn_notify != null && global[0] != 0) {
            synchronized(contextCallbackMap) {
                contextCallbackMap.put(ctx, global[0]);
            }
        }
        return ctx;
    }
    private native long clCreateContextFromType0(Object properties, int props_offset, long device_type, CLErrorHandler pfn_notify, long[] global, Object errcode_ret, int err_offset);

    
    public int clReleaseContext(long context) {
        long global = 0;
        synchronized(contextCallbackMap) {
            global = contextCallbackMap.remove(context);
        }
        return clReleaseContextImpl(context, global);
    }

    /** Interface to C language function: <br> <code> int32_t {@native clReleaseContext}(cl_context context); </code>    */
    public native int clReleaseContextImpl(long context, long global);


    /** Interface to C language function: <br> <code> int32_t clBuildProgram(cl_program, uint32_t, cl_device_id * , const char * , void * ); </code>    */
    public int clBuildProgram(long program, int deviceCount, PointerBuffer deviceList, String options, BuildProgramCallback cb)  {

        if(deviceList!=null && !deviceList.isDirect())
            throw new RuntimeException("Argument \"properties\" was not a direct buffer");

        return clBuildProgram0(program, deviceCount, deviceList!=null?deviceList.getBuffer():null,
                               Buffers.getDirectBufferByteOffset(deviceList), options, cb);
    }
    /** Entry point to C language function: <code> int32_t clBuildProgram(cl_program, uint32_t, cl_device_id * , const char * , void * ); </code>    */
    private native int clBuildProgram0(long program, int deviceCount, Object deviceList, int deviceListOffset, String options, BuildProgramCallback cb);


  /** Interface to C language function: <br> <code> void *  {@native clEnqueueMapImage}(cl_command_queue command_queue, cl_mem image, uint32_t blocking_map, uint64_t map_flags, const size_t * , const size_t * , size_t *  image_row_pitch, size_t *  image_slice_pitch, uint32_t num_events_in_wait_list, cl_event *  event_wait_list, cl_event *  event, int32_t *  errcode_ret); </code>
  @param origin a direct {@link com.jogamp.common.nio.PointerBuffer}
  @param range a direct {@link com.jogamp.common.nio.PointerBuffer}
  @param image_row_pitch a direct {@link com.jogamp.common.nio.PointerBuffer}
  @param image_slice_pitch a direct {@link com.jogamp.common.nio.PointerBuffer}
  @param event_wait_list a direct {@link com.jogamp.common.nio.PointerBuffer}
  @param event a direct {@link com.jogamp.common.nio.PointerBuffer}
  @param errcode_ret a direct {@link java.nio.IntBuffer}   */
  public java.nio.ByteBuffer clEnqueueMapImage(long command_queue, long image, int blocking_map, long map_flags,
              Int64Buffer origin, Int64Buffer range,
              Int64Buffer image_row_pitch, Int64Buffer image_slice_pitch,
              int num_events_in_wait_list,
              PointerBuffer event_wait_list, PointerBuffer event, java.nio.IntBuffer errcode_ret)  {

    if (!Buffers.isDirect(origin))
      throw new CLException("Argument \"origin\" was not a direct buffer");
    if (!Buffers.isDirect(range))
      throw new CLException("Argument \"range\" was not a direct buffer");
    if (!Buffers.isDirect(image_row_pitch))
      throw new CLException("Argument \"image_row_pitch\" was not a direct buffer");
    if (!Buffers.isDirect(image_slice_pitch))
      throw new CLException("Argument \"image_slice_pitch\" was not a direct buffer");
    if (!Buffers.isDirect(event_wait_list))
      throw new CLException("Argument \"event_wait_list\" was not a direct buffer");
    if (!Buffers.isDirect(event))
      throw new CLException("Argument \"event\" was not a direct buffer");
    if (!Buffers.isDirect(errcode_ret))
      throw new CLException("Argument \"errcode_ret\" was not a direct buffer");

    java.nio.ByteBuffer _res;
    _res = clEnqueueMapImage0(command_queue, image, blocking_map, map_flags, origin!=null?origin.getBuffer():null,
            Buffers.getDirectBufferByteOffset(origin), range!=null?range.getBuffer():null,
            Buffers.getDirectBufferByteOffset(range), image_row_pitch!=null?image_row_pitch.getBuffer():null,
            Buffers.getDirectBufferByteOffset(image_row_pitch), image_slice_pitch!=null?image_slice_pitch.getBuffer():null,
            Buffers.getDirectBufferByteOffset(image_slice_pitch), num_events_in_wait_list,
            event_wait_list!=null?event_wait_list.getBuffer():null, Buffers.getDirectBufferByteOffset(event_wait_list),
            event!=null?event.getBuffer():null, Buffers.getDirectBufferByteOffset(event), errcode_ret,
            Buffers.getDirectBufferByteOffset(errcode_ret));
    if (_res == null) return null;
    Buffers.nativeOrder(_res);
    return _res;
  }

  /** Entry point to C language function: <code> void *  {@native clEnqueueMapImage}(cl_command_queue command_queue, cl_mem image, uint32_t blocking_map, uint64_t map_flags, const size_t * , const size_t * , size_t *  image_row_pitch, size_t *  image_slice_pitch, uint32_t num_events_in_wait_list, cl_event *  event_wait_list, cl_event *  event, int32_t *  errcode_ret); </code>
      @param origin a direct {@link com.jogamp.gluegen.runtime.PointerBuffer}
      @param range a direct {@link com.jogamp.gluegen.runtime.PointerBuffer}
      @param image_row_pitch a direct {@link com.jogamp.gluegen.runtime.PointerBuffer}
      @param image_slice_pitch a direct {@link com.jogamp.gluegen.runtime.PointerBuffer}
      @param event_wait_list a direct {@link com.jogamp.gluegen.runtime.PointerBuffer}
      @param event a direct {@link com.jogamp.gluegen.runtime.PointerBuffer}
      @param errcode_ret a direct {@link java.nio.IntBuffer}   */
  private native ByteBuffer clEnqueueMapImage0(long command_queue, long image, int blocking_map, long map_flags, Object origin, int origin_byte_offset, Object range, int range_byte_offset, Object image_row_pitch, int image_row_pitch_byte_offset, Object image_slice_pitch, int image_slice_pitch_byte_offset, int num_events_in_wait_list, Object event_wait_list, int event_wait_list_byte_offset, Object event, int event_byte_offset, Object errcode_ret, int errcode_ret_byte_offset);


    /**
     * Returns the extension function address for the given function name.
     */
    public long clGetExtensionFunctionAddress(String name)  {
        ByteBuffer res = clGetExtensionFunctionAddressImpl(name);
        if(Platform.is32Bit()) {
            return res.getInt();
        }else{
            return res.getLong();
        }
    }

/*
    private static void convert32To64(long[] values) {
        if(values.length%2 == 1) {
            values[values.length-1] = values[values.length/2]>>>32;
        }
        for (int i = values.length - 1 - values.length%2; i >= 0; i-=2) {
            long temp = values[i/2];
            values[i-1] = temp>>>32;
            values[i  ] = temp & 0x00000000FFFFFFFFL;
        }
    }
*/