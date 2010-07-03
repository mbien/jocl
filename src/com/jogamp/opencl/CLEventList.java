package com.jogamp.opencl;

import com.jogamp.common.nio.PointerBuffer;
import java.util.Iterator;

/**
 * Fixed size list for storing CLEvents.
 * @author Michael Bien
 */
public final class CLEventList implements CLResource, AutoCloseable, Iterable<CLEvent> {

    private final CLEvent[] events;

    final PointerBuffer IDs;
    int size;

    public CLEventList(int capacity) {
        this.events = new CLEvent[capacity];
        this.IDs = PointerBuffer.allocateDirect(capacity);
    }

    public CLEventList(CLEvent... events) {
        this.events = events;
        this.IDs = PointerBuffer.allocateDirect(events.length);
        for (CLEvent event : events) {
            IDs.put(event.ID);
        }
        IDs.rewind();
        size = events.length;
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
