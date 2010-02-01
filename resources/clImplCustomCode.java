
    final static CLProcAddressTable addressTable = new CLProcAddressTable();

//    static{
//        ProcAddressHelper.resetProcAddressTable(addressTable, );
//    }

    public long clCreateContext(PointerBuffer properties, PointerBuffer devices, CreateContextCallback pfn_notify, Object userData, IntBuffer errcode_ret) {

        if(properties!=null && !properties.isDirect())
            throw new RuntimeException("Argument \"properties\" was not a direct buffer");

        if(pfn_notify != null)
            throw new RuntimeException("asynchronous execution with callback is not yet implemented, pass null through this method to block until complete.");

        if(userData != null)
            System.err.println("WARNING: userData not yet implemented... ignoring");

        return this.clCreateContext0(
                properties!=null?properties.getBuffer():null, BufferFactory.getDirectBufferByteOffset(properties),
                devices!=null?devices.getBuffer():null, BufferFactory.getDirectBufferByteOffset(devices),
                null, null,
                errcode_ret, BufferFactory.getDirectBufferByteOffset(errcode_ret) );
    }
    private native long clCreateContext0(Object cl_context_properties, int props_offset, Object devices, int devices_offset, CreateContextCallback pfn_notify, Object userData, Object errcode_ret, int err_offset);

        
    public long clCreateContextFromType(PointerBuffer properties, long device_type, CreateContextCallback pfn_notify, Object userData, IntBuffer errcode_ret) {

        if(properties!=null && !properties.isDirect())
            throw new RuntimeException("Argument \"properties\" was not a direct buffer");

        if(pfn_notify != null)
            throw new RuntimeException("asynchronous execution with callback is not yet implemented, pass null through this method to block until complete.");

        if(userData != null)
            System.err.println("WARNING: userData not yet implemented... ignoring");

        return this.clCreateContextFromType0(
                properties!=null?properties.getBuffer():null, BufferFactory.getDirectBufferByteOffset(properties),
                device_type, pfn_notify, null,
                errcode_ret, BufferFactory.getDirectBufferByteOffset(errcode_ret) );
    }
    private native long clCreateContextFromType0(Object properties, int props_offset, long device_type, CreateContextCallback pfn_notify, Object userData, Object errcode_ret, int err_offset);


    /** Interface to C language function: <br> <code> int32_t clBuildProgram(cl_program, uint32_t, cl_device_id * , const char * , void * ); </code>    */
    public int clBuildProgram(long program, int deviceCount, PointerBuffer deviceList, String options, BuildProgramCallback cb, Object userData)  {

        if(deviceList!=null && !deviceList.isDirect())
            throw new RuntimeException("Argument \"properties\" was not a direct buffer");

        if(cb != null)
            throw new RuntimeException("asynchronous execution with callback is not yet implemented, pass null through this method to block until complete.");

        if(userData != null)
            System.err.println("WARNING: userData not yet implemented... ignoring");

        return clBuildProgram0(program, deviceCount,
                               deviceList!=null?deviceList.getBuffer():null, BufferFactory.getDirectBufferByteOffset(deviceList),
                               options, cb, userData);
    }
    /** Entry point to C language function: <code> int32_t clBuildProgram(cl_program, uint32_t, cl_device_id * , const char * , void * ); </code>    */
    private native int clBuildProgram0(long program, int deviceCount, Object deviceList, int deviceListOffset, String options, BuildProgramCallback cb, Object userData);


    private final static void convert32To64(long[] values) {
        if(values.length%2 == 1) {
            values[values.length-1] = values[values.length/2]>>>32;
        }
        for (int i = values.length - 1 - values.length%2; i >= 0; i-=2) {
            long temp = values[i/2];
            values[i-1] = temp>>>32;
            values[i  ] = temp & 0x00000000FFFFFFFFL;
        }
    }
