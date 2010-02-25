package com.mbien.opencl;

import java.util.Map;

/**
 * Configuration representing everything needed to build an OpenCL program (program included).
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
     * Sets the program which should be build.
     */
    public CLProgramConfiguration setProgram(CLProgram program);


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

}
