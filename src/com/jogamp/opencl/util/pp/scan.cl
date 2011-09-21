// created on Wednesday, September 21 2011
//@author mbien

#ifdef DOUBLE_FP
    #ifdef cl_khr_fp64
        #pragma OPENCL EXTENSION cl_khr_fp64 : enable
    #elif defined(cl_amd_fp64)
        #pragma OPENCL EXTENSION cl_amd_fp64 : enable
    #else
        #error "Double precision floating point not supported."
    #endif
#endif

#ifndef TYPE
    typedef int TYPE;
    #warning "TYPE was not set"
#endif

/*
 * Work-efficient compute implementation of scan, one thread per 2 elements
 * O(log(n)) steps and O(n) adds using shared memory
 * Uses a balanced tree algorithm. See Belloch, 1990 "Prefix Sums and Their Applications"
 * Implementation is based on AMD's prefix sum example.
 * length must be smaller than workgroup size, vector types are not supported.
 */
kernel void smallScan(const global TYPE* input, global TYPE* output, local TYPE* block, const uint length) {

    const uint id = get_global_id(0);
    const uint id2= id*2;

    int offset = 1;

    /* Cache the computational window in shared memory */
    if(id2 < length - 1) {
        block[id2]     = input[id2];
        block[id2 + 1] = input[id2 + 1];
    }else if(id2 == length - 1) { // odd buffer length
        block[id2]     = input[id2];
        block[id2 + 1] = 0;
    }else{ // no-op when out of bounds
        block[id2]     = 0;
        block[id2 + 1] = 0;
    }

    const uint limit = (length-length%2);

    /* build the sum in place up the tree */
    for(int d = limit>>1; d > 0; d >>=1) {
        barrier(CLK_LOCAL_MEM_FENCE);

        if(id < d) {
            int ai = offset*(id2 + 1) - 1;
            int bi = offset*(id2 + 2) - 1;

            block[bi] += block[ai];
        }
        offset *= 2;
    }

    /* scan back down the tree */

    /* clear the last element */
    if(id == 0) {
        block[limit - 1] = 0;
    }

    /* traverse down the tree building the scan in the place */
    for(int d = 1; d < limit; d *= 2) {
        offset >>=1;
        barrier(CLK_LOCAL_MEM_FENCE);

        if(id < d) {
            int ai = offset*(id2 + 1) - 1;
            int bi = offset*(id2 + 2) - 1;

            TYPE t = block[ai];
            block[ai] = block[bi];
            block[bi] += t;
        }
    }

    barrier(CLK_LOCAL_MEM_FENCE);

    /*write the results back to global memory */
    if(id2 < length - 1) {
        output[id2]     = block[id2];
        output[id2 + 1] = block[id2 + 1];
    }else if(id2 == length - 1) { // odd length
        output[id2]     = block[id2]+block[id2-1];
    }
}

