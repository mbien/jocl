
      public long clCreateContext(IntBuffer properties, int offset1, long[] devices, CreateContextCallback cb, Object userData, IntBuffer errcode_ret, int offset2) {
          return this.clCreateContext0(properties, offset1, devices, cb, null, errcode_ret, offset2);
      }
      private native long clCreateContext0(IntBuffer properties, int size, long[] devices, CreateContextCallback pfn_notify, Object userData, IntBuffer errcode_ret, int size2);



      public long clCreateContextFromType(IntBuffer arg0, int offset1, long device_type, CreateContextCallback pfn_notify, Object userData, IntBuffer errcode_ret, int offset2) {
          return this.clCreateContextFromType0(arg0, offset1, device_type, pfn_notify, null, errcode_ret, offset2);
      }
      private native long clCreateContextFromType0(IntBuffer arg0, int size, long device_type, CreateContextCallback pfn_notify, Object userData, IntBuffer errcode_ret, int size2);

      