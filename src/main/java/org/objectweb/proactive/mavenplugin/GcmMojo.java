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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Mojo used to create ProActive/GCM meta objects and ProActive/GCM stubs
 * (representatives of component interfaces).
 * 
 * @goal gcm
 * 
 * @author bsauvan
 */
public class GcmMojo extends AbstractClassGeneratorMojo {

    private static final String UTILS_CLASSNAME =
            "org.objectweb.proactive.core.component.gen.Utils";

    private static final String COMPONENT_CLASSNAME =
            "org.objectweb.fractal.api.Component";

    private static final String CONTROLLER_DESCRIPTION_CLASSNAME =
            "org.objectweb.proactive.core.component.ControllerDescription";

    private static final String INTERFACE_ELEMENT = "interface";

    private static final String NAME_ATTRIBUTE = "name";

    private static final String SIGNATURE_ATTRIBUTE = "signature";

    private static final String CONTROLLER_ELEMENT = "controller";

    private static final String DESC_ATTRIBUTE = "desc";

    /**
     * Directory tree where the compiled remote classes are located.
     * 
     * @parameter default-value="${project.build.outputDirectory}"
     * @readonly
     */
    private File classesDirectory;

    private Class<?> componentClass;

    private Constructor<?> controllerDescriptionConstructor;

    private Method getControllersSignaturesMethod;

    private Method getMetaObjectClassNameMethod;

    private Method getMetaObjectComponentRepresentativeClassNameMethod;

    private Method getClassDataMethod;

    private List<String> classNames;

    protected void init() throws MojoExecutionException {
        try {
            this.componentClass =
                    this.classLoader.loadClass(COMPONENT_CLASSNAME);
            Class<?> controllerDescriptionClass =
                    this.classLoader.loadClass(CONTROLLER_DESCRIPTION_CLASSNAME);
            this.controllerDescriptionConstructor =
                    controllerDescriptionClass.getConstructor(
                            String.class, String.class, String.class);
            this.getControllersSignaturesMethod =
                    controllerDescriptionClass.getMethod("getControllersSignatures");
            Class<?> utilsClass = this.classLoader.loadClass(UTILS_CLASSNAME);
            this.getMetaObjectClassNameMethod =
                    utilsClass.getMethod(
                            "getMetaObjectClassName", String.class,
                            String.class);
            this.getMetaObjectComponentRepresentativeClassNameMethod =
                    utilsClass.getMethod(
                            "getMetaObjectComponentRepresentativeClassName",
                            String.class, String.class);
            this.getClassDataMethod =
                    utilsClass.getMethod("getClassData", String.class);
            this.classNames = new ArrayList<String>();
        } catch (ClassNotFoundException cnfe) {
            throw new MojoExecutionException(
                    "ProActive Programming is not a dependency or a transitive dependency of the current module");
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    protected List<String> getClassNames() throws MojoExecutionException {
        try {
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
            XMLInputFactory factory = XMLInputFactory.newInstance();

            // adds meta object controller classnames for default controllers
            this.addControllerClassNames(null);

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

                            this.addMetaObjectAndRepresentativeClassName(
                                    name, signature);
                        } else if (elementName.equals(CONTROLLER_ELEMENT)) {
                            String desc =
                                    streamReader.getAttributeValue(
                                            null, DESC_ATTRIBUTE);
                            if (!desc.equals("primitive")
                                    && !desc.equals("composite")) {
                                this.addControllerClassNames(desc);
                            }
                        }
                    }
                }
            }

            return this.classNames;
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void addControllerClassNames(String controllersConfigFileLocation)
            throws Exception {
        Object controllerDescription =
                this.controllerDescriptionConstructor.newInstance(
                        null, null, controllersConfigFileLocation);
        Map<String, String> controllersSignatures =
                (Map<String, String>) this.getControllersSignaturesMethod.invoke(controllerDescription);

        for (String interfaceSignature : controllersSignatures.keySet()) {
            try {
                String interfaceImplementation =
                        controllersSignatures.get(interfaceSignature);
                Class<?> interfaceImplementationClass =
                        this.classLoader.loadClass(interfaceImplementation);
                Constructor<?> interfaceImplementationConstructor =
                        interfaceImplementationClass.getConstructor(this.componentClass);
                Object controllerInstance =
                        interfaceImplementationConstructor.newInstance((Object) null);
                Method getFcItfNameMethod =
                        controllerInstance.getClass().getMethod("getFcItfName");
                String interfaceName =
                        (String) getFcItfNameMethod.invoke(controllerInstance);

                this.addMetaObjectAndRepresentativeClassName(
                        interfaceName, interfaceSignature);
            } catch (Exception e) {
                this.getLog().warn(
                        "Cannot generate meta object for controller "
                                + interfaceSignature);
                if (this.getLog().isDebugEnabled()) {
                    this.getLog().debug(e);
                }
            }
        }
    }

    private void addMetaObjectAndRepresentativeClassName(String interfaceName,
                                                         String interfaceSignature)
            throws Exception {
        String metaObjectClassName =
                (String) this.getMetaObjectClassNameMethod.invoke(
                        null, interfaceName, interfaceSignature);

        if (!this.classNames.contains(metaObjectClassName)) {
            this.classNames.add(metaObjectClassName);
        }

        String representativeClassName =
                (String) this.getMetaObjectComponentRepresentativeClassNameMethod.invoke(
                        null, interfaceName, interfaceSignature);

        if (!this.classNames.contains(representativeClassName)) {
            this.classNames.add(representativeClassName);
        }
    }

    protected byte[] generateClass(String className) throws Exception {
        return (byte[]) this.getClassDataMethod.invoke(null, className);
    }

    protected String getKind() {
        return "ProActive/GCM class";
    }

}
