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
public final class CLProgramBuilder {

    private transient CLProgram program;
    private final Set<String> optionList = new HashSet<String>();
    private final Set<String> defineList = new HashSet<String>();
    private final Set<CLDevice> deviceList = new HashSet<CLDevice>();

    public CLProgramBuilder() {  }

    public CLProgramBuilder(CLProgram program) {
        this.program = program;
    }

    public CLProgramBuilder withOption(String option) {
        this.optionList.add(option);
        return this;
    }

    public CLProgramBuilder withOptions(String... options) {
        for (String option : options) {
            this.optionList.add(option);
        }
        return this;
    }

    public CLProgramBuilder withDefine(String name) {
        this.defineList.add(CLProgram.define(name));
        return this;
    }

    public CLProgramBuilder withDefines(String... names) {
        for (String name : names) {
            this.defineList.add(CLProgram.define(name));
        }
        return this;
    }

    public CLProgramBuilder withDefine(String name, Object value) {
        this.defineList.add(CLProgram.define(name, value.toString()));
        return this;
    }

    public CLProgramBuilder withDefines(Map<String, String> defines) {
        for (String name : defines.keySet()) {
            defineList.add(CLProgram.define(name, defines.get(name)));
        }
        return this;
    }

    public CLProgramBuilder forDevice(CLDevice device) {
        CLDevice[] devices = new CLDevice[]{device};
        for (CLDevice device1 : devices) {
            this.deviceList.add(device1);
        }
        return this;
    }

    public CLProgramBuilder forDevices(CLDevice... devices) {
        for (CLDevice device : devices) {
            this.deviceList.add(device);
        }
        return this;
    }

    /**
     * Builds or rebuilds a program.
     */
    public CLProgram build() {
        return build(program);
    }

    /**
     * Builds or rebuilds a program.
     */
    public CLProgram build(CLProgram program) {
        if(program == null) {
            throw new NullPointerException("no program has been set");
        }
        List<String> setup = new ArrayList<String>();
        setup.addAll(optionList);
        setup.addAll(defineList);
        String options = CLProgram.optionsOf(setup.toArray(new String[setup.size()]));
        CLDevice[] devices = deviceList.toArray(new CLDevice[deviceList.size()]);
        return program.build(options, devices);
    }

    /**
     * Resets this builder's configuration like options, devices and defines.
     */
    public CLProgramBuilder reset() {
        optionList.clear();
        defineList.clear();
        deviceList.clear();
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
        sb.append("{optionList=").append(optionList);
        sb.append(", defineList=").append(defineList);
        sb.append(", deviceList=").append(deviceList);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CLProgramBuilder that = (CLProgramBuilder) o;

        if (defineList != null ? !defineList.equals(that.defineList) : that.defineList != null) return false;
        if (deviceList != null ? !deviceList.equals(that.deviceList) : that.deviceList != null) return false;
        if (optionList != null ? !optionList.equals(that.optionList) : that.optionList != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = optionList != null ? optionList.hashCode() : 0;
        result = 31 * result + (defineList != null ? defineList.hashCode() : 0);
        result = 31 * result + (deviceList != null ? deviceList.hashCode() : 0);
        return result;
    }
}
