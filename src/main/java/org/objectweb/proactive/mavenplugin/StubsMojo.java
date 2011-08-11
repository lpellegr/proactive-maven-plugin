/**
 * This file is part of proactive-maven-plugin.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 **/
package org.objectweb.proactive.mavenplugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Mojo used to create ProActive stubs for the specified classes.
 * 
 * @goal stubs
 * @phase compile
 * @requiresDependencyResolution compile+runtime
 * 
 * @author lpellegr
 */
public class StubsMojo extends AbstractMojo {

    private static final String JAVASSIST_BYTE_CODE_STUB_BUILDER_CLASSNAME =
            "org.objectweb.proactive.core.mop.JavassistByteCodeStubBuilder";

    private static final String UTILS_CLASSNAME =
            "org.objectweb.proactive.core.mop.Utils";

    /**
     * Directory tree where the compiled remote classes are located.
     * 
     * @parameter default-value="${project.build.outputDirectory}"
     * @readonly
     */
    private File classesDirectory;

    /**
     * Specifies where to place the generated class files.
     * 
     * @parameter default-value="${project.build.outputDirectory}"
     */
    private File outputDirectory;

    /**
     * Compile classpath of the maven project.
     * 
     * @parameter expression="${project.compileClasspathElements}"
     * @readonly
     */
    private List<String> projectClasspathElements;

    /**
     * A list of inclusions when searching for classes to compile.
     * 
     * @parameter
     * @required
     */
    private List<String> includes;

    private URLClassLoader classLoader;

    private Class<?> javassistByteCodeStubBuilderClass;

    private Class<?> utilsClass;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        this.classLoader =
                Util.createClassLoader(this.projectClasspathElements);

        try {
            this.javassistByteCodeStubBuilderClass =
                    this.classLoader.loadClass(JAVASSIST_BYTE_CODE_STUB_BUILDER_CLASSNAME);
            this.utilsClass = this.classLoader.loadClass(UTILS_CLASSNAME);

        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException(
                    "ProActive Programming is not a dependency or a transitive dependency of the current module");
        }

        for (String className : this.includes) {
            this.createAndWriteStub(className);
        }
    }

    public void createAndWriteStub(String className)
            throws MojoExecutionException {
        // generates the bytecode for the class
        byte[] data;

        data = this.createStub(className);

        // writes the bytecode into a File
        String fileName =
                new File(
                        this.outputDirectory.toString(), this.getStubClassName(
                                className).replace('.', File.separatorChar)
                                + ".class").toString();

        FileOutputStream fos = null;
        try {
            new File(fileName.substring(
                    0, fileName.lastIndexOf(File.separatorChar))).mkdirs();

            // dumps the bytecode into the file
            fos = new FileOutputStream(new File(fileName));
            fos.write(data);
            fos.flush();

            super.getLog().info("Generated stub " + fileName);
        } catch (IOException e) {
            super.getLog().error(
                    "Failed to write stub for '" + className + "' in "
                            + fileName, e);
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private byte[] createStub(String className) throws MojoExecutionException {
        Method m = null;
        try {
            m =
                    this.javassistByteCodeStubBuilderClass.getMethod(
                            "create", String.class, Class[].class);

            return (byte[]) (m.invoke(null, className, null));
        } catch (SecurityException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            super.getLog().warn("Could not find class: " + className);
            super.getLog().info("Within this classpath:");

            for (int it = 0; it < classLoader.getURLs().length; ++it) {
                URL url = classLoader.getURLs()[it];
                super.getLog().info(" * " + url.toExternalForm());
            }

            throw new MojoExecutionException(
                    "Could not find "
                            + className
                            + " on the classpath. Please verify that the class is contain by the current module or by a dependency from the current module");
        }
    }

    private String getStubClassName(String className)
            throws MojoExecutionException {
        Method m;
        try {
            m =
                    this.utilsClass.getMethod(
                            "convertClassNameToStubClassName", String.class,
                            Class[].class);
            return (String) (m.invoke(null, className, null));
        } catch (SecurityException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
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
        return this.projectClasspathElements;
    }

}
