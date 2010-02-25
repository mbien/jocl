package com.mbien.opencl;

import java.util.Map;

/**
 * Configuration representing everything needed to build an OpenCL program.
 * @author Michael Bien
 */
public interface CLBuildConfiguration {

    /**
     * Builds or rebuilds the program.
     *  @param program The program which should be build.
     */
    public CLProgram build(CLProgram program);

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
     * Adds the definition to the build configuration.
     * @see public CLProgram#define(java.lang.String)
     */
    public CLBuildConfiguration withDefine(String name);

    /**
     * Adds the definition to the build configuration.
     * @see public CLProgram#define(java.lang.String, java.lang.String)
     */
    public CLBuildConfiguration withDefine(String name, Object value);

    /**
     * Adds the definitions to the build configuration.
     * @see public CLProgram#define(java.lang.String)
     */
    public CLBuildConfiguration withDefines(String... names);

    /**
     * Adds the definitions to the build configuration.
     * @see public CLProgram#define(java.lang.String, java.lang.String)
     */
    public CLBuildConfiguration withDefines(Map<String, String> defines);

    /**
     * Adds the compiler option to the build configuration.
     * @see CompilerOptions
     */
    public CLBuildConfiguration withOption(String option);

    /**
     * Adds the compiler options to the build configuration.
     * @see CompilerOptions
     */
    public CLBuildConfiguration withOptions(String... options);

}
