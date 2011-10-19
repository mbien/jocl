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


import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.IntBuffer;
import javax.imageio.ImageIO;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static java.lang.System.*;
import static com.jogamp.common.nio.Buffers.*;
import static com.jogamp.opencl.CLImageFormat.ChannelOrder.*;
import static com.jogamp.opencl.CLImageFormat.ChannelType.*;

/**
 * Test testing CLImage API.
 * @author Michael Bien
 */
public class CLImageTest {

    private static int[] pixels;

    @BeforeClass
    public static void init() throws IOException {
        BufferedImage bi = ImageIO.read(CLImageTest.class.getResourceAsStream("jogamp.png"));
        pixels = new int[128*128*4];
        bi.getData().getPixels(0, 0, 128, 128, pixels);
    }

    public CLDevice getCompatibleDevice() {

        CLPlatform[] platforms = CLPlatform.listCLPlatforms();
        for (CLPlatform platform : platforms) {
            CLDevice[] devices = platform.listCLDevices();

            for (CLDevice device : devices) {
                if(device.isImageSupportAvailable()) {
                    return device;
                }
            }
        }

        return null;
    }


    @Test
    public void supportedImageFormatsTest() {
        CLDevice device = getCompatibleDevice();
        if(device == null) {
            out.println("WARNING: can not test image api.");
            return;
        }
        CLContext context = CLContext.create(device);

        try{
            CLImageFormat[] formats = context.getSupportedImage2dFormats();
            assertTrue(formats.length > 0);
            out.println("sample image format: "+formats[0]);
//            for (CLImageFormat format : formats) {
//                out.println(format);
//            }
        }finally{
            context.release();
        }

    }
    
    @Test
    public void image2dCopyTest() throws IOException {

        CLDevice device = getCompatibleDevice();
        if(device == null) {
            out.println("WARNING: can not test image api.");
            return;
        }
        CLContext context = CLContext.create(device);

        CLCommandQueue queue = device.createCommandQueue();

        try{

            CLImageFormat format = new CLImageFormat(RGBA, UNSIGNED_INT32);
            
            CLImage2d<IntBuffer> imageA = context.createImage2d(newDirectIntBuffer(pixels), 128, 128, format);
            CLImage2d<IntBuffer> imageB = context.createImage2d(newDirectIntBuffer(pixels.length), 128, 128, format);

            queue.putWriteImage(imageA, false)
                 .putCopyImage(imageA, imageB)
                 .putReadImage(imageB, true);
            
            IntBuffer bufferA = imageA.getBuffer();
            IntBuffer bufferB = imageB.getBuffer();

            while(bufferA.hasRemaining()) {
                assertEquals(bufferA.get(), bufferB.get());
            }

        }finally{
            context.release();
        }

    }
    
    @Test
    public void image2dCopyBufferTest() throws IOException {

        CLDevice device = getCompatibleDevice();
        if(device == null) {
            out.println("WARNING: can not test image api.");
            return;
        }
        CLContext context = CLContext.create(device);

        CLCommandQueue queue = device.createCommandQueue();

        try{

            CLImageFormat format = new CLImageFormat(RGBA, UNSIGNED_INT32);
            
            CLImage2d<IntBuffer> imageA = context.createImage2d(newDirectIntBuffer(pixels), 128, 128, format);
            CLImage2d<IntBuffer> imageB = context.createImage2d(newDirectIntBuffer(pixels.length), 128, 128, format);
            CLBuffer<IntBuffer>  buffer = context.createBuffer(newDirectIntBuffer(pixels.length));

            // image -> buffer
            queue.putWriteImage(imageA, false)
                 .putCopyImageToBuffer(imageA, buffer)
                 .putReadBuffer(buffer, true);
            
            IntBuffer content = buffer.getBuffer();
            while(content.hasRemaining()) {
                assertEquals(pixels[content.position()], content.get());
            }
            content.rewind();
            
            // buffer -> image
            queue.putCopyBufferToImage(buffer, imageB)
                 .putReadImage(imageB, true);
            
            IntBuffer bufferA = imageA.getBuffer();
            IntBuffer bufferB = imageB.getBuffer();

            while(bufferA.hasRemaining()) {
                assertEquals(bufferA.get(), bufferB.get());
            }

        }finally{
            context.release();
        }

    }

    @Test
    public void image2dKernelCopyTest() throws IOException {

        CLDevice device = getCompatibleDevice();
        if(device == null) {
            out.println("WARNING: can not test image api.");
            return;
        }
        CLContext context = CLContext.create(device);

        String src =
        "constant sampler_t imageSampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP | CLK_FILTER_NEAREST; \n" +
        "kernel void image2dCopy(read_only image2d_t input, write_only image2d_t output) { \n" +
        "    int2 coord = (int2)(get_global_id(0), get_global_id(1)); \n" +
        "    uint4 temp = read_imageui(input, imageSampler, coord); \n" +
        "    write_imageui(output, coord, temp); \n" +
        "} \n";

        CLKernel kernel = context.createProgram(src).build().createCLKernel("image2dCopy");

        CLCommandQueue queue = device.createCommandQueue();

        try{

            CLImageFormat format = new CLImageFormat(RGBA, UNSIGNED_INT32);

            CLImage2d<IntBuffer> imageA = context.createImage2d(newDirectIntBuffer(pixels), 128, 128, format);
            CLImage2d<IntBuffer> imageB = context.createImage2d(newDirectIntBuffer(pixels.length), 128, 128, format);

            kernel.putArgs(imageA, imageB);
            queue.putWriteImage(imageA, false)
                 .put2DRangeKernel(kernel, 0, 0, 128, 128, 0, 0)
                 .putReadImage(imageB, true);

            IntBuffer bufferA = imageA.getBuffer();
            IntBuffer bufferB = imageB.getBuffer();

            while(bufferA.hasRemaining()) {
                assertEquals(bufferA.get(), bufferB.get());
            }

        }finally{
            context.release();
        }

    }

}
