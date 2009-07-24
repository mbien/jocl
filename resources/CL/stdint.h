#if defined(_WIN32)
    #error windows does not support stdint.h
    // typedef signed   __int32 int32_t;
    // typedef unsigned __int32 uint32_t;
    // typedef signed   __int64 int64_t;
    // typedef unsigned __int64 uint64_t;
#else

    typedef signed char       int8_t;
    typedef unsigned short    int16_t;
    typedef int               int32_t;
    typedef unsigned char     uint8_t;
    typedef unsigned short    uint16_t;
    typedef unsigned int      uint32_t;

    typedef int               intptr_t;
    typedef unsigned int      uintptr_t;

    typedef unsigned int      size_t;


    #if defined(__ia64__) || defined(__x86_64__)
        typedef signed   long int  int64_t;
        typedef unsigned long int uint64_t;
    #else
        typedef signed   long long int  int64_t;
        typedef unsigned long long int uint64_t;
    #endif
#endif
