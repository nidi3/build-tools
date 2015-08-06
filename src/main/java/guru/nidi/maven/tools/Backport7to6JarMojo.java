/*
 * Copyright (C) 2014 Stefan Niederhauser (nidin@gmx.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package guru.nidi.maven.tools;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

/**
 * @goal backport7to6-jar
 * @phase prepare-package
 * @requiresProject false
 */
public class Backport7to6JarMojo extends AbstractMojo {
    /**
     * @parameter expression="${file}"
     * @required
     * @readonly
     */
    private File file;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!file.exists() || !file.getName().endsWith(".jar")) {
            throw new MojoExecutionException("File must exists and be a jar");
        }
        try {
            new Backporter7to6(getLog()).backportJar(file);
        } catch (Exception e) {
            throw new MojoExecutionException("Could not backport", e);
        }
    }


}
