package com.jogamp.opencl;

// early import of JDK7's ARM interface for JDK6 backwards compatibility.
public interface AutoCloseable {
    void close() throws Exception;
}
