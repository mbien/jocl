/*
 * Created on Friday, February 26 2010
 */

package com.jogamp.opencl.gl;

/**
 *
 * @author Michael Bien
 */
interface CLGLTexture extends CLGLObject {

    /**
     * Returns the OpenGL texture target of this texture.
     */
    public int getTextureTarget();

    /**
     * Returns the OpenGL mipmap level of this texture.
     */
    public int getMipMapLevel();

}
