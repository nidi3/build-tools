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

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import java.io.File;

/**
 * @goal backport7to6-artifact
 * @phase prepare-package
 */
public class Backport7to6ArtifactMojo extends AbstractMojo {
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;
    /**
     * @component
     * @required
     * @readonly
     */
    private MavenSession session;
    /**
     * @component
     * @required
     * @readonly
     */
    private RepositorySystem repository;
    /**
     * @parameter expression="${groupId}"
     * @required
     * @readonly
     */
    private String groupId;
    /**
     * @parameter expression="${artifactId}"
     * @required
     * @readonly
     */
    private String artifactId;
    /**
     * @parameter expression="${version}"
     * @required
     * @readonly
     */
    private String version;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            new Backporter7to6(getLog()).backportJar(resolve());
        } catch (Exception e) {
            throw new MojoExecutionException("Could not backport", e);
        }
    }

    private File resolve() {
        final DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, version, "compile", "jar", "", new DefaultArtifactHandler("jar"));

        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        request.setResolveRoot(true).setResolveTransitively(false);
        request.setServers(session.getRequest().getServers());
        request.setMirrors(session.getRequest().getMirrors());
        request.setProxies(session.getRequest().getProxies());
        request.setLocalRepository(session.getLocalRepository());
        request.setRemoteRepositories(session.getRequest().getRemoteRepositories());
        repository.resolve(request);

        return artifact.getFile();
    }
}
