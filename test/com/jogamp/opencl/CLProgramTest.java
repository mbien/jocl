/*
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package com.jogamp.opencl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLWork.CLWork1D;
import com.jogamp.opencl.util.CLBuildConfiguration;
import com.jogamp.opencl.util.CLProgramConfiguration;
import com.jogamp.opencl.CLProgram.Status;
import com.jogamp.opencl.util.CLBuildListener;
import com.jogamp.opencl.llb.CL;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;
import static java.lang.System.*;
import static com.jogamp.opencl.CLProgram.CompilerOptions.*;
import static com.jogamp.opencl.util.CLPlatformFilters.*;
import static com.jogamp.opencl.CLVersion.*;

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

        CLKernel kernel = program.createCLKernel("VectorAddGM");
        assertNotNull(kernel);

        // rebuild
        // 1. release kernels (internally)
        // 2. build program
        program.build();
        assertTrue(program.isExecutable());
        out.println(program.getBuildStatus());

        // try again with rebuilt program
        kernel = program.createCLKernel("VectorAddGM");
        assertNotNull(kernel);

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
    public void builderTest() throws IOException, ClassNotFoundException, InterruptedException {
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

        // async build test
        {
            final CountDownLatch countdown = new CountDownLatch(1);
            final CLProgram outerProgram = program;

            CLBuildListener buildCallback = new CLBuildListener() {
                @Override
                public void buildFinished(CLProgram program) {
                    assertEquals(outerProgram, program);
                    countdown.countDown();
                }
            };

            builder.setProgram(program).build(buildCallback);
            countdown.await();
        }

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

        String source = "__attribute__((reqd_work_group_size(1, 1, 1))) kernel void foo(float a, int b, short c) { }\n";

        CLContext context = CLContext.create();

        try{
            CLProgram program = context.createProgram(source).build();
            assertTrue(program.isExecutable());

            CLKernel kernel = program.createCLKernel("foo");
            assertNotNull(kernel);

            long[] wgs = kernel.getCompileWorkGroupSize(context.getDevices()[0]);

            out.println("compile workgroup size: " + wgs[0]+" "+wgs[1]+" "+wgs[2]);

            assertEquals(1, wgs[0]);
            assertEquals(1, wgs[1]);
            assertEquals(1, wgs[2]);

            // put args test
            assertEquals(0, kernel.position());

            kernel.putArg(1.0f);
            assertEquals(1, kernel.position());

            kernel.putArg(2);
            assertEquals(2, kernel.position());

            kernel.putArg((short)3);
            assertEquals(3, kernel.position());
            
            try{
                kernel.putArg(3);
                fail("exception not thrown");
            }catch (IndexOutOfBoundsException expected){ }

            assertEquals(3, kernel.position());
            assertEquals(0, kernel.rewind().position());

        }finally{
            context.release();
        }

    }

    @Test
    public void kernelVectorArgsTest() {

        String source =
                "kernel void vector(global float * out,\n"
              + "                   const float v1,\n"
              + "                   const float2 v2,\n"
              + "//                   const float3 v3,\n" // nv does not support float3
              + "                   const float4 v4,\n"
              + "                   const float8 v8) {\n"
              + "    out[0] = v1;\n"

              + "    out[1] = v2.x;\n"
              + "    out[2] = v2.y;\n"

              + "    out[3] = v4.x;\n"
              + "    out[4] = v4.y;\n"
              + "    out[5] = v4.z;\n"
              + "    out[6] = v4.w;\n"

              + "    out[ 7] = v8.s0;\n"
              + "    out[ 8] = v8.s1;\n"
              + "    out[ 9] = v8.s2;\n"
              + "    out[10] = v8.s3;\n"
              + "    out[11] = v8.s4;\n"
              + "    out[12] = v8.s5;\n"
              + "    out[13] = v8.s6;\n"
              + "    out[14] = v8.s7;\n"
              + "}\n";

        CLContext context = CLContext.create();

        try{
            CLProgram program = context.createProgram(source).build();
            CLKernel kernel = program.createCLKernel("vector");

            CLBuffer<FloatBuffer> buffer = context.createFloatBuffer(15, CLBuffer.Mem.WRITE_ONLY);
            
            final int seed = 7;
            Random rnd = new Random(seed);
            
            kernel.putArg(buffer);
            kernel.putArg(rnd.nextFloat());
            kernel.putArg(rnd.nextFloat(), rnd.nextFloat());
//            kernel.putArg(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()); // nv does not support float3
            kernel.putArg(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat());
            kernel.putArg(TestUtils.fillBuffer(Buffers.newDirectFloatBuffer(8), seed));

            CLCommandQueue queue = context.getMaxFlopsDevice().createCommandQueue();
            queue.putTask(kernel).putReadBuffer(buffer, true);

            FloatBuffer out = buffer.getBuffer();

            rnd = new Random(seed);
            for(int i = 0; i < 7; i++) {
                assertEquals(rnd.nextFloat(), out.get(), 0.01f);
            }

            rnd = new Random(seed);
            for(int i = 0; i < 8; i++) {
                assertEquals(rnd.nextFloat(), out.get(), 0.01f);
            }

        }finally{
            context.release();
        }

    }

    @Test
    public void createAllKernelsTest() {
        
        String source = "kernel void foo(int a) { }\n"+
                        "kernel void bar(float b) { }\n";

        CLContext context = CLContext.create();
        try{
            CLProgram program = context.createProgram(source).build();
            assertTrue(program.isExecutable());

            Map<String, CLKernel> kernels = program.createCLKernels();
            for (CLKernel kernel : kernels.values()) {
                out.println("kernel: "+kernel.toString());
            }

            assertNotNull(kernels.get("foo"));
            assertNotNull(kernels.get("bar"));

            kernels.get("foo").setArg(0, 42);
            kernels.get("bar").setArg(0, 3.14f);


        }finally{
            context.release();
        }

    }

    @Test
    public void workTest() throws IOException {

        CLContext context = CLContext.create(CLPlatform.getDefault(version(CL_1_1)));

        try{
            CLProgram program = context.createProgram(CLProgramTest.class.getResourceAsStream("testkernels.cl")).build();

            CLDevice device = context.getMaxFlopsDevice();
            out.println(device);
            CLCommandQueue queue = device.createCommandQueue();

            CLBuffer<IntBuffer> buffer = context.createIntBuffer(20);

            CLWork1D work = CLWork.create1D(program.createCLKernel("add"));
            work.getKernel().setArgs(buffer, 5, buffer.getNIOCapacity());
            work.setWorkSize(20, 1).optimizeFor(device);

            queue.putWriteBuffer(buffer, false)
                 .putWork(work)
                 .putReadBuffer(buffer, true);

            while(buffer.getBuffer().hasRemaining()) {
                assertEquals(5, buffer.getBuffer().get());
            }

        }finally{
            context.release();
        }

    }

//    @Test
    public void loadTest() throws IOException, ClassNotFoundException, InterruptedException {
        for(int i = 0; i < 100; i++) {
            rebuildProgramTest();
            builderTest();
            programBinariesTest();
        }
    }

}
