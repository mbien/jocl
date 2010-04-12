package com.jogamp.ant;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Keeps downloaded headers up2date.
 * @author Michael Bien
 */
public class HeaderFileDownloader extends Task {

    private String filePath;
    private String urlStr;

    /*example: $Revision: 9283 $ on $Date: 2009-10-14 10:18:57 -0700 (Wed, 14 Oct 2009) $ */
    private final Pattern revisionPattern = Pattern.compile("\\$Revision:\\s(\\d+)");

    @Override
    public void execute() throws BuildException {

        if(filePath == null)
            throw new IllegalArgumentException("file must be set");
        if(urlStr == null)
            throw new IllegalArgumentException("update url must be set");

        try {
            URL url = new URL(urlStr);
            int remoteRevision = readRevision(url.openStream());
            int localRevision = readRevision(new FileInputStream(new File(filePath)));

            if(remoteRevision != localRevision) {

                System.out.println("updating header: "+filePath);
                System.out.println("from revision "+localRevision +" to revision "+remoteRevision);

                BufferedInputStream in = new BufferedInputStream(url.openStream());
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filePath));

                int val;
                while((val=in.read()) != -1) {
                    out.write(val);
                }
                in.close();

                out.flush();
                out.close();
            }else{
                System.out.println("header "+filePath+" is up to date");
            }

        } catch (IOException ex) {
            throw new BuildException(ex);
        }finally{
        }

    }

    private int readRevision(InputStream is) throws IOException  {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                Matcher matcher = revisionPattern.matcher(line);
                if(matcher.find()) {
                    System.out.println(line);
                    return Integer.parseInt(matcher.group(1));
                }
            }
        } finally {
            reader.close();
        }

        return 0;
    }


    public void setURL(String url) {
        this.urlStr = url;
    }

    public void setHeader(String filePath) {
        this.filePath = filePath;
    }


}
