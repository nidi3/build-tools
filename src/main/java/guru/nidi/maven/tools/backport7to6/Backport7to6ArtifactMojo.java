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

import guru.nidi.maven.tools.MavenUtil;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;

import java.io.File;
import java.io.IOException;

@Mojo(name = "backport7to6-artifact", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class Backport7to6ArtifactMojo extends AbstractBackport7to6Mojo {
    @Component
    protected ProjectBuilder projectBuilder;

    @Parameter(property = "backport.groupId", required = true)
    private String groupId;

    @Parameter(property = "backport.artifactId", required = true)
    private String artifactId;

    @Parameter(property = "backport.version", required = true)
    private String version;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            final File dir = createTempDir();
            final Artifact artifact = resolveAndUnzipArtifact(dir);
            final MavenProject project = createMavenProject(dir, artifact);
            MavenUtil.extendPluginClasspath(project.getCompileClasspathElements());
            if (new Backporter7to6(getChecker(project), getLog()).backportFiles(dir, dir.getAbsolutePath())) {
                IoUtil.zip(dir, targetFile(artifact.getFile()));
            } else {
                getLog().info("No conversion needed.");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Could not backport", e);
        }
    }

    private File createTempDir() {
        final File dir = new File(System.getProperty("java.io.tmpdir"), "backport7to6/" + artifactId);
        dir.mkdirs();
        IoUtil.deleteAll(dir);
        return dir;
    }

    private Artifact resolveAndUnzipArtifact(File dir) throws IOException {
        final Artifact artifact = MavenUtil.resolveArtifactFile(session, repository, groupId, artifactId, version, "jar");
        IoUtil.unzip(artifact.getFile(), dir);
        return artifact;
    }

    private MavenProject createMavenProject(File dir, Artifact artifact) throws ProjectBuildingException {
        final MavenProject project = MavenUtil.projectFromArtifact(session, projectBuilder, artifact, true);
        project.getBuild().setOutputDirectory(dir.getAbsolutePath());
        return project;
    }

    private File targetFile(File jar) {
        final String targetName = jar.getName().substring(0, jar.getName().length() - 4);
        return new File(jar.getParentFile(), targetName + "-backported7to6.jar");
    }

}
