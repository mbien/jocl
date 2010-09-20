/*
 * Created on Sunday, September 19 2010
 */

package com.jogamp.opencl.util;

/**
 *
 * @author Michael Bien
 */
public interface Filter<I> {

    /**
     * Returns true only if the item should be accepted.
     */
    public boolean accept(I item);

}
