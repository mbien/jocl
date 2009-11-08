package com.mbien.opencl;

/**
 * Main Exception type for runtime OpenCL errors and unsuccessfull function calls (e.g. returning other values than CL_SUCCESS).
 * @author Michael Bien
 */
public class CLException extends RuntimeException {

    public final int errorcode;

//    public CLException(Throwable cause) {
//        super(cause);
//    }
//
//    public CLException(String message, Throwable cause) {
//        super(message, cause);
//    }
//
//    public CLException(String message) {
//        super(message);
//    }

    public CLException(int error, String message) {
        super(identifyError(error) + ": " + message);
        errorcode = error;
    }

    public static final void checkForError(int status, String message) {
        if(status != CL.CL_SUCCESS)
            throw new CLException(status, message);
    }


    private static final String identifyError(int error) {

        switch (error) {
            case CL.CL_DEVICE_NOT_FOUND:
                return "CL_DEVICE_NOT_FOUND";

            case CL.CL_DEVICE_NOT_AVAILABLE:
                return "CL_DEVICE_NOT_AVAILABLE";

            case CL.CL_COMPILER_NOT_AVAILABLE:
                return "CL_COMPILER_NOT_AVAILABLE";

            case CL.CL_MEM_OBJECT_ALLOCATION_FAILURE:
                return "CL_MEM_OBJECT_ALLOCATION_FAILURE";

            case CL.CL_OUT_OF_RESOURCES:
                return "CL_OUT_OF_RESOURCES";

            case CL.CL_OUT_OF_HOST_MEMORY:
                return "CL_OUT_OF_HOST_MEMORY";

            case CL.CL_PROFILING_INFO_NOT_AVAILABLE:
                return "CL_PROFILING_INFO_NOT_AVAILABLE";

            case CL.CL_MEM_COPY_OVERLAP:
                return "CL_MEM_COPY_OVERLAP";

            case CL.CL_IMAGE_FORMAT_MISMATCH:
                return "CL_IMAGE_FORMAT_MISMATCH";

            case CL.CL_IMAGE_FORMAT_NOT_SUPPORTED:
                return "CL_IMAGE_FORMAT_NOT_SUPPORTED";

            case CL.CL_BUILD_PROGRAM_FAILURE:
                return "CL_BUILD_PROGRAM_FAILURE";

            case CL.CL_MAP_FAILURE:
                return "CL_MAP_FAILURE";

            case CL.CL_INVALID_VALUE:
                return "CL_INVALID_VALUE";

            case CL.CL_INVALID_DEVICE_TYPE:
                return "CL_INVALID_DEVICE_TYPE";

            case CL.CL_INVALID_PLATFORM:
                return "CL_INVALID_PLATFORM";

            case CL.CL_INVALID_DEVICE:
                return "CL_INVALID_DEVICE";

            case CL.CL_INVALID_CONTEXT:
                return "CL_INVALID_CONTEXT";

            case CL.CL_INVALID_QUEUE_PROPERTIES:
                return "CL_INVALID_QUEUE_PROPERTIES";

            case CL.CL_INVALID_COMMAND_QUEUE:
                return "CL_INVALID_COMMAND_QUEUE";

            case CL.CL_INVALID_HOST_PTR:
                return "CL_INVALID_HOST_PTR";

            case CL.CL_INVALID_MEM_OBJECT:
                return "CL_INVALID_MEM_OBJECT";

            case CL.CL_INVALID_IMAGE_FORMAT_DESCRIPTOR:
                return "CL_INVALID_IMAGE_FORMAT_DESCRIPTOR";

            case CL.CL_INVALID_IMAGE_SIZE:
                return "CL_INVALID_IMAGE_SIZE";

            case CL.CL_INVALID_SAMPLER:
                return "CL_INVALID_SAMPLER";

            case CL.CL_INVALID_BINARY:
                return "CL_INVALID_BINARY";

            case CL.CL_INVALID_BUILD_OPTIONS:
                return "CL_INVALID_BUILD_OPTIONS";

            case CL.CL_INVALID_PROGRAM:
                return "CL_INVALID_PROGRAM";

            case CL.CL_INVALID_PROGRAM_EXECUTABLE:
                return "CL_INVALID_PROGRAM_EXECUTABLE";

            case CL.CL_INVALID_KERNEL_NAME:
                return "CL_INVALID_KERNEL_NAME";

            case CL.CL_INVALID_KERNEL_DEFINITION:
                return "CL_INVALID_KERNEL_DEFINITION";

            case CL.CL_INVALID_KERNEL:
                return "CL_INVALID_KERNEL";

            case CL.CL_INVALID_ARG_INDEX:
                return "CL_INVALID_ARG_INDEX";

            case CL.CL_INVALID_ARG_VALUE:
                return "CL_INVALID_ARG_VALUE";

            case CL.CL_INVALID_ARG_SIZE:
                return "CL_INVALID_ARG_SIZE";

            case CL.CL_INVALID_KERNEL_ARGS:
                return "CL_INVALID_KERNEL_ARGS";

            case CL.CL_INVALID_WORK_DIMENSION:
                return "CL_INVALID_WORK_DIMENSION";

            case CL.CL_INVALID_WORK_GROUP_SIZE:
                return "CL_INVALID_WORK_GROUP_SIZE";

            case CL.CL_INVALID_WORK_ITEM_SIZE:
                return "CL_INVALID_WORK_ITEM_SIZE";

            case CL.CL_INVALID_GLOBAL_OFFSET:
                return "CL_INVALID_GLOBAL_OFFSET";

            case CL.CL_INVALID_EVENT_WAIT_LIST:
                return "CL_INVALID_EVENT_WAIT_LIST";

            case CL.CL_INVALID_EVENT:
                return "CL_INVALID_EVENT";

            case CL.CL_INVALID_OPERATION:
                return "CL_INVALID_OPERATION";

            case CL.CL_INVALID_GL_OBJECT:
                return "CL_INVALID_GL_OBJECT";

            case CL.CL_INVALID_BUFFER_SIZE:
                return "CL_INVALID_BUFFER_SIZE";

            case CL.CL_INVALID_MIP_LEVEL:
                return "CL_INVALID_MIP_LEVEL";

            case CL.CL_INVALID_GLOBAL_WORK_SIZE:
                return "CL_INVALID_GLOBAL_WORK_SIZE or CL_INVALID_GL_SHAREGROUP_REFERENCE_KHR";

// error-code conflict with CL_INVALID_GLOBAL_WORK_SIZE
//            case CL.CL_INVALID_GL_SHAREGROUP_REFERENCE_KHR:
//                return "CL_INVALID_GL_SHAREGROUP_REFERENCE_KHR";

            default:
                return "unknown cause: error " + error;
        }
    }


}
