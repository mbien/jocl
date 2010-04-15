package com.jogamp.opencl;

import com.jogamp.opencl.util.CLBuildConfiguration;
import com.jogamp.opencl.util.CLProgramConfiguration;
import com.jogamp.opencl.CLProgram.Status;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;
import static java.lang.System.*;
import static com.jogamp.opencl.CLProgram.CompilerOptions.*;

/**
 *
 * @author Michael Bien
 */
public class CLProgramTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();


    @Test
    public void enumsTest() {

        // CLProgram enums
        for (Status e : Status.values()) {
            assertEquals(e, Status.valueOf(e.STATUS));
        }
    }

    @Test
    public void rebuildProgramTest() throws IOException {

        out.println(" - - - CLProgramTest; rebuild program test - - - ");

        CLContext context = CLContext.create();
        CLProgram program = context.createProgram(getClass().getResourceAsStream("testkernels.cl"));

        try{
            program.createCLKernels();
            fail("expected exception but got none :(");
        }catch(CLException ex) {
            out.println("got expected exception:  "+ex.getCLErrorString());
            assertEquals(ex.errorcode, CL.CL_INVALID_PROGRAM_EXECUTABLE);
        }

        out.println(program.getBuildStatus());
        program.build();
        out.println(program.getBuildStatus());

        assertTrue(program.isExecutable());

        Map<String, CLKernel> kernels = program.createCLKernels();
        assertNotNull(kernels);
        assertTrue("kernel map is empty", kernels.size() > 0);

        // rebuild
        // 1. release kernels (internally)
        // 2. build program
        program.build();
        assertTrue(program.isExecutable());
        out.println(program.getBuildStatus());

        // try again with rebuilt program
        kernels = program.createCLKernels();
        assertNotNull(kernels);
        assertTrue("kernel map is empty", kernels.size() > 0);
        assertTrue(kernels.size() > 0);

        context.release();
    }

    @Test
    public void programBinariesTest() throws IOException {

        out.println(" - - - CLProgramTest; down-/upload binaries test - - - ");

        CLContext context = CLContext.create();
        CLProgram program = context.createProgram(getClass().getResourceAsStream("testkernels.cl"))
                                   .build(ENABLE_MAD, WARNINGS_ARE_ERRORS);

        // optain binaries
        Map<CLDevice, byte[]> binaries = program.getBinaries();
        assertFalse(binaries.isEmpty());

        CLDevice[] devices = program.getCLDevices();
        for (CLDevice device : devices) {
            assertTrue(binaries.containsKey(device));
        }

        // 1. release program
        // 2. re-create program with old binaries
        program.release();

        assertFalse(program.isExecutable());

        assertNotNull(program.getBinaries());
        assertEquals(program.getBinaries().size(), 0);

        assertNotNull(program.getBuildLog());
        assertEquals(program.getBuildLog().length(), 0);

        assertNotNull(program.getSource());
        assertEquals(program.getSource().length(), 0);

        assertNotNull(program.getCLDevices());
        assertEquals(program.getCLDevices().length, 0);

        {
            Map<String, CLKernel> kernels = program.createCLKernels();
            assertNotNull(kernels);
            assertEquals(kernels.size(), 0);
        }
        assertNull(program.createCLKernel("foo"));

        program = context.createProgram(binaries);

        assertFalse(program.isExecutable());

        assertNotNull(program.getCLDevices());
        assertTrue(program.getCLDevices().length != 0);

        assertNotNull(program.getBinaries());
        assertEquals(program.getBinaries().size(), 0);

        assertNotNull(program.getBuildLog());
        assertTrue(program.getBuildLog().length() != 0);

        assertNotNull(program.getSource());
        assertEquals(program.getSource().length(), 0);

        try{
            Map<String, CLKernel> kernels = program.createCLKernels();
            fail("expected an exception from createCLKernels but got: "+kernels);
        }catch(CLException ex) {
            // expected, not build yet
        }

        out.println(program.getBuildStatus());
        program.build();
        out.println(program.getBuildStatus());

        assertNotNull(program.createCLKernel("Test"));

        assertTrue(program.isExecutable());

        context.release();

    }

    @Test
    public void builderTest() throws IOException, ClassNotFoundException {
        out.println(" - - - CLProgramTest; program builder test - - - ");

        CLContext context = CLContext.create();
        CLProgram program = context.createProgram(getClass().getResourceAsStream("testkernels.cl"));

        // same as program.build()
        program.prepare().build();

        assertTrue(program.isExecutable());


        // complex build
        program.prepare().withOption(ENABLE_MAD)
                         .forDevice(context.getMaxFlopsDevice())
                         .withDefine("RADIUS", 5)
                         .withDefine("ENABLE_FOOBAR")
                         .build();

        assertTrue(program.isExecutable());

        // reusable builder
        CLBuildConfiguration builder = CLProgramBuilder.createConfiguration()
                                     .withOption(ENABLE_MAD)
                                     .forDevices(context.getDevices())
                                     .withDefine("RADIUS", 5)
                                     .withDefine("ENABLE_FOOBAR");

        out.println(builder);
        
        builder.setProgram(program).build();
        assertTrue(program.isExecutable());

        // serialization test
        File file = tmpFolder.newFile("foobar.builder");
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        builder.save(oos);
        oos.close();

        // build configuration
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        CLBuildConfiguration buildConfig = CLProgramBuilder.loadConfiguration(ois);
        ois.close();

        assertEquals(builder, buildConfig);

        buildConfig.build(program);
        assertTrue(program.isExecutable());

        // program configuration
        ois = new ObjectInputStream(new FileInputStream(file));
        CLProgramConfiguration programConfig = CLProgramBuilder.loadConfiguration(ois, context);
        assertNotNull(programConfig.getProgram());
        ois.close();
        program = programConfig.build();
        assertTrue(program.isExecutable());

        
        // cloneing
        assertEquals(builder, builder.clone());

        context.release();
    }


    @Test
    public void kernelTest() {

        String source = "__attribute__((reqd_work_group_size(512, 512, 512))) kernel void foo(void) { }\n";

        CLContext context = CLContext.create();

        try{
            CLProgram program = context.createProgram(source).build();
            assertTrue(program.isExecutable());
            
            CLKernel kernel = program.createCLKernel("foo");
            assertNotNull(kernel);

            long[] wgs = kernel.getCompileWorkGroupSize(context.getDevices()[0]);

            out.println("compile workgroup size: " + wgs[0]+" "+wgs[1]+" "+wgs[2]);

            assertEquals(512, wgs[0]);
            assertEquals(512, wgs[1]);
            assertEquals(512, wgs[2]);


        }finally{
            context.release();
        }

    }


}
