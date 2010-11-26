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
 * Created on Thursday, June 24 2010 05:38
 */
package com.jogamp.opencl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version of an OpenCL Implementation.
 * All comparison operations use the {@link #getSpecVersion()} for comparison.
 * @author Michael Bien
 */
public class CLVersion implements Comparable<CLVersion> {

    private final static Pattern pattern = Pattern.compile("OpenCL (?:C )?(\\d+)\\.(\\d+)(.*)");

    public final static CLVersion CL_1_0 = new CLVersion("OpenCL 1.0");
    public final static CLVersion CL_1_1 = new CLVersion("OpenCL 1.1");

    /**
     * The full version String is defined as:
     * <code>OpenCL[space][major_version].[minor_version][space][platform-specific information]</code>
     */
    public final String fullversion;
    /**
     * The platform specific part of the version string.
     * @see #fullversion
     */
    public final String implversion;
    /**
     * Minor version number.
     * @see #fullversion
     */
    public final short minor;
    /**
     * Mayor version number.
     * @see #fullversion
     */
    public final short major;

    protected CLVersion(String version) {
        this.fullversion = version;
        Matcher matcher = pattern.matcher(version);
        matcher.matches();
        major = Short.parseShort(matcher.group(1));
        minor = Short.parseShort(matcher.group(2));

        if(matcher.groupCount() == 4) {//first group == whole string
            implversion = matcher.group(3).substring(1);
        }else{
            implversion = "";
        }
    }

    public int compareTo(CLVersion other) {
        return compareTo(other.major, other.minor);
    }
    
    private int compareTo(int otherMajor, int otherMinor)  {
        if(otherMajor == major && otherMinor == minor) {
            return 0;
        }else if(this.major > otherMajor || (this.major == otherMajor && this.minor > otherMinor)) {
            return 1;
        }else{
            return -1;
        }
    }

    public boolean isAtLeast(CLVersion other) {
        return this.compareTo(other) >= 0;
    }

    public boolean isAtLeast(int major, int minor) {
        return this.compareTo(major, minor) >= 0;
    }

    public boolean isEqual(CLVersion other) {
        return this.isEqual(other.major, other.minor);
    }
    
    public boolean isEqual(int major, int minor) {
        return this.major == major && this.minor == minor;
    }

    /**
     * Returns <code>'"OpenCL " + major + "." + minor'</code>.
     */
    public String getSpecVersion() {
        return "OpenCL " + major + '.' + minor;
    }

    /**
     * Returns the full, unfiltered version string.
     * @see #fullversion
     */
    public String getFullVersion() {
        return fullversion;
    }

    /**
     * @see #implversion
     */
    public String getImplVersion() {
        return implversion;
    }

    /**
     * @see #major
     */
    public short getMajor() {
        return major;
    }

    /**
     * @see #minor
     */
    public short getMinor() {
        return minor;
    }

    @Override
    public String toString() {
        return getFullVersion();
    }

    @Override
    public int hashCode() {
        return fullversion.hashCode();
    }

    /**
     * Returns true if both {@link #fullversion} Strings match.
     */
    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == getClass() && fullversion.equals(((CLVersion)obj).fullversion);
    }


}
