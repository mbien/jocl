package com.mbien.opencl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * CLProgramBuilder is a helper for building programs with more complex configurations or
 * building multiple programs with the same configuration.
 * @see CLProgram#prepare()
 * @see #createConfiguration()
 * @see #createConfiguration(com.mbien.opencl.CLProgram)
 * @see #load(java.io.ObjectInputStream) 
 * @author Michael Bien
 */
public final class CLProgramBuilder implements CLProgramConfiguration, Serializable {

    static final long serialVersionUID = 42;

    private static final byte[] NO_BINARIES = new byte[0];

    private transient CLProgram program;
    private transient Map<CLDevice, byte[]> binariesMap = new LinkedHashMap<CLDevice, byte[]>();

    private String source;

    private final Set<String> optionSet = new LinkedHashSet<String>();
    private final Set<String> defineSet = new LinkedHashSet<String>();


    private CLProgramBuilder() {
        this(null);
    }

    private CLProgramBuilder(CLProgram program) {
        this(program, null, null);
    }

    private CLProgramBuilder(CLProgram program, String source, Map<CLDevice, byte[]> map) {
        this.program = program;
        this.source = source;
        if(map != null) {
            this.binariesMap.putAll(map);
        }
    }

    public static CLBuildConfiguration createConfiguration() {
        return createConfiguration(null);
    }

    public static CLProgramConfiguration createConfiguration(CLProgram program) {
        return new CLProgramBuilder(program);
    }

    public void save(ObjectOutputStream oos) throws IOException {
        if(program != null) {
            this.source = program.getSource();
            if(program.isExecutable()) {
                binariesMap = program.getBinaries();
            }
        }
        oos.writeObject(this);
    }

    public CLProgramConfiguration load(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        return (CLProgramConfiguration) ois.readObject();
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
        binariesMap.put(device, NO_BINARIES);
        return this;
    }

    public CLProgramBuilder forDevices(CLDevice... devices) {
        for (CLDevice device : devices) {
            binariesMap.put(device, NO_BINARIES);
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
        CLDevice[] devices = binariesMap.keySet().toArray(new CLDevice[binariesMap.size()]);
        return program.build(options, devices);
    }

    public CLProgramBuilder reset() {
        optionSet.clear();
        defineSet.clear();
        binariesMap.clear();
        return this;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(binariesMap.size());

        for (CLDevice device : binariesMap.keySet()) {
            byte[] binaries = binariesMap.get(device);
            out.writeLong(device.ID);
            out.writeInt(binaries.length);
            out.write(binaries);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.binariesMap = new LinkedHashMap<CLDevice, byte[]>();
        int mapSize = in.readInt();

        for (int i = 0; i < mapSize; i++) {
            long deviceID = in.readLong();
            int length = in.readInt();
            byte[] binaries = new byte[length];
            in.readFully(binaries);

            CLDevice device = new CLDevice(CLPlatform.getLowLevelCLInterface(), deviceID);
            binariesMap.put(device, binaries);
        }
    }

    @Override
    public CLProgramBuilder clone() {
        CLProgramBuilder builder = new CLProgramBuilder(program, source, binariesMap);
        builder.defineSet.addAll(defineSet);
        builder.optionSet.addAll(optionSet);
        return builder;
    }

    public CLProgram getProgram() {
        return program;
    }

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
        sb.append(", devices=").append(binariesMap);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CLProgramBuilder that = (CLProgramBuilder) o;

        if (source    != null ? !source.equals(that.source)       : that.source    != null) return false;
        if (defineSet != null ? !defineSet.equals(that.defineSet) : that.defineSet != null) return false;
        if (optionSet != null ? !optionSet.equals(that.optionSet) : that.optionSet != null) return false;

        if(binariesMap != null && that.binariesMap != null) {
            if(binariesMap.size() != that.binariesMap.size()) {
                return false;
            }
            Iterator<CLDevice> iterator0 = binariesMap.keySet().iterator();
            Iterator<CLDevice> iterator1 = that.binariesMap.keySet().iterator();
            for (int i = 0; i < binariesMap.size(); i++) {
                CLDevice device0 = iterator0.next();
                CLDevice device1 = iterator1.next();
                if(!device0.equals(device1) || !Arrays.equals(binariesMap.get(device0), that.binariesMap.get(device1)))
                    return false;
            }
        }else if(binariesMap != null || that.binariesMap != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = optionSet != null ? optionSet.hashCode() : 0;
        result = 31 * result + (defineSet != null ? defineSet.hashCode() : 0);
        result = 31 * result + (binariesMap != null ? binariesMap.hashCode() : 0);
        return result;
    }

}
