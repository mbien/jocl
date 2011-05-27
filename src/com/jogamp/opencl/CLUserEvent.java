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

/*
 * Created on Wednesday, June 16 2010 18:05
 */

package com.jogamp.opencl;

import static com.jogamp.opencl.CLException.*;
import static com.jogamp.opencl.llb.CL.*;

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
