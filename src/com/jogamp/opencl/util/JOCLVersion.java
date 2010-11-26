/*
 * Created on Monday, November 15 2010 19:44
 */
package com.jogamp.opencl.util;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.JogampVersion;
import com.jogamp.common.util.VersionUtil;
import java.util.jar.Manifest;

import static com.jogamp.common.util.VersionUtil.*;

/**
 *
 * @author Michael Bien
 */
public class JOCLVersion extends JogampVersion {

    private static final String PACKAGE = "com.jogamp.opencl";

    private JOCLVersion(Manifest mf) {
        super(PACKAGE, mf);
    }

    private static JOCLVersion createInstance(){
        Manifest manifest = VersionUtil.getManifest(JOCLVersion.class.getClassLoader(), PACKAGE);
        return new JOCLVersion(manifest);
    }

    public static String getVersion() {
        return createInstance().toString();
    }

    public static String getAllVersions() {

        StringBuilder sb = new StringBuilder();

        sb.append(SEPERATOR).append(Platform.getNewline());
        sb.append(getPlatformInfo(null));
        sb.append(SEPERATOR).append(Platform.getNewline());

        createInstance().toString(sb);

        sb.append(GlueGenVersion.getInstance().toString());

        return sb.toString();
    }


    public StringBuilder toString(StringBuilder sb) {
        return sb.append(toString((StringBuffer)null));
    }
    
    @Override
    public StringBuffer toString(StringBuffer sb) {
        if(sb == null) {
            sb = new StringBuffer();
        }
        return super.toString(sb);
    }

    public static void main(String[] args) {
        System.out.println(JOCLVersion.getAllVersions());
    }
}
