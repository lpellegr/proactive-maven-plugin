package com.elaunira.pmp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.sun.xml.internal.rngom.ast.builder.BuildException;

/**
 * Compiles ProActive stubs and skeleton classes from a remote implementation
 * class.
 * 
 * @goal stubs
 * @phase compile
 * @requiresDependencyResolution compile
 * 
 * @author lpellegr
 */
public class StubsMojo extends AbstractMojo {

	private static String STUB_GENERATOR_CLASSNAME = "org.objectweb.proactive.ext.util.QuietStubGenerator";

	/**
	 * Directory tree where the compiled remote classes are located.
	 * 
	 * @parameter default-value="${project.build.outputDirectory}"
	 */
	private File classesDirectory;

	/**
	 * Compile classpath of the maven project.
	 * 
	 * @parameter expression="${project.compileClasspathElements}"
	 * @readonly
	 */
	private List<String> projectCompileClasspathElements;
	
	/**
	 * A list of inclusions when searching for classes to compile.
	 * 
	 * @parameter
	 */
	private String[] includes;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		URLClassLoader classLoader = null;
		boolean containsProActive = false;
		for (String elem : this.projectCompileClasspathElements) {
			if (elem.contains("org/objectweb/proactive/proactive/")) {
				containsProActive = true;
				break;
			}
		}

		if (!containsProActive) {
			throw new MojoFailureException(
					"ProActive library cannot be found in the current maven project classpath.");
		}

		try {
			URL[] classpathUrls = new URL[this.projectCompileClasspathElements
					.size()];

			for (int i = 0; i < this.projectCompileClasspathElements.size(); i++) {
				classpathUrls[i] = new File(
						this.projectCompileClasspathElements.get(i)).toURI().toURL();
			}

			classLoader = new URLClassLoader(classpathUrls);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		Class<?> stubGeneratorClass = null;
		try {
			stubGeneratorClass = classLoader.loadClass(STUB_GENERATOR_CLASSNAME);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	

		String[] classes = this.includes;
		if (System.getProperty("class") != null) {
			int length = this.includes == null ? 0 : this.includes.length;
			classes = new String[length + 1];
			for (int i = 0; i < length; i++) {
				classes[i] = this.includes[i];
			}
			classes[length] = System.getProperty("class");
		}
		
		
		
		
		for (String clazz : classes) {
			this.generateClass(clazz);
//			try {
//				stubGeneratorMethod.invoke(null, new Object[] { new String[] {
//						"-srcDir", this.classesDirectory.toString(),
//						"-destDir", this.classesDirectory.toString(), "-class",
//						c } });
//			} catch (IllegalAccessException e) {
//				e.printStackTrace();
//			} catch (InvocationTargetException e) {
//				e.printStackTrace();
//			}
		}
	}

	 public void generateClass(String className) throws MojoExecutionException {
         String stubClassName = null;

         try {
             // Generates the bytecode for the class
             byte[] data;

             data = this.createStub(className);
             stubClassName = this.getStubClassName(className);

             // Write the bytecode into a File
             char sep = File.separatorChar;
             String fileName = new File(
            		 				this.classesDirectory.toString(), 
            		 				stubClassName.replace('.', sep) + ".class").toString();
             try {
                 new File(fileName.substring(0, fileName.lastIndexOf(sep))).mkdirs();
                 // dump the bytecode into the file
                 File f = new File(fileName);
                 FileOutputStream fos = new FileOutputStream(f);
                 fos.write(data);
                 fos.flush();
                 fos.close();
                 System.out.println("Written " + fileName);
             } catch (IOException e) {
                 throw new MojoExecutionException("Failed to write stub for " + className + " in " + fileName, e);
             }
         } catch (Throwable e) {
             e.printStackTrace();
             throw new MojoExecutionException("Stub generation failed for class: " + className, e);
         }

     }
	
    private byte[] createStub(String className) throws Exception {
        // Do not import the class since Utils must not depends on the core
        Class<?> cl = Class.forName("org.objectweb.proactive.core.mop.JavassistByteCodeStubBuilder");
        Method m = cl.getMethod("create", String.class, Class[].class);
        return (byte[]) (m.invoke(null, className, null));
    }

    private String getStubClassName(String className) throws Exception {
        // Do not import the class sinceUtils must not depeonds on the core
        Class<?> cl = Class.forName("org.objectweb.proactive.core.mop.Utils");
        Method m = cl.getMethod("convertClassNameToStubClassName", String.class, Class[].class);
        return (String) (m.invoke(null, className, null));
    }
	
	/**
	 * Get the directory where the project classes are located.
	 * 
	 * @return the project classes directory.
	 */
	public File getClassesDirectory() {
		return this.classesDirectory;
	}

	/**
	 * Get the list of classpath elements for the project.
	 * 
	 * @return a list containing the project classpath elements.
	 */
	public List<String> getProjectClasspathElements() {
		return this.projectCompileClasspathElements;
	}

}
