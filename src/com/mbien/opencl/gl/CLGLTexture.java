/*
 * Created on Friday, February 26 2010
 */

package com.mbien.opencl.gl;

/**
 *
 * @author Michael Bien
 */
interface CLGLTexture {

    /**
     * Returns the OpenGL texture target of this texture.
     */
    public int getTextureTarget();

    /**
     * Returns the OpenGL mipmap level of this texture.
     */
    public int getMipMapLevel();

}
