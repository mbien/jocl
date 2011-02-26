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

/*
 * Created on Monday, November 15 2010 19:44
 */
package com.jogamp.opencl.util;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.JogampVersion;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.opencl.CL;
import java.security.PrivilegedAction;
import java.util.jar.Manifest;

import static java.security.AccessController.*;
import static com.jogamp.common.util.VersionUtil.*;

/**
 * Utility for querying module versions and environment properties.
 * @author Michael Bien
 */
public class JOCLVersion extends JogampVersion {

    private static final String PACKAGE = "com.jogamp.opencl";

    private JOCLVersion(Manifest mf) {
        super(PACKAGE, mf);
    }

    private static JOCLVersion createInstance() {
        return doPrivileged(new PrivilegedAction<JOCLVersion>() {
            public JOCLVersion run() {
                Manifest manifest = VersionUtil.getManifest(CL.class.getClassLoader(), PACKAGE);
                if(manifest == null) {
                manifest = new Manifest();
                }
                return new JOCLVersion(manifest);
            }
        });
    }

    public static String getVersion() {
        return createInstance().toString();
    }

    public static String getAllVersions() {

        final StringBuilder sb = new StringBuilder();

        try{
            sb.append(getPlatformInfo(null));
            sb.append(Platform.getNewline());
        }catch(Exception e) {
            sb.append(e.getMessage());
            e.printStackTrace();
        }

        try{
            createInstance().toString(sb);
            sb.append(Platform.getNewline());
        }catch(Exception e) {
            sb.append(e.getMessage());
            e.printStackTrace();
        }

        try{
            doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    sb.append(GlueGenVersion.getInstance().toString());
                    return null;
                }
            });
        }catch(Exception e) {
            sb.append(e.getMessage());
            e.printStackTrace();
        }

        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println(JOCLVersion.getAllVersions());
    }
}
