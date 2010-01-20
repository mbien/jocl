package com.mbien.opencl;

import java.io.IOException;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.*;
import static java.lang.System.*;
import static com.mbien.opencl.TestUtils.*;
import static com.sun.gluegen.runtime.BufferFactory.*;

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

        program.build();
        assertTrue(program.isExecutable());
        out.println(program.getBuildStatus());

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
        CLProgram program = context.createProgram(getClass().getResourceAsStream("testkernels.cl")).build();

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

        program = context.createProgram(binaries);

        out.println(program.getBuildStatus());

        program.build();

        assertTrue(program.isExecutable());

    }



}
