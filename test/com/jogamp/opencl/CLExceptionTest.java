
package com.jogamp.opencl;

import java.lang.reflect.InvocationTargetException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Michael Bien
 */
public class CLExceptionTest {

    @Test
    public void testCLExceptions() throws InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        Class<?>[] subTypes = CLException.class.getDeclaredClasses();

        for (Class<?> type : subTypes) {

            if(type.getName().startsWith(CLException.class.getName()+"$CL")) {

                CLException exception = (CLException) type.getConstructor(String.class).newInstance("foo");

                assertNotNull("can not resolve "+exception, CLException.resolveErrorCode(exception.errorcode));

                try{
                    CLException.checkForError(exception.errorcode, "foo");
                    fail("expected exception for: "+exception.getClass().getName()+" code: "+exception.errorcode);
                }catch(CLException ex) {
                    assertTrue("wrong instance; expected "+exception.getClass()+" but got "+ex.getClass(),
                            exception.getClass().equals(ex.getClass()));
                }
            }
        }
    }

}
