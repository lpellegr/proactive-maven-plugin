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

    /**
     * {@inheritDoc}
     */
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
                    classpathUrls[i] =
                            new File(
                                    this.projectCompileClasspathElements.get(i)).toURI()
                                    .toURL();
                }

                URLClassLoader classLoader = new URLClassLoader(classpathUrls);

                Class<?> cl =
                        classLoader.loadClass("org.objectweb.proactive.api.PAVersion");
                Method m = cl.getMethod("getProActiveVersion");
                System.out.println("The current module uses ProActive Programming "
                        + m.invoke(null));
            } catch (MalformedURLException e) {
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
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

}
