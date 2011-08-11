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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * Some convenient methods.
 * 
 * @author lpellegr
 */
public class Util {

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

}
