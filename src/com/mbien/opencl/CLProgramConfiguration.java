package com.mbien.opencl;

import java.util.Map;

/**
 * Configuration representing everything needed to build an OpenCL program (program included).
 * CLProgramConfiguration is a helper for building programs with more complex configurations or
 * building multiple programs with the similar configuration.
 * @see CLProgram#prepare()
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


    // overwrite with CLProgramConfiguration as return type
    @Override public CLProgramConfiguration forDevice(CLDevice device);
    @Override public CLProgramConfiguration forDevices(CLDevice... devices);
    @Override public CLProgramConfiguration withDefine(String name);
    @Override public CLProgramConfiguration withDefine(String name, Object value);
    @Override public CLProgramConfiguration withDefines(String... names);
    @Override public CLProgramConfiguration withDefines(Map<String, String> defines);
    @Override public CLProgramConfiguration withOption(String option);
    @Override public CLProgramConfiguration withOptions(String... options);
    @Override public CLProgramConfiguration reset();
    @Override public CLProgramConfiguration clone();

}
