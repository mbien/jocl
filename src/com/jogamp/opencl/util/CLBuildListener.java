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
 * Sunday, May 02 2010 20:38
 */

package com.jogamp.opencl.util;

import com.jogamp.opencl.CLProgram;

/**
 * A callback an application can register to be called when the program executable
 * has been built (successfully or unsuccessfully).<br/>
 * Note1: registering a build callback can make {@link com.jogamp.opencl.CL#clBuildProgram} non blocking (OpenCL implementation dependent).<br/>
 * Note2: the thread which calls this method is unspecified. The Application should ensure propper synchronization.
 * @author Michael Bien
 * @see com.jogamp.opencl.CL#clBuildProgram(long, int, com.jogamp.common.nio.NativeSizeBuffer, java.lang.String, com.jogamp.opencl.impl.BuildProgramCallback)
 */
public interface CLBuildListener {

    /**
     * Called when the program executable
     * has been built (successfully or unsuccessfully).
     */
    public void buildFinished(CLProgram program);

}
