package com.mbien.opencl;

import java.io.IOException;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.System.*;
import static com.mbien.opencl.CLProgram.CompilerOptions.*;

/**
 *
 * @author Michael Bien
 */
public class CLProgramTest {

    @Test
    public void rebuildProgramTest() throws IOException {

        out.println(" - - - CLProgramTest; rebuild program test - - - ");

        CLContext context = CLContext.create();
        CLProgram program = context.createProgram(getClass().getResourceAsStream("testkernels.cl"));

        try{
            program.getCLKernels();
            fail("expected exception but got none :(");
        }catch(CLException ex) {
            out.println("got expected exception:  "+ex.getCLErrorString());
            assertEquals(ex.errorcode, CL.CL_INVALID_PROGRAM_EXECUTABLE);
        }

        out.println(program.getBuildStatus());
        program.build();
        out.println(program.getBuildStatus());

        assertTrue(program.isExecutable());

        Map<String, CLKernel> kernels = program.getCLKernels();
        assertNotNull(kernels);
        assertTrue("kernel map is empty", kernels.size() > 0);

        // rebuild
        // 1. release kernels (internally)
        // 2. build program
        program.build();
        assertTrue(program.isExecutable());
        out.println(program.getBuildStatus());

        // try again with rebuilt program
        kernels = program.getCLKernels();
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

        assertNotNull(program.getCLKernels());
        assertEquals(program.getCLKernels().size(), 0);

        assertNull(program.getCLKernel("foo"));

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
            program.getCLKernels();
        }catch(CLException ex) {
            // expected, not build yet
        }

        out.println(program.getBuildStatus());
        program.build();
        out.println(program.getBuildStatus());

        assertNotNull(program.getCLKernel("Test"));

        assertTrue(program.isExecutable());

    }



}
