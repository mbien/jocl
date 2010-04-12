package com.jogamp.ant;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import static java.util.regex.Pattern.*;

/**
 * Build setup utility. Uncomments funcion parameter names in header files.
 *
 * before:
 * foo(int /x bar x/ )
 *
 * after:
 * foo(int   bar  )
 *
 * @author Michael Bien
 */
public class FunctionParamUncommenter extends Task {

    final static Pattern PARAMS_PATTERN
            = compile("cl\\w+ \\(   (  \\s* [^;]+  )  \\)", MULTILINE|COMMENTS);

    final static Pattern COMMENT_PATTERN
            = compile("\\s*(const)?\\w+\\s* \\**\\s+ (/\\*) \\s+[^\\*\\[]+ (\\*/)", MULTILINE|COMMENTS);
                                                                     //^ array size in param name causes some problems
    private String src;
    private String dest;

    @Override
    public void execute() throws BuildException {
        try {
            uncomment(src, dest, true);
        } catch (FileNotFoundException ex) {
            throw new BuildException(ex);
        } catch (IOException ex) {
            throw new BuildException(ex);
        }
    }

    private final void uncomment(String srcFile, String destFile, boolean replace) throws FileNotFoundException, IOException {

        System.out.println("uncommenting params in "+srcFile);

        StringBuilder headerSrc = readSourceFile(new File(srcFile));
        Matcher matcher = PARAMS_PATTERN.matcher(headerSrc);

        // iterate through funcions
        while (matcher.find()) {

            StringBuilder params = new StringBuilder(matcher.group(1));
//            System.out.println("- - - - ");
//            System.out.println(params.toString());
//            System.out.println("- - - - ");

            //iterate through params
            Matcher m = COMMENT_PATTERN.matcher(params);
            while(m.find()) {
                //uncomment param
                params.replace(m.start(2), m.end(2), "  ");
                params.replace(m.start(3), m.end(3), "  ");
            }

            //replace old params with uncommented params
            headerSrc.replace(matcher.start(1), matcher.end(1), params.toString());
        }

        if(replace) {
            //replace old file
            BufferedWriter out = new BufferedWriter(new FileWriter(destFile));
            out.write(headerSrc.toString());
            out.close();
        }else{
            System.out.println(headerSrc);
        }
    }


    private final StringBuilder readSourceFile(File file) throws FileNotFoundException, IOException {

        char[] buffer = new char[(int)file.length()];
        FileReader reader = new FileReader(file);
        int length = reader.read(buffer);
        if(reader != null) {
            reader.close();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(buffer, 0, length);

        return sb;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

}
