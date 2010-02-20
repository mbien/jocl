#ifdef _WIN32
#include    <windows.h>
#else
//just to make ANTLR happy
#define __GNUC__
#endif

#include    <CL/cl_platform.h>
#include    <CL/cl.h>
#include    <CL/cl_ext.h>

//#include    <GL/gl.h>
#include    <gltypes.h>
#include    <CL/cl_gl.h>
