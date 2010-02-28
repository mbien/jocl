package com.mbien.opencl;

import java.util.Map;

/**
 * Configuration representing everything needed to build an OpenCL program (program included).
 * CLProgramConfiguration is a helper for building programs with more complex configurations or
 * building multiple programs with the similar configuration.
 * @see CLProgram#prepare()
 * @see CLProgramBuilder#createConfiguration(com.mbien.opencl.CLProgram)
 * @see CLProgramBuilder#loadConfiguration(java.io.ObjectInputStream, com.mbien.opencl.CLContext)
 * @author Michael Bien
 */
public interface CLProgramConfiguration extends CLBuildConfiguration {

    /**
     * Builds or rebuilds a program.
     */
    public CLProgram build();

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
