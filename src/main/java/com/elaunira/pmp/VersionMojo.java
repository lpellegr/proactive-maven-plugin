package com.elaunira.pmp;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Retrieves the version of the ProActive Programming depencency used in the
 * module on which the goal is executed.
 * 
 * @goal version
 * @requiresDependencyResolution compile
 * 
 * @author lpellegr
 */
public class VersionMojo extends AbstractMojo {

	/**
	 * Compile classpath of the maven project.
	 * 
	 * @parameter expression="${project.compileClasspathElements}"
	 * @readonly
	 */
	private List<String> projectCompileClasspathElements;
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		boolean containsProActive = false;
		for (String path : this.projectCompileClasspathElements) {
			if (path.contains("org/objectweb/proactive/")) {
				containsProActive = true;
				break;
			}
		}

		if (!containsProActive) {
			System.out.println("ProActive Programming is not used in the current module.");
		} else {
			
			try {
				URL[] classpathUrls = 
					new URL[this.projectCompileClasspathElements.size()];

				for (int i = 0; i < this.projectCompileClasspathElements.size(); i++) {
					classpathUrls[i] = new File(
							this.projectCompileClasspathElements.get(i)).toURI().toURL();
				}

				URLClassLoader classLoader = new URLClassLoader(classpathUrls);
				
				try {
					Class<?> cl = classLoader.loadClass("org.objectweb.proactive.api.PAVersion");
					Method m = cl.getMethod("getProActiveVersion");
					System.out.println("The current module use ProActive Programming " +  m.invoke(null));
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
	}

}
