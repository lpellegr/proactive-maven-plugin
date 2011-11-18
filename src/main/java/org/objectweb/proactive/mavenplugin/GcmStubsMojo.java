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
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

/**
 * Mojo used to create ProActive/GCM stubs (representative of component
 * interfaces).
 * 
 * @goal gcm-stubs
 * @phase compile
 * @requiresDependencyResolution compile+runtime
 * 
 * @author bsauvan
 */
public class GcmStubsMojo extends AbstractMojo {

    private static final String UTILS_CLASSNAME =
            "org.objectweb.proactive.core.component.gen.Utils";

    private static final String INTERFACE_ELEMENT = "interface";

    private static final String NAME_ATTRIBUTE = "name";

    private static final String SIGNATURE_ATTRIBUTE = "signature";

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

    private URLClassLoader classLoader;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        this.classLoader =
                Util.createClassLoader(this.projectClasspathElements);

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
        Set<String> stubClassNames = new HashSet<String>();
        try {
            final XMLInputFactory factory = XMLInputFactory.newInstance();
            Class<?> utilsClass = this.classLoader.loadClass(UTILS_CLASSNAME);
            Method utilsMethod =
                    utilsClass.getMethod(
                            "getMetaObjectComponentRepresentativeClassName",
                            String.class, String.class);

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
        } catch (ClassNotFoundException cnfe) {
            throw new MojoExecutionException(
                    "ProActive Programming is not a dependency or a transitive dependency of the current module");
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        // creates thread pool
        ExecutorService executorService =
                Executors.newFixedThreadPool(Runtime.getRuntime()
                        .availableProcessors(), new ThreadFactory() {
                    @Override
                    public Thread newThread(final Runnable r) {
                        Thread thread = new Thread(r);
                        try {
                            thread.setContextClassLoader(classLoader);
                        } catch (SecurityException se) {
                            Log log = new SystemStreamLog();
                            log.error("Failed to set context class loader", se);
                        }
                        return thread;
                    }
                });

        // submits all generators
        for (final String stubClassName : stubClassNames) {
            try {
                this.classLoader.loadClass(stubClassName);
            } catch (ClassNotFoundException cnfe) {
                executorService.submit(new GcmStubGenerator(
                        super.getLog(), stubClassName, this.outputDirectory));
            }
        }

        // awaits termination
        try {
            executorService.shutdown();
            executorService.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException ie) {
            throw new MojoExecutionException(ie.getMessage(), ie);
        }
    }

    static final class GcmStubGenerator implements Runnable {

        final Log log;

        final String className;

        final File outputDirectory;

        public GcmStubGenerator(Log log, String className, File outputDirectory) {
            this.log = log;
            this.className = className;
            this.outputDirectory = outputDirectory;
        }

        @Override
        public void run() {
            try {
                ClassLoader classLoader =
                        Thread.currentThread().getContextClassLoader();
                Class<?> clazz = classLoader.loadClass(UTILS_CLASSNAME);
                Method method = clazz.getMethod("getClassData", String.class);
                byte[] data = (byte[]) method.invoke(null, this.className);

                Util.writeClass(this.outputDirectory, this.className, data);

                this.log.info("Generated ProActive/GCM stub " + this.className);
            } catch (Exception e) {
                this.log.error("Failed to generate ProActive/GCM stub "
                        + this.className, e);
            }
        }

    }

}
