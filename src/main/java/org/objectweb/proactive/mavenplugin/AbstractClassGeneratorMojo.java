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
import java.net.URLClassLoader;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * This class is used to provide the operations which are common to all the
 * ProActive class generations.
 * 
 * @phase compile
 * @requiresDependencyResolution compile+runtime
 * 
 * @author lpellegr
 * @author bsauvan
 */
public abstract class AbstractClassGeneratorMojo extends AbstractMojo {

    /**
     * Compile classpath of the maven project.
     * 
     * @parameter expression="${project.compileClasspathElements}"
     * @readonly
     */
    protected List<String> projectClasspathElements;

    /**
     * Specifies where to place the generated class files.
     * 
     * @parameter default-value="${project.build.outputDirectory}"
     */
    protected File outputDirectory;

    protected URLClassLoader classLoader;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        this.classLoader =
                Util.createClassLoader(this.projectClasspathElements);

        this.init();

        List<String> classNames = this.getClassNames();
        Thread.currentThread().setContextClassLoader(this.classLoader);
        for (final String className : classNames) {
            try {
                this.classLoader.loadClass(className);
            } catch (ClassNotFoundException cnfe) {
                try {
                    byte[] data = this.generateClass(className);

                    String fileName =
                            this.writeClass(
                                    this.outputDirectory, className, data);

                    this.getLog().info(
                            "Generated " + this.getKind() + " " + fileName);
                } catch (Exception e) {
                    this.getLog().error(
                            "Failed to generate " + this.getKind() + " "
                                    + className, e);
                }
            }
        }
    }

    protected abstract void init() throws MojoExecutionException;

    protected abstract List<String> getClassNames()
            throws MojoExecutionException;

    protected abstract byte[] generateClass(String className) throws Exception;

    public String writeClass(File outputDirectory, String className, byte[] data)
            throws IOException {
        // writes the bytecode into a file
        String fileName =
                new File(outputDirectory.toString(), className.replace(
                        '.', File.separatorChar)
                        + ".class").toString();
        FileOutputStream fos = null;

        try {
            new File(fileName.substring(
                    0, fileName.lastIndexOf(File.separatorChar))).mkdirs();

            // dumps the bytecode into the file
            fos = new FileOutputStream(new File(fileName));
            fos.write(data);
            fos.flush();
        } finally {
            try {
                fos.close();
            } catch (IOException ioe) {
                this.getLog().error("Failed to close " + fileName, ioe);
            }
        }

        return fileName;
    }

    protected abstract String getKind();

}
