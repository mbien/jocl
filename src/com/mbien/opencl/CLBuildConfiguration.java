package com.mbien.opencl;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * Configuration representing everything needed to build an OpenCL program.
 * @author Michael Bien
 * @see CLProgramBuilder#createConfiguration()
 * @see CLProgramBuilder#loadConfiguration(java.io.ObjectInputStream)
 */
public interface CLBuildConfiguration extends Cloneable {

    /**
     * Builds or rebuilds the program.
     * @param program The program which should be build.
     */
    public CLProgram build(CLProgram program);

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
     * @see CLProgram#define(java.lang.String)
     */
    public CLBuildConfiguration withDefines(String... names);

    /**
     * Adds the definitions to the build configuration.
     * @see CLProgram#define(java.lang.String, java.lang.Object)
     */
    public CLBuildConfiguration withDefines(Map<String, ? extends Object> defines);

    /**
     * Adds the compiler option to the build configuration.
     * @see CLProgram.CompilerOptions
     */
    public CLBuildConfiguration withOption(String option);

    /**
     * Adds the compiler options to the build configuration.
     * @see CLProgram.CompilerOptions
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
