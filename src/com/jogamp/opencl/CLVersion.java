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
