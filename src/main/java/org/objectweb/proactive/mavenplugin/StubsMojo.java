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

    private Method createMethod;

    private Method convertClassNameToStubClassNameMethod;

    private Method convertStubClassNameToClassNameMethod;

    protected void init() throws MojoExecutionException {
        try {
            Class<?> utilsClass = this.classLoader.loadClass(UTILS_CLASSNAME);
            this.convertClassNameToStubClassNameMethod =
                    utilsClass.getMethod(
                            "convertClassNameToStubClassName", String.class,
                            Class[].class);
            this.convertStubClassNameToClassNameMethod =
                    utilsClass.getMethod(
                            "convertStubClassNameToClassName", String.class);
            Class<?> javassistByteCodeStubBuilderClass =
                    this.classLoader.loadClass(JAVASSIST_BYTE_CODE_STUB_BUILDER_CLASSNAME);
            this.createMethod =
                    javassistByteCodeStubBuilderClass.getMethod(
                            "create", String.class, Class[].class);
        } catch (ClassNotFoundException cnfe) {
            throw new MojoExecutionException(
                    "ProActive Programming is not a dependency or a transitive dependency of the current module");
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    protected List<String> getClassNames() throws MojoExecutionException {
        try {
            List<String> classNames = new ArrayList<String>();

            for (String include : this.includes) {
                classNames.add(this.getStubClassName(include));
            }

            return classNames;
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private String getStubClassName(String className) throws Exception {
        return (String) (this.convertClassNameToStubClassNameMethod.invoke(
                null, className, null));
    }

    protected byte[] generateClass(String className) throws Exception {
        return (byte[]) (this.createMethod.invoke(
                null, this.getObjectClassName(className), null));
    }

    private String getObjectClassName(String stubClassName) throws Exception {
        return (String) (this.convertStubClassNameToClassNameMethod.invoke(
                null, stubClassName));
    }

    protected String getKind() {
        return "ProActive stub";
    }

}
