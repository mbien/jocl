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
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * Configuration representing everything needed to build an OpenCL program.
 * @author Michael Bien
 * @see com.jogamp.opencl.CLProgramBuilder#createConfiguration()
 * @see com.jogamp.opencl.CLProgramBuilder#loadConfiguration(java.io.ObjectInputStream)
 */
public interface CLBuildConfiguration extends Cloneable {

    /**
     * Builds or rebuilds the program.
     * @param program The program which should be build.
     */
    public CLProgram build(CLProgram program);

    /**
     * Builds or rebuilds the program.
     * @param program The program which should be build.
     * @param listener The callback who is notified when the program has built.
     */
    public CLProgram build(CLProgram program, CLBuildListener listener);

    /**
     * Sets the program which should be build.
     */
    public CLProgramConfiguration setProgram(CLProgram program);

    /**
     * Adds the device as build target.
     */
    public CLBuildConfiguration forDevice(CLDevice device);

    /**
     * Adds the devices as build target.
     */
    public CLBuildConfiguration forDevices(CLDevice... devices);

    /**
     * Resets this builder's configuration like options, devices and definitions.
     */
    public CLBuildConfiguration reset();

    /**
     * Resets this builder's configuration options.
     */
    public CLBuildConfiguration resetOptions();

    /**
     * Resets this builder's macro definitions.
     */
    public CLBuildConfiguration resetDefines();

    /**
     * Resets this builder's device list.
     */
    public CLBuildConfiguration resetDevices();

    /**
     * Adds the definition to the build configuration.
     * @see CLProgram#define(java.lang.String)
     */
    public CLBuildConfiguration withDefine(String name);

    /**
     * Adds the definition to the build configuration.
     * @see CLProgram#define(java.lang.String, java.lang.Object)
     */
    public CLBuildConfiguration withDefine(String name, Object value);

    /**
     * Adds the definitions to the build configuration.
     * @see com.jogamp.opencl.CLProgram#define(java.lang.String)
     */
    public CLBuildConfiguration withDefines(String... names);

    /**
     * Adds the definitions to the build configuration.
     * @see com.jogamp.opencl.CLProgram#define(java.lang.String, java.lang.Object)
     */
    public CLBuildConfiguration withDefines(Map<String, ? extends Object> defines);

    /**
     * Adds the compiler option to the build configuration.
     * @see com.jogamp.opencl.CLProgram.CompilerOptions
     */
    public CLBuildConfiguration withOption(String option);

    /**
     * Adds the compiler options to the build configuration.
     * @see com.jogamp.opencl.CLProgram.CompilerOptions
     */
    public CLBuildConfiguration withOptions(String... options);

    /**
     * Clones this configuration.
     */
    public CLBuildConfiguration clone();

    /**
     * Saves this configuration to the ObjectOutputStream.
     * The caller is responsible for closing the stream.
     */
    public void save(ObjectOutputStream oos) throws IOException;

}
