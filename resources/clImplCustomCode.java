
      public long clCreateContext(IntBuffer properties, int arg1, long[] devices, CreateContextCallback cb, Object userData, IntBuffer errcode_ret) {
          return this.clCreateContext0(properties, arg1, devices, cb, null, errcode_ret);
      }

      public native long clCreateContext0(IntBuffer properties, int arg1, long[] devices, CreateContextCallback cb, Object userData, IntBuffer errcode_ret);

      public long clCreateContextFromType(IntBuffer arg0, long device_type, CreateContextCallback pfn_notify, Object userData, IntBuffer errcode_ret) {
          return this.clCreateContextFromType0(arg0, device_type, pfn_notify, null, errcode_ret);
      }

      public native long clCreateContextFromType0(IntBuffer arg0, long device_type, Object pfn_notify, Object userData, IntBuffer errcode_ret);
      