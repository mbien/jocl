/*
 * Created on Thursday, April 28 2011 22:10
 */
package com.jogamp.opencl.util;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.CLResource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.*;
import static com.jogamp.opencl.CLDevice.Type.*;

/**
 * Utility for organizing multiple {@link CLContext}s.
 *
 * @author Michael Bien
 */
public class CLMultiContext implements CLResource {

    private final List<CLContext> contexts;

    private CLMultiContext() {
        contexts = new ArrayList<CLContext>();
    }

    /**
     * Creates a multi context with all devices of the specified platforms.
     */
    public static CLMultiContext create(CLPlatform... platforms) {
        return create(platforms, ALL);
    }

    /**
     * Creates a multi context with all devices of the specified platforms and types.
     */
    public static CLMultiContext create(CLPlatform[] platforms, CLDevice.Type... types) {
        
        if(platforms == null) {
            throw new NullPointerException("platform list was null");
        }else if(platforms.length == 0) {
            throw new IllegalArgumentException("platform list was empty");
        }

        List<CLDevice> devices = new ArrayList<CLDevice>();
        for (CLPlatform platform : platforms) {
            devices.addAll(asList(platform.listCLDevices(types)));
        }
        return create(devices);
    }

    /**
     * Creates a multi context with the specified devices.
     * The devices don't have to be from the same platform.
     */
    public static CLMultiContext create(Collection<CLDevice> devices) {

        if(devices.isEmpty()) {
            throw new IllegalArgumentException("device list was empty");
        }

        Map<CLPlatform, List<CLDevice>> platformDevicesMap = filterPlatformConflicts(devices);

        // create contexts
        CLMultiContext mc = new CLMultiContext();
        for (Map.Entry<CLPlatform, List<CLDevice>> entry : platformDevicesMap.entrySet()) {
            List<CLDevice> list = entry.getValue();
            // one context per device to workaround driver bugs
            for (CLDevice device : list) {
                CLContext context = CLContext.create(device);
                mc.contexts.add(context);
            }
        }

        return mc;
    }

    /**
     * Creates a multi context with specified contexts.
     */
    public static CLMultiContext wrap(CLContext... contexts) {
        CLMultiContext mc = new CLMultiContext();
        mc.contexts.addAll(asList(contexts));
        return mc;
    }

    /**
     * filter devices; don't allow the same device to be used in more than one platform.
     * example: a CPU available via the AMD and Intel SDKs shouldn't end up in two contexts
     */
    private static Map<CLPlatform, List<CLDevice>> filterPlatformConflicts(Collection<CLDevice> devices) {

        // FIXME: devicename-platform is used as unique device identifier - replace if we have something better
        
        Map<CLPlatform, List<CLDevice>> filtered = new HashMap<CLPlatform, List<CLDevice>>();
        Map<String, CLPlatform> used = new HashMap<String, CLPlatform>();

        for (CLDevice device : devices) {

            String name = device.getName(); 

            CLPlatform platform = device.getPlatform();
            CLPlatform usedPlatform = used.get(name);

            if(usedPlatform == null || platform.equals(usedPlatform)) {
                if(!filtered.containsKey(platform)) {
                    filtered.put(platform, new ArrayList<CLDevice>());
                }
                filtered.get(platform).add(device);
                used.put(name, platform);
            }
            
        }
        return filtered;
    }


    /**
     * Releases all contexts.
     * @see CLContext#release()
     */
    public void release() {
        for (CLContext context : contexts) {
            context.release();
        }
        contexts.clear();
    }

    public List<CLContext> getContexts() {
        return Collections.unmodifiableList(contexts);
    }

    /**
     * Returns a list containing all devices used in this multi context.
     */
    public List<CLDevice> getDevices() {
        List<CLDevice> devices = new ArrayList<CLDevice>();
        for (CLContext context : contexts) {
            devices.addAll(asList(context.getDevices()));
        }
        return devices;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+" [" + contexts.size()+" contexts, "
                                               + getDevices().size()+ " devices]";
    }



}
