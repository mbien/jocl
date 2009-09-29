
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
public class FunctionParamUncommenter {

    final static Pattern PARAMS_PATTERN
            = compile("cl\\w+ \\(   (  \\s* [^;]+  )  \\)", MULTILINE|COMMENTS);

    final static Pattern COMMENT_PATTERN
            = compile("\\s*(const)?\\w+\\s* \\**\\s+ (/\\*) \\s+[^\\*\\[]+ (\\*/)", MULTILINE|COMMENTS);
                                                                     //^ array size in param name causes some problems

    public static void main(String[] args) throws FileNotFoundException, IOException {
        uncomment("/home/mbien/NetBeansProjects/JOGL/jocl/resources/CL/cl.h", false);
        uncomment("/home/mbien/NetBeansProjects/JOGL/jocl/resources/CL/cl_gl.h", false);
    }

    private static void uncomment(String file, boolean replace) throws FileNotFoundException, IOException {

        System.out.println("- - - begin uncomment - - -");

        StringBuilder src = readSourceFile(new File(file));
        Matcher matcher = PARAMS_PATTERN.matcher(src);

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
            src.replace(matcher.start(1), matcher.end(1), params.toString());
        }

        if(replace) {
            //replace old file
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.write(src.toString());
            out.close();
        }else{
            System.out.println(src);
        }

        System.out.println("- - - done - - -");
    }


    private static StringBuilder readSourceFile(File file) throws FileNotFoundException, IOException {

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

}
