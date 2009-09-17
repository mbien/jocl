
#if defined(_WIN32)
    #error windows does not support stdint.h
    // typedef signed   __int32 int32_t;
    // typedef unsigned __int32 uint32_t;
    // typedef signed   __int64 int64_t;
    // typedef unsigned __int64 uint64_t;
#else

    typedef signed   char     int8_t;
    typedef unsigned short    int16_t;
    typedef          int      int32_t;

    typedef unsigned char     uint8_t;
    typedef unsigned short    uint16_t;
    typedef unsigned int      uint32_t;

    typedef          int      intptr_t;
    typedef unsigned int      uintptr_t;

    typedef unsigned int      size_t;

    // FIXME workaround prevent re-defininition of int16_t in types.h
    #  define __int8_t_defined

    /* Greatest-width integer types */
    /* Modern GCCs provide __INTMAX_TYPE__ */
    #if defined(__INTMAX_TYPE__)
      typedef __INTMAX_TYPE__ intmax_t;
    #elif __have_longlong64
      typedef signed long long intmax_t;
    #else
      typedef signed long intmax_t;
    #endif

    /* Modern GCCs provide __UINTMAX_TYPE__ */
    #if defined(__UINTMAX_TYPE__)
      typedef __UINTMAX_TYPE__ uintmax_t;
    #elif __have_longlong64
      typedef unsigned long long uintmax_t;
    #else
      typedef unsigned long uintmax_t;
    #endif

    #if defined(__ia64__) || defined(__x86_64__)
        typedef signed   long int  int64_t;
        typedef unsigned long int uint64_t;
    #else
        typedef signed   long long int  int64_t;
        typedef unsigned long long int uint64_t;
    #endif
#endif
