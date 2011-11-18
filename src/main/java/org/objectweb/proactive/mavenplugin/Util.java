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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

/**
 * Some convenient methods.
 * 
 * @author lpellegr
 * @author bsauvan
 */
public class Util {

    private static final Log log = new SystemStreamLog();

    public static URLClassLoader createClassLoader(List<String> urls) {
        URL[] classpathUrls = new URL[urls.size()];

        try {
            for (int i = 0; i < urls.size(); i++) {
                classpathUrls[i] = new File(urls.get(i)).toURI().toURL();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return new URLClassLoader(classpathUrls);
    }

    public static void writeClass(File outputDirectory, String className,
                                  byte[] data) throws IOException {
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
                log.error("Failed to close " + fileName, ioe);
            }
        }
    }

}
