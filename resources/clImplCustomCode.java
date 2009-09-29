
      public long clCreateContext(IntBuffer properties, int arg1, long[] devices, CreateContextCallback cb, Object userData, IntBuffer errcode_ret) {
          return this.clCreateContext0(properties, arg1, devices, cb, null, errcode_ret, 0);
      }
      private native long clCreateContext0(IntBuffer properties, int size, long[] devices, CreateContextCallback pfn_notify, Object userData, IntBuffer errcode_ret, int size2);



      public long clCreateContextFromType(IntBuffer arg0, long device_type, CreateContextCallback pfn_notify, Object userData, IntBuffer errcode_ret) {
          return this.clCreateContextFromType0(arg0, 0, device_type, pfn_notify, null, errcode_ret, 0);
      }
      private native long clCreateContextFromType0(IntBuffer arg0, int size, long device_type, CreateContextCallback pfn_notify, Object userData, IntBuffer errcode_ret, int size2);

      