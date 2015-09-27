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
package guru.nidi.maven.tools.backport7to6;

import guru.nidi.maven.tools.MavenUtil;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

/**
 * @goal backport7to6
 * @phase prepare-package
 * @requiresDependencyResolution compile
 */
public class Backport7to6Mojo extends AbstractBackport7to6Mojo {
    public void execute() throws MojoExecutionException, MojoFailureException {
        final File classes = new File(project.getBuild().getOutputDirectory());
        try {
            MavenUtil.extendPluginClasspath(project.getCompileClasspathElements());
            final String base = project.getBasedir().getParentFile().getAbsolutePath();
            new Backporter7to6(getChecker(project), getLog()).backportFiles(classes, base);
        } catch (Exception e) {
            throw new MojoExecutionException("Could not backport", e);
        }
    }
}
