/*
 * Copyright 2009 - 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package com.jogamp.opencl;

import com.jogamp.opencl.impl.CLEventCallback;
import com.jogamp.common.nio.NativeSizeBuffer;
import java.nio.Buffer;

import static com.jogamp.opencl.CL.*;
import static com.jogamp.opencl.CLException.*;

/**
 * Event objects can be used for synchronizing command queues, e.g you can wait until a
 * event occurs or they can also be used to capture profiling information that
 * measure execution time of a command.
 * Profiling of OpenCL commands can be enabled by using a {@link com.jogamp.opencl.CLCommandQueue} created with
 * {@link com.jogamp.opencl.CLCommandQueue.Mode#PROFILING_MODE}.
 * @author Michael Bien
 */
public class CLEvent extends CLObject implements CLResource {

    private final CLEventInfoAccessor eventInfo;
    private final CLEventProfilingInfoAccessor eventProfilingInfo;

    CLEvent(CLContext context, long id) {
        super(context, id);
        this.eventInfo = new CLEventInfoAccessor();
        this.eventProfilingInfo = new CLEventProfilingInfoAccessor();
    }

    /**
     * Registers a callback which will be called when the event terminates (COMPLETE or ERROR).
     */
    public void registerCallback(final CLEventListener callback) {
        this.registerCallback(callback, ExecutionStatus.COMPLETE);
    }

    // apparently only ExecutionStatus.COMPLETE is allowed -> private
    private void registerCallback(final CLEventListener callback, ExecutionStatus trigger) {
        cl.clSetEventCallback(ID, trigger.STATUS, new CLEventCallback() {
            public void eventStateChanged(long event, int status) {
                callback.eventStateChanged(CLEvent.this, status);
            }
        });
    }

    public void release() {
        int ret = cl.clReleaseEvent(ID);
        checkForError(ret, "can not release event");
    }

    /**
     * Returns the execution status of the command which triggers this event.
     */
    public ExecutionStatus getStatus() {
        return ExecutionStatus.valueOf(getStatusCode());
    }

    /**
     * Returns true only if {@link #getStatus} returns {@link ExecutionStatus#COMPLETE}.
     */
    public boolean isComplete() {
        return ExecutionStatus.COMPLETE.equals(getStatus());
    }
    
    public int getStatusCode() {
        return (int)eventInfo.getLong(CL_EVENT_COMMAND_EXECUTION_STATUS);
    }

    public CommandType getType() {
        int status = (int)eventInfo.getLong(CL_EVENT_COMMAND_TYPE);
        return CommandType.valueOf(status);
    }

    public long getProfilingInfo(ProfilingCommand command) {
        return eventProfilingInfo.getLong(command.COMMAND);
    }


    @Override
    public String toString() {
        return getClass().getSimpleName()+" [id: " + ID
                      + " name: " + getType()
                      + " status: " + getStatus()+"]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CLEvent other = (CLEvent) obj;
        if (this.context != other.context && (this.context == null || !this.context.equals(other.context))) {
            return false;
        }
        if (this.ID != other.ID) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 13 * hash + (this.context != null ? this.context.hashCode() : 0);
        hash = 13 * hash + (int) (this.ID ^ (this.ID >>> 32));
        return hash;
    }

    

    private class CLEventInfoAccessor extends CLInfoAccessor {

        @Override
        protected int getInfo(int name, long valueSize, Buffer value, NativeSizeBuffer valueSizeRet) {
            return cl.clGetEventInfo(ID, name, valueSize, value, valueSizeRet);
        }

    }

    private class CLEventProfilingInfoAccessor extends CLInfoAccessor {

        @Override
        protected int getInfo(int name, long valueSize, Buffer value, NativeSizeBuffer valueSizeRet) {
            return cl.clGetEventProfilingInfo(ID, name, valueSize, value, valueSizeRet);
        }

    }

    // TODO merge with ExecutionStatus?

    public enum ProfilingCommand {

        /**
         * A 64-bit value that describes the current device time counter in nanoseconds
         * when the command identified by event is enqueued in a command-queue by the host.
         */
        QUEUED(CL_PROFILING_COMMAND_QUEUED),

        /**
         * A 64-bit value that describes the current device time counter in nanoseconds when
         * the command identified by event that has been enqueued is submitted by the host to
         * the device associated with the commandqueue.
         */
        SUBMIT(CL_PROFILING_COMMAND_SUBMIT),

