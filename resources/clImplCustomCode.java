
        public long clCreateContext(IntBuffer properties, long[] devices, CreateContextCallback cb, Object userData, IntBuffer errcode_ret) {

            throw new RuntimeException("not yet implemented, use clCreateContextFromType instead");
//            return this.clCreateContext0(properties, offset1, devices, cb, null, errcode_ret, offset2);
        }
        private native long clCreateContext0(Object cl_context_properties, int props_offset, long[] devices, CreateContextCallback pfn_notify, Object userData, Object errcode_ret, int err_offset);

        
        public long clCreateContextFromType(IntBuffer properties, long device_type, CreateContextCallback pfn_notify, Object userData, IntBuffer errcode_ret) {

            if(pfn_notify != null)
                throw new RuntimeException("asynchronous execution with callback is not yet implemented, pass null through this method to block until complete.");

            if(userData != null)
                System.err.println("WARNING: userData not yet implemented... ignoring");

            return this.clCreateContextFromType1(
                    BufferFactory.getArray(properties), BufferFactory.getIndirectBufferByteOffset(properties), device_type, pfn_notify, null,
                    BufferFactory.getArray(errcode_ret), BufferFactory.getIndirectBufferByteOffset(errcode_ret) );
        }
        private native long clCreateContextFromType1(Object properties, int props_offset, long device_type, CreateContextCallback pfn_notify, Object userData, Object errcode_ret, int err_offset);


        /** Interface to C language function: <br> <code> int32_t clBuildProgram(cl_program, uint32_t, cl_device_id * , const char * , void * ); </code>    */
        public int clBuildProgram(long program, long[] deviceList, String options, BuildProgramCallback cb, Object userData)  {

              if(cb != null)
                  throw new RuntimeException("asynchronous execution with callback is not yet implemented, pass null through this method to block until complete.");

              if(userData != null)
                  System.err.println("WARNING: userData not yet implemented... ignoring");

              int listLength = 0;
              if(deviceList != null)
                  listLength = deviceList.length;

              return clBuildProgram0(program, listLength, deviceList, 0, options, cb, userData);
        }
        /** Entry point to C language function: <code> int32_t clBuildProgram(cl_program, uint32_t, cl_device_id * , const char * , void * ); </code>    */
        private native int clBuildProgram0(long program, int devices, Object deviceList, int arg2_byte_offset, String options, BuildProgramCallback cb, Object userData);

