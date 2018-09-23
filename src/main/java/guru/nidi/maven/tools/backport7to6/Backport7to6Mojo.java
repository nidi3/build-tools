/*
 * Copyright © 2014 Stefan Niederhauser (nidin@gmx.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package guru.nidi.maven.tools.backport7to6;

import guru.nidi.maven.tools.util.MavenUtil;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.io.IOException;

@Mojo(name = "backport7to6", defaultPhase = LifecyclePhase.TEST_COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class Backport7to6Mojo extends AbstractBackport7to6Mojo {
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * If a separate artifact with classifier 'jdk6' should be created.
     */
    @Parameter(property = "backport.classified")
    private boolean classified;

    public void execute() throws MojoExecutionException {
        final File classes = new File(project.getBuild().getOutputDirectory());
        try {
            MavenUtil.extendPluginClasspath(project.getCompileClasspathElements());
            final String base = project.getBasedir().getParentFile().getAbsolutePath();
            if (classified) {
                final File jdk6Classes = new File(classes.getParentFile(), "classes-jdk6");
                IoUtil.copyRecursively(classes, jdk6Classes);
                backport(base, jdk6Classes);
                final File jar = new File(jdk6Classes.getParentFile(), project.getBuild().getFinalName() + "-jdk6.jar");
                IoUtil.zip(jdk6Classes, jar);
                projectHelper.attachArtifact(project, "jar", "jdk6", jar);
            } else {
                backport(base, classes);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Could not backport", e);
        }
    }

    private boolean backport(String base, File target) throws IOException {
        return new Backporter7to6(getChecker(project), getLog()).backportFiles(target, base);
    }
}
