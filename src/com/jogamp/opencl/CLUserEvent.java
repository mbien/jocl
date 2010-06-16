/*
 * Created on Wednesday, June 16 2010 18:05
 */

package com.jogamp.opencl;

import static com.jogamp.opencl.CLException.*;
import static com.jogamp.opencl.CL.*;

/**
 * Custom, user controlled event.
 * @see CLEvent
 * @author Michael Bien
 */
public class CLUserEvent extends CLEvent {

    CLUserEvent(CLContext context, long ID) {
        super(context, ID);
    }

    /**
     * Creates a new user event.
     */
    public static CLUserEvent create(CLContext context) {
        int[] error = new int[1];
        long ID = context.cl.clCreateUserEvent(context.ID, error, 0);
        checkForError(error[0], "can not create user event.");
        return new CLUserEvent(context, ID);
    }

    /**
     * Sets the event execution status.
     * Calls {@native clSetUserEventStatus}.
     */
    public CLUserEvent setStatus(CLEvent.ExecutionStatus status) {
        int err = cl.clSetUserEventStatus(ID, status.STATUS);
        if(err != CL_SUCCESS) {
            newException(err, "can not set status "+status);
        }
        return this;
    }

    /**
     * Sets this event's status to {@link CLEvent.ExecutionStatus#COMPLETE}.
     * @see #setStatus(com.jogamp.opencl.CLEvent.ExecutionStatus)
     */
    public CLUserEvent setComplete() {
        return setStatus(ExecutionStatus.COMPLETE);
    }

    /**
     * Returns {@link CLEvent.CommandType#USER}.
     */
    @Override
    public CommandType getType() {
        return CommandType.USER;
    }

}
