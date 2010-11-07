#ifndef __stdint_h_
#define __stdint_h_

    /**
     * Look in the GlueGen.java API documentation for the build-in types (terminal symbols) 
     * definition.
     * 
     * The following types are build-in:
     *
     * __int32
     * int32_t
     * uint32_t
     * __int64
     * int64_t
     * uint64_t
     * ptrdiff_t
     * size_t
     */

    typedef signed   char     int8_t;
    typedef unsigned short    int16_t;

    typedef unsigned char     uint8_t;
    typedef unsigned short    uint16_t;

    typedef ptrdiff_t         intptr_t;
    typedef size_t            uintptr_t;

    /* Greatest-width integer types */
    /* Modern GCCs provide __INTMAX_TYPE__ */
    #if defined(__INTMAX_TYPE__)
      typedef __INTMAX_TYPE__ intmax_t;
    #elif __have_longlong64
      typedef signed long long intmax_t;
    #else
      typedef int64_t          intmax_t;
    #endif

    /* Modern GCCs provide __UINTMAX_TYPE__ */
    #if defined(__UINTMAX_TYPE__)
      typedef __UINTMAX_TYPE__   uintmax_t;
    #elif __have_longlong64
      typedef unsigned long long uintmax_t;
    #else
      typedef uint64_t           uintmax_t;
    #endif

#endif /*  __stdint_h_ */