        /**
         * A 64-bit value that describes the current device time counter in nanoseconds when
         * the command identified by event starts execution on the device.
         */
        START(CL_PROFILING_COMMAND_START),

        /**
         * A 64-bit value that describes the current device time counter in nanoseconds when
         * the command identified by event has finished execution on the device.
         */
        END(CL_PROFILING_COMMAND_END);

        /**
         * Value of wrapped OpenCL profiling command.
         */
        public final int COMMAND;

        private ProfilingCommand(int command) {
            this.COMMAND = command;
        }

        public static ProfilingCommand valueOf(int status) {
            switch(status) {
                case(CL_PROFILING_COMMAND_QUEUED):
                    return QUEUED;
                case(CL_PROFILING_COMMAND_SUBMIT):
                    return SUBMIT;
                case(CL_PROFILING_COMMAND_START):
                    return START;
                case(CL_PROFILING_COMMAND_END):
                    return END;
            }
            return null;
        }

    }



    public enum ExecutionStatus {

        /**
         * Command has been enqueued in the command-queue.
         */
        QUEUED(CL_QUEUED),

        /**
         * Enqueued command has been submitted by the host to the device
         * associated with the command-queue.
         */
        SUBMITTED(CL_SUBMITTED),
        
        /**
         * Device is currently executing this command.
         */
        RUNNING(CL_RUNNING),

        /**
         * The command has completed.
         */
        COMPLETE(CL_COMPLETE),

        /**
         * The command did not complete because of an error.
         */
        ERROR(-1);


        /**
         * Value of wrapped OpenCL command execution status.
         */
        public final int STATUS;

        private ExecutionStatus(int status) {
            this.STATUS = status;
        }

        public static ExecutionStatus valueOf(int status) {
            switch(status) {
                case(CL_QUEUED):
                    return QUEUED;
                case(CL_SUBMITTED):
                    return SUBMITTED;
                case(CL_RUNNING):
                    return RUNNING;
                case(CL_COMPLETE):
                    return COMPLETE;
            }
            if(status < 0) {
                return ERROR;
            }
            return null;
        }
    }

    public enum CommandType {

        NDRANGE_KERNEL(CL_COMMAND_NDRANGE_KERNEL),
        TASK(CL_COMMAND_TASK),
        NATIVE_KERNEL(CL_COMMAND_NATIVE_KERNEL),
        READ_BUFFER(CL_COMMAND_READ_BUFFER),
        WRITE_BUFFER(CL_COMMAND_WRITE_BUFFER),
        COPY_BUFFER(CL_COMMAND_COPY_BUFFER),
        READ_IMAGE(CL_COMMAND_READ_IMAGE),
        WRITE_IMAGE(CL_COMMAND_WRITE_IMAGE),
        COPY_IMAGE(CL_COMMAND_COPY_IMAGE),
        COPY_BUFFER_TO_IMAGE(CL_COMMAND_COPY_BUFFER_TO_IMAGE),
        COPY_IMAGE_TO_BUFFER(CL_COMMAND_COPY_IMAGE_TO_BUFFER),
        MAP_BUFFER(CL_COMMAND_MAP_BUFFER),
        MAP_IMAGE(CL_COMMAND_MAP_IMAGE),
        UNMAP_MEM_OBJECT(CL_COMMAND_UNMAP_MEM_OBJECT),
        MARKER(CL_COMMAND_MARKER),
        READ_BUFFER_RECT(CL_COMMAND_READ_BUFFER_RECT),
        WRITE_BUFFER_RECT(CL_COMMAND_WRITE_BUFFER_RECT),
        COPY_BUFFER_RECT(CL_COMMAND_COPY_BUFFER_RECT),
        USER(CL_COMMAND_USER),
        ACQUIRE_GL_OBJECTS(CL_COMMAND_ACQUIRE_GL_OBJECTS),
        RELEASE_GL_OBJECTS(CL_COMMAND_RELEASE_GL_OBJECTS),
        GL_FENCE_SYNC_OBJECT_KHR(CL_COMMAND_GL_FENCE_SYNC_OBJECT_KHR);

        /**
         * Value of wrapped OpenCL command type.
         */
        public final int TYPE;

        private CommandType(int type) {
            this.TYPE = type;
        }

        public static CommandType valueOf(int commandType) {
            CommandType[] values = CommandType.values();
            for (CommandType value : values) {
                if(value.TYPE == commandType)
                    return value;
            }
            return null;
        }

    }

}
