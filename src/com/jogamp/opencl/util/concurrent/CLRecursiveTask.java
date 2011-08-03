/*
 * Copyright (c) 2011, Michael Bien
 * All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

/*
 * Created on Tuesday, August 02 2011 02:02
 */
package com.jogamp.opencl.util.concurrent;

import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.util.concurrent.CLExecutorService.CommandQueueThread;
import java.util.concurrent.RecursiveTask;

/**
 * A recursive decomposable task executed on a {@link CLCommandQueue}.
 * The two main operations are {@link #fork()} for decomposing and {@link #join()} to wait for a forked task.
 * @see RecursiveTask
 * @author Michael Bien
 */
public abstract class CLRecursiveTask<C extends CLQueueContext, R> extends RecursiveTask<R> implements CLPoolable<C, R> {

    @Override
    protected final R compute() {
        
        CommandQueueThread thread = (CommandQueueThread)Thread.currentThread();

        final Object key = getContextKey();

        CLQueueContext context = thread.getContextMap().get(key);
        if(context == null) {
            context = createQueueContext(thread.getQueue());
            thread.getContextMap().put(key, context);
        }

        @SuppressWarnings("unchecked")
        CLPoolable<CLQueueContext, R> task = (CLPoolable<CLQueueContext, R>) this;

        R result = task.execute(context);

        // TODO: currently only the root task supports finish actions
//        if(mode.equals(FinishAction.FLUSH)) {
//            context.queue.flush();
//        }else if(mode.equals(FinishAction.FINISH)) {
//            context.queue.finish();
//        }
        return result;
    }
    
    /**
     * Returns the context key for this task. Default implementation returns {@link #getClass()}.
     */
    @Override
    public Object getContextKey() {
        return getClass();
    }

}
