/*
 * Copyright 2009 - 2010 JogAmp Community. All rights reserved.
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

import com.jogamp.common.AutoCloseable;
import com.jogamp.common.nio.CachedBufferFactory;
import com.jogamp.common.nio.PointerBuffer;
import java.util.Iterator;

/**
 * Fixed size list for storing CLEvents.
 * @author Michael Bien
 */
public final class CLEventList implements CLResource, AutoCloseable, Iterable<CLEvent> {

    private final CLEvent[] events;

    /**
     * stores event ids for fast access.
     */
    final PointerBuffer IDs;
    
    /**
     * Points always to the first element of the id buffer.
     */
    final PointerBuffer IDsView;
    
    int size;
    
    public CLEventList(int capacity) {
        this(null, capacity);
    }

    public CLEventList(CLEvent... events) {
        this(null, events);
    }

    public CLEventList(CachedBufferFactory factory, int capacity) {
        this.events = new CLEvent[capacity];
        this.IDs = initIDBuffer(factory, capacity);
        this.IDsView = IDs.duplicate();
    }

    public CLEventList(CachedBufferFactory factory, CLEvent... events) {
        this.events = events;
        this.IDs = initIDBuffer(factory, events.length);
        this.IDsView = IDs.duplicate();
        
        for (CLEvent event : events) {
            if(event == null) {
                throw new IllegalArgumentException("event list containes null element.");
            }
            IDs.put(event.ID);
        }
        IDs.rewind();
        size = events.length;
    }
    
    private PointerBuffer initIDBuffer(CachedBufferFactory factory, int size) {
        if(factory == null) {
            return PointerBuffer.allocateDirect(size);
        }else{
            return PointerBuffer.wrap(factory.newDirectByteBuffer(size*PointerBuffer.ELEMENT_SIZE));
        }
    }

    void createEvent(CLContext context) {

        if(events[size] != null)
            events[size].release();

        events[size] = new CLEvent(context, IDs.get());
        size++;
    }

    /**
     * Releases all CLEvents in this list.
     */
    public void release() {
        for (int i = 0; i < size; i++) {
            events[i].release();
            events[i] = null;
        }
        size = 0;
        IDs.rewind();
    }

    /**
     * @deprecated use {@link #release()} instead.
     */
    @Deprecated
    public final void close() throws Exception {
        release();
    }
 
    public CLEvent getEvent(int index) {
        if(index >= size)
            throw new IndexOutOfBoundsException("list contains "+size+" events, can not return event with index "+index);
        return events[index];
    }

    /**
     * Returns the current size of this list.
     */
    public int size() {
        return size;
    }

    /**
     * Returns the maximum size of this list.
     */
    public int capacity() {
        return events.length;
    }

    public Iterator<CLEvent> iterator() {
        return new EventIterator(events, size);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append('[');
        for (int i = 0; i < size; i++) {
            sb.append(events[i].toString());
            if(i+1 != size) {
                sb.append(", ");
            }
        }
        return sb.append(']').toString();
    }

    private static class EventIterator implements Iterator<CLEvent> {

        private final CLEvent[] events;
        private final int size;
        private int index;

        private EventIterator(CLEvent[] events, int size) {
            this.events = events;
            this.size = size;
        }

        public boolean hasNext() {
            return index < size;
        }

        public CLEvent next() {
            if(hasNext())
                return events[index++];
            else
                return null;
        }

        public void remove() {
            throw new UnsupportedOperationException("remove() not supported.");
        }

    }

}
