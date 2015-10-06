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
package guru.nidi.maven.tools.dependency;

import guru.nidi.maven.tools.MavenUtil;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.repository.RepositorySystem;

/**
 *
 */
public class MavenContext {
    private final MavenSession session;
    private final RepositorySystem repository;
    private final ProjectBuilder projectBuilder;

    public MavenContext(MavenSession session, RepositorySystem repository, ProjectBuilder projectBuilder) {
        this.session = session;
        this.repository = repository;
        this.projectBuilder = projectBuilder;
    }

    public MavenProject projectFromArtifact(Artifact artifact) throws ProjectBuildingException {
        return MavenUtil.projectFromArtifact(session, projectBuilder, artifact, false);
    }

    public ArtifactResolutionResult resolveArtifact(Artifact artifact, ArtifactFilter filter) {
        return MavenUtil.resolveArtifact(session, repository, artifact, true, filter);
    }



}
