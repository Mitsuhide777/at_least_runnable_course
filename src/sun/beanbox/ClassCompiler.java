package sun.beanbox;

/**
 * A class that generates .class files
 * It currently uses the sun.tools.javac.* classes
 */

import javax.lang.model.SourceVersion;
import javax.tools.*;
import java.io.File;
import java.util.Arrays;
import java.util.Properties;

public class ClassCompiler 
{

    public static boolean compile(String fileName, String classpath) 
	{
//----------------------------- My test for JavaCompiler API usage ---------------------------------------------------
/*
        // TODO: read this: http://www.ibm.com/developerworks/library/j-jcomp/
        // http://www.javacodegeeks.com/2015/09/java-compiler-api.html
        // http://docs.oracle.com/javase/7/docs/api/javax/tools/JavaCompiler.html
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
//        for( final SourceVersion version: compiler.getSourceVersions() ) {
//            System.out.println( version );
//        }
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, null );
//        final File file = new File(CompilerExample.class.getResource("/SampleClass.java").toURI() );
        final File file = new File(fileName);
        final Iterable< ? extends JavaFileObject > sources = manager.getJavaFileObjectsFromFiles( Arrays.asList( file ));
        final JavaCompiler.CompilationTask task = compiler.getTask( null, manager, diagnostics, null, null, sources );
        task.call();
        for( final Diagnostic< ? extends JavaFileObject > diagnostic: diagnostics.getDiagnostics() ) {
            System.out.format("%s, line %d in %s",
                diagnostic.getMessage( null ),
                diagnostic.getLineNumber(),
                diagnostic.getSource().getName() );
        }
//*/
//--------------------------------------------------------------------------------------------------------------------
System.err.println("ClassCompiler.compile("+fileName+", "+classpath+"):");
String java_class_path = System.getProperty("java.class.path");
System.err.println("java_class_path = "+java_class_path);

//        String cmnd_line = "C:\\PROGRA~1\\Java\\jdk1.8.0_66\\bin\\javac "; //TODO: check the path to javac.exe
		String cmnd_line = "javac "; //TODO: check the path to javac.exe

		String[] cmdarray = new String[2];
		cmdarray[0] = cmnd_line;
		cmdarray[1] = fileName;
		String[] envp = new String[1];
		envp[0] = "classpath="+classpath;
//        envp[0] = "classpath="+"\""+classpath+"\"";

		Process p = null;	
		try 
		{
			p = Runtime.getRuntime().exec(cmdarray, envp);

			int exit_value = p.waitFor();
			System.err.println("exec(): exit_value = "+exit_value);
			return (exit_value == 0) ? true : false;
						
		}
		catch (Throwable th) 
		{
			System.err.println(
				"WARNING: Could not run Runtime.exec(String) in the standard way: " + th);
			th.printStackTrace();
		}	
	
		return warningMessage();
    }
    
    static boolean warningMessage()
	{
	    System.err.println("");
	    System.err.println("Check that the version of \"javac\" that you are running");
	    System.err.println("is the one supplied with Sun's JDK1.x (which includes the");
	    System.err.println("compiler classes) and not some other version of \"java\"");
	    System.err.println("or JRE shipped with some other product.");
	    System.err.println("");
	    return false;
    }
}
