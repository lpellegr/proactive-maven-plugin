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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Mojo used to create ProActive stubs for the specified classes.
 * 
 * @goal stubs
 * 
 * @author lpellegr
 * @author bsauvan
 */
public class StubsMojo extends AbstractClassesMojo {

    private static final String UTILS_CLASSNAME =
            "org.objectweb.proactive.core.mop.Utils";

    private static final String JAVASSIST_BYTE_CODE_STUB_BUILDER_CLASSNAME =
            "org.objectweb.proactive.core.mop.JavassistByteCodeStubBuilder";

    /**
     * A list of inclusions when searching for classes to compile.
     * 
     * @parameter
     * @required
     */
    private List<String> includes;

    private Class<?> utilsClass;

    private Class<?> javassistByteCodeStubBuilderClass;

    protected void init() throws MojoExecutionException {
        try {
            this.utilsClass = this.classLoader.loadClass(UTILS_CLASSNAME);
            this.javassistByteCodeStubBuilderClass =
                    this.classLoader.loadClass(JAVASSIST_BYTE_CODE_STUB_BUILDER_CLASSNAME);
        } catch (ClassNotFoundException cnfe) {
            throw new MojoExecutionException(
                    "ProActive Programming is not a dependency or a transitive dependency of the current module");
        }
    }

    protected List<String> getClassNames() throws MojoExecutionException {
        List<String> classNames = new ArrayList<String>();

        for (String include : this.includes) {
            classNames.add(this.getStubClassName(include));
        }

        return classNames;
    }

    private String getStubClassName(String className)
            throws MojoExecutionException {
        try {
            Method convertClassNameToStubClassNameMethod =
                    this.utilsClass.getMethod(
                            "convertClassNameToStubClassName", String.class,
                            Class[].class);

            return (String) (convertClassNameToStubClassNameMethod.invoke(
                    null, className, null));
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    protected byte[] generateClass(String className) throws Exception {
        Method createMethod =
                javassistByteCodeStubBuilderClass.getMethod(
                        "create", String.class, Class[].class);

        return (byte[]) (createMethod.invoke(
                null, this.getObjectClassName(className), null));
    }

    private String getObjectClassName(String stubClassName)
            throws MojoExecutionException {
        try {
            Method convertClassNameToStubClassNameMethod =
                    this.utilsClass.getMethod(
                            "convertStubClassNameToClassName", String.class);

            return (String) (convertClassNameToStubClassNameMethod.invoke(
                    null, stubClassName));
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    protected String getKind() {
        return "ProActive stub";
    }

}
