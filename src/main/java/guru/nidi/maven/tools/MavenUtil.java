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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.*;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;

import java.io.File;
import java.util.List;

/**
 *
 */
public class MavenUtil {
    private MavenUtil() {
    }

    public static void extendPluginClasspath(List<String> elements) throws MojoExecutionException {
        ClassWorld world = new ClassWorld();
        try {
            ClassRealm realm = world.newRealm("maven", Thread.currentThread().getContextClassLoader());
            for (String element : elements) {
                File elementFile = new File(element);
                realm.addConstituent(elementFile.toURI().toURL());
            }
            Thread.currentThread().setContextClassLoader(realm.getClassLoader());
        } catch (Exception ex) {
            throw new MojoExecutionException(ex.toString(), ex);
        }
    }

    public static Artifact resolveArtifactFile(MavenSession session, RepositorySystem repository, String groupId, String artifactId, String version, String type) {
        final DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, version, "compile", type, "", new DefaultArtifactHandler(type));
        final ArtifactResolutionResult result = resolveArtifact(session, repository, artifact, false, null);
        for (Artifact a : result.getArtifacts()) {
            if (a.equals(artifact)) {
                return a;
            }
        }
        throw new IllegalArgumentException(artifact + " could not be resolved");
    }

    public static ArtifactResolutionResult resolveArtifact(MavenSession session, RepositorySystem repository, Artifact artifact, boolean transitive, ArtifactFilter resolutionFilter) {
        artifact.setArtifactHandler(new DefaultArtifactHandler(artifact.getType()));
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
                .setArtifact(artifact)
                .setResolveRoot(true)
                .setServers(session.getRequest().getServers())
                .setMirrors(session.getRequest().getMirrors())
                .setProxies(session.getRequest().getProxies())
                .setLocalRepository(session.getLocalRepository())
                .setRemoteRepositories(session.getRequest().getRemoteRepositories())
                .setResolveTransitively(transitive)
                .setCollectionFilter(resolutionFilter)
                .setResolutionFilter(resolutionFilter);
        //.setListeners(Arrays.<ResolutionListener>asList(new DebugResolutionListener(new ConsoleLogger())));
        return repository.resolve(request);
    }

    public static MavenProject projectFromArtifact(MavenSession session, ProjectBuilder projectBuilder, Artifact artifact, boolean resolveDependencies) throws ProjectBuildingException {
        final ProjectBuildingRequest request = new DefaultProjectBuildingRequest()
                .setLocalRepository(session.getLocalRepository())
                .setRepositorySession(session.getRepositorySession())
                .setSystemProperties(System.getProperties())
                .setResolveDependencies(resolveDependencies);
        return projectBuilder.build(artifact, request).getProject();
    }
}
