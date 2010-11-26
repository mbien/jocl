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

package com.jogamp.opencl.util;

import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLProgram;
import java.util.Map;

/**
 * Configuration representing everything needed to build an OpenCL program (program included).
 * CLProgramConfiguration is a helper for building programs with more complex configurations or
 * building multiple programs with the similar configuration.
 * @see CLProgram#prepare()
 * @see com.jogamp.opencl.CLProgramBuilder#createConfiguration(com.jogamp.opencl.CLProgram)
 * @see com.jogamp.opencl.CLProgramBuilder#loadConfiguration(java.io.ObjectInputStream, com.jogamp.opencl.CLContext)
 * @author Michael Bien
 */
public interface CLProgramConfiguration extends CLBuildConfiguration {

    /**
     * Builds or rebuilds a program.
     */
    public CLProgram build();

    /**
     * Builds or rebuilds a program.
     * @param listener The callback who will be notified when the program has built.
     */
    public CLProgram build(CLBuildListener listener);

    /**
     * Returns the program.
     */
    public CLProgram getProgram();

    /**
     * Returns a new instance of of this configuration without a {@link CLProgram},
     * program binaries or sources associated with it.
     */
    public CLBuildConfiguration asBuildConfiguration();


    // overwrite with CLProgramConfiguration as return type
    @Override public CLProgramConfiguration forDevice(CLDevice device);
    @Override public CLProgramConfiguration forDevices(CLDevice... devices);
    @Override public CLProgramConfiguration withDefine(String name);
    @Override public CLProgramConfiguration withDefine(String name, Object value);
    @Override public CLProgramConfiguration withDefines(String... names);
    @Override public CLProgramConfiguration withDefines(Map<String, ? extends Object> defines);
    @Override public CLProgramConfiguration withOption(String option);
    @Override public CLProgramConfiguration withOptions(String... options);
    @Override public CLProgramConfiguration reset();
    @Override public CLProgramConfiguration resetOptions();
    @Override public CLProgramConfiguration resetDefines();
    @Override public CLProgramConfiguration resetDevices();
    @Override public CLProgramConfiguration clone();

}
