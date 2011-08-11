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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Retrieves the version of the ProActive Programming dependency used in the
 * module on which the goal is executed.
 * 
 * @goal version
 * @requiresDependencyResolution compile+runtime
 * @requiresDirectInvocation true
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
    private List<String> projectClasspathElements;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        URLClassLoader classLoader =
                Util.createClassLoader(this.projectClasspathElements);

        Class<?> paVersionClass;
        try {
            paVersionClass =
                    classLoader.loadClass("org.objectweb.proactive.api.PAVersion");
            Method getProActiveVersionMethod =
                    paVersionClass.getMethod("getProActiveVersion");

            super.getLog().info(
                    "ProActive Programming "
                            + getProActiveVersionMethod.invoke(null)
                            + " detected");
        } catch (ClassNotFoundException e) {
            super.getLog()
                    .info(
                            "ProActive Programming is not a dependency or a transitive dependency of the current module");
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

}
