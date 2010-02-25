package com.mbien.opencl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * CLProgramBuilder is a helper for building programs with more complex configurations or
 * building multiple programs with the same configuration.
 * @see CLProgram#prepare()
 * @author Michael Bien
 */
public final class CLProgramBuilder implements CLProgramConfiguration {

    private transient CLProgram program;
    private String source;
    private Map<CLDevice, byte[]> binaries;

    private final Set<String> optionSet = new HashSet<String>();
    private final Set<String> defineSet = new HashSet<String>();
    private final Set<CLDevice> deviceSet = new HashSet<CLDevice>();


    private CLProgramBuilder() {  }

    private CLProgramBuilder(CLProgram program) {
        this.program = program;
    }

    public static CLProgramConfiguration createForProgram(CLProgram program) {
        return new CLProgramBuilder(program);
    }

    public static CLBuildConfiguration createConfiguration() {
        return new CLProgramBuilder();
    }

    public CLProgramBuilder withOption(String option) {
        optionSet.add(option);
        return this;
    }

    public CLProgramBuilder withOptions(String... options) {
        for (String option : options) {
            optionSet.add(option);
        }
        return this;
    }

    public CLProgramBuilder withDefine(String name) {
        defineSet.add(CLProgram.define(name));
        return this;
    }

    public CLProgramBuilder withDefines(String... names) {
        for (String name : names) {
            defineSet.add(CLProgram.define(name));
        }
        return this;
    }

    public CLProgramBuilder withDefine(String name, Object value) {
        defineSet.add(CLProgram.define(name, value.toString()));
        return this;
    }

    public CLProgramBuilder withDefines(Map<String, String> defines) {
        for (String name : defines.keySet()) {
            defineSet.add(CLProgram.define(name, defines.get(name)));
        }
        return this;
    }

    public CLProgramBuilder forDevice(CLDevice device) {
        deviceSet.add(device);
        return this;
    }

    public CLProgramBuilder forDevices(CLDevice... devices) {
        for (CLDevice device : devices) {
            deviceSet.add(device);
        }
        return this;
    }

    public CLProgram build() {
        return build(program);
    }

    public CLProgram build(CLProgram program) {
        if(program == null) {
            throw new NullPointerException("no program has been set");
        }
        List<String> setup = new ArrayList<String>();
        setup.addAll(optionSet);
        setup.addAll(defineSet);
        String options = CLProgram.optionsOf(setup.toArray(new String[setup.size()]));
        CLDevice[] devices = deviceSet.toArray(new CLDevice[deviceSet.size()]);
        return program.build(options, devices);
    }

    public CLProgramBuilder reset() {
        optionSet.clear();
        defineSet.clear();
        deviceSet.clear();
        return this;
    }

    public CLProgram getProgram() {
        return program;
    }

    /**
     * Sets the program which should be build.
     */
    public CLProgramBuilder setProgram(CLProgram program) {
        this.program = program;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CLProgramBuilder");
        sb.append("{options=").append(optionSet);
        sb.append(", defines=").append(defineSet);
        sb.append(", devices=").append(deviceSet);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CLProgramBuilder that = (CLProgramBuilder) o;

        if (defineSet != null ? !defineSet.equals(that.defineSet) : that.defineSet != null) return false;
        if (deviceSet != null ? !deviceSet.equals(that.deviceSet) : that.deviceSet != null) return false;
        if (optionSet != null ? !optionSet.equals(that.optionSet) : that.optionSet != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = optionSet != null ? optionSet.hashCode() : 0;
        result = 31 * result + (defineSet != null ? defineSet.hashCode() : 0);
        result = 31 * result + (deviceSet != null ? deviceSet.hashCode() : 0);
        return result;
    }

}
