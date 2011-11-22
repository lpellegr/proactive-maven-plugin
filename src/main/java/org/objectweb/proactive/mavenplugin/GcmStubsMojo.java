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
import java.io.FileFilter;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Mojo used to create ProActive/GCM stubs (representative of component
 * interfaces).
 * 
 * @goal gcm-stubs
 * 
 * @author bsauvan
 */
public class GcmStubsMojo extends AbstractClassesMojo {

    private static final String UTILS_CLASSNAME =
            "org.objectweb.proactive.core.component.gen.Utils";

    private static final String INTERFACE_ELEMENT = "interface";

    private static final String NAME_ATTRIBUTE = "name";

    private static final String SIGNATURE_ATTRIBUTE = "signature";

    private Class<?> utilsClass;

    /**
     * Directory tree where the compiled remote classes are located.
     * 
     * @parameter default-value="${project.build.outputDirectory}"
     * @readonly
     */
    private File classesDirectory;

    protected void init() throws MojoExecutionException {
        try {
            this.utilsClass = this.classLoader.loadClass(UTILS_CLASSNAME);
        } catch (ClassNotFoundException cnfe) {
            throw new MojoExecutionException(
                    "ProActive Programming is not a dependency or a transitive dependency of the current module");
        }
    }

    protected List<String> getClassNames() throws MojoExecutionException {
        // gets all fractal files
        final ArrayList<File> fractalFiles = new ArrayList<File>();
        this.classesDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(final File pathname) {
                if (pathname.isDirectory()) {
                    pathname.listFiles(this);
                }
                if (pathname.getName().endsWith(".fractal")) {
                    fractalFiles.add(pathname);
                }
                return false;
            }
        });

        // parses all fractal files
        try {
            Method utilsMethod =
                    this.utilsClass.getMethod(
                            "getMetaObjectComponentRepresentativeClassName",
                            String.class, String.class);
            List<String> stubClassNames = new ArrayList<String>();
            XMLInputFactory factory = XMLInputFactory.newInstance();

            for (File fractalFile : fractalFiles) {
                Reader reader = new FileReader(fractalFile);
                XMLStreamReader streamReader =
                        factory.createXMLStreamReader(reader);

                while (streamReader.hasNext()) {
                    int code = streamReader.next();

                    if (code == XMLStreamReader.START_ELEMENT) {
                        String elementName = streamReader.getLocalName();

                        if (elementName.equals(INTERFACE_ELEMENT)) {
                            String name =
                                    streamReader.getAttributeValue(
                                            null, NAME_ATTRIBUTE);
                            String signature =
                                    streamReader.getAttributeValue(
                                            null, SIGNATURE_ATTRIBUTE);
                            String stubClassName =
                                    (String) utilsMethod.invoke(
                                            null, name, signature);
                            stubClassNames.add(stubClassName);
                        }
                    }
                }
            }

            return stubClassNames;
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    protected byte[] generateClass(String className) throws Exception {
        Method getClassDataMethod =
                this.utilsClass.getMethod("getClassData", String.class);

        return (byte[]) getClassDataMethod.invoke(null, className);
    }

    protected String getKind() {
        return "ProActive/GCM stub";
    }

}
