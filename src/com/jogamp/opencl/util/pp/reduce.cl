// created on Sep 8, 2011
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
    typedef int4 TYPE;
    #warning "TYPE was not set"
#endif

inline TYPE op(TYPE a, TYPE b) {
#if OP_ADD
    return a+b;
#elif OP_MUL
    return a*b;
#elif OP_MIN
    return min(a, b);
#elif OP_MAX
    return max(a, b);
#else
    return 0;
    #warning "operation was not set"
#endif
}

kernel void reduce(const global TYPE* input, global TYPE* output, local TYPE* shared, const uint length) {

    uint localID = get_local_id(0);
    uint groupID = get_group_id(0);
    uint globalID = get_global_id(0);

    uint groupSize = get_local_size(0);
    uint stride = globalID * 2;

    // store results of first round in cache
    if(globalID < length) {
        shared[localID] = op(input[stride], input[stride+1]);
    }else{
    // no-ops if out of bounds
    #if OP_ADD
        shared[localID] = 0;
    #elif OP_MUL
        shared[localID] = 1;
    #elif OP_MIN
        shared[localID] = shared[0];
    #elif OP_MAX
        shared[localID] = shared[0];
    #endif
    }

    barrier(CLK_LOCAL_MEM_FENCE);

    // recursive reduction
    for(uint i = groupSize >> 1; i > 0; i >>= 1) {
        if(localID < i) {
            shared[localID] = op(shared[localID], shared[localID + i]);
        }
        barrier(CLK_LOCAL_MEM_FENCE);
    }

    // return first element as result
    if(localID == 0) {
        output[groupID] = shared[0];
    }
}
