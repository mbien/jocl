
    // OpenCL Kernel Function for element by element vector addition
    __kernel void VectorAddGM(__global const int* a, __global const int* b, __global int* c, int iNumElements) {
        // get index into global data array
        int iGID = get_global_id(0);
        // bound check (equivalent to the limit on a 'for' loop for standard/serial C code
        if (iGID >= iNumElements)  {
            return;
        }
        // add the vector elements
        c[iGID] = a[iGID] + b[iGID];
    }

    __kernel void Test(__global const int* a, __global const int* b, __global int* c, int iNumElements) {
        // get index into global data array
        int iGID = get_global_id(0);
        // bound check (equivalent to the limit on a 'for' loop for standard/serial C code
        if (iGID >= iNumElements)  {
            return;
        }
        c[iGID] = iGID;
    }
