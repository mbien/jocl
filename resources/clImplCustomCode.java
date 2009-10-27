
        public long clCreateContext(IntBuffer properties, long[] devices, CreateContextCallback pfn_notify, Object userData, IntBuffer errcode_ret) {

            if(pfn_notify != null)
                throw new RuntimeException("asynchronous execution with callback is not yet implemented, pass null through this method to block until complete.");

            if(userData != null)
                System.err.println("WARNING: userData not yet implemented... ignoring");

            int listLength = 0;
            if(devices != null)
                listLength = devices.length;

            return this.clCreateContext1(
                    BufferFactory.getArray(properties), BufferFactory.getIndirectBufferByteOffset(properties), listLength, devices, null, null,
                    BufferFactory.getArray(errcode_ret), BufferFactory.getIndirectBufferByteOffset(errcode_ret) );
        }
        private native long clCreateContext1(Object cl_context_properties, int props_offset, int deviceCount, long[] devices, CreateContextCallback pfn_notify, Object userData, Object errcode_ret, int err_offset);

        
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

              return clBuildProgram1(program, listLength, deviceList, options, cb, userData);
        }
        /** Entry point to C language function: <code> int32_t clBuildProgram(cl_program, uint32_t, cl_device_id * , const char * , void * ); </code>    */
        private native int clBuildProgram1(long program, int devices, Object deviceList, String options, BuildProgramCallback cb, Object userData);

