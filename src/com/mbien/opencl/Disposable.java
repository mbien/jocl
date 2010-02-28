package com.mbien.opencl;

/*
 * JDK7 ARM proposal, cypied to be forward compatible with java 7 automatic resource managment blocks.
 * @author Michael Bien
 */

//package java.lang;

/**
 * A resource that must be closed when it is no longer needed.
 *
 * @param X the type of exception thrown by the close method (or
 *     {@link RuntimeException} if the close method is not permitted
 *     to throw any checked exceptions).
 */
/*public*/ interface Disposable<X extends Throwable> {

    void close() throws X;

}
