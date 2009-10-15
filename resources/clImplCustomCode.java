
        public long clCreateContext(IntBuffer properties, int offset1, long[] devices, CreateContextCallback cb, Object userData, IntBuffer errcode_ret, int offset2) {

            throw new RuntimeException("not yet implemented, use clCreateContextFromType instead");
//            return this.clCreateContext0(properties, offset1, devices, cb, null, errcode_ret, offset2);
        }
        private native long clCreateContext0(IntBuffer cl_context_properties, int size, long[] devices, CreateContextCallback pfn_notify, Object userData, IntBuffer errcode_ret, int size2);


        public long clCreateContextFromType(IntBuffer properties, int offset1, long device_type, CreateContextCallback pfn_notify, Object userData, IntBuffer errcode_ret, int offset2) {

            if(pfn_notify != null)
                throw new RuntimeException("asynchronous execution with callback is not yet implemented, pass null through this method to block until complete.");

            if(userData != null)
                System.out.println("WARNING: userData not yet implemented... ignoring");

            return this.clCreateContextFromType0(properties, offset1, device_type, pfn_notify, null, errcode_ret, offset2);
        }
        private native long clCreateContextFromType0(IntBuffer properties, int size, long device_type, CreateContextCallback pfn_notify, Object userData, IntBuffer errcode_ret, int size2);


        /** Interface to C language function: <br> <code> int32_t clBuildProgram(cl_program, uint32_t, cl_device_id * , const char * , void * ); </code>    */
        public int clBuildProgram(long program, long[] deviceList, String options, BuildProgramCallback cb, Object userData)  {

              if(cb != null)
                  throw new RuntimeException("asynchronous execution with callback is not yet implemented, pass null through this method to block until complete.");

              if(userData != null)
                  System.out.println("WARNING: userData not yet implemented... ignoring");

              int listLength = 0;
              if(deviceList != null)
                  listLength = deviceList.length;

              return clBuildProgram0(program, listLength, deviceList, 0, options, cb, userData);
        }
        /** Entry point to C language function: <code> int32_t clBuildProgram(cl_program, uint32_t, cl_device_id * , const char * , void * ); </code>    */
        private native int clBuildProgram0(long program, int devices, Object deviceList, int arg2_byte_offset, String options, BuildProgramCallback cb, Object userData);

