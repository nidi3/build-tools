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
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.mojo.animal_sniffer.ClassFileVisitor;
import org.codehaus.mojo.animal_sniffer.ClassListBuilder;
import org.codehaus.mojo.animal_sniffer.SignatureChecker;
import org.codehaus.mojo.animal_sniffer.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

/**
 *
 */
public abstract class AbstractBackport7to6Mojo extends AbstractMojo {
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @component
     * @required
     * @readonly
     */
    protected MavenSession session;
    /**
     * @component
     * @required
     * @readonly
     */
    protected RepositorySystem repository;

    protected SignatureChecker getChecker() throws IOException {
        final File signature = MavenUtil.resolveArtifactFile(session, repository, "org.codehaus.mojo.signature", "java16", "1.1", "signature");
        final SignatureChecker checker = new SignatureChecker(new FileInputStream(signature), buildPackageList(), new SnifferLogger());
        checker.setSourcePath(Arrays.asList(new File(".")));
        return checker;
    }

    private Set<String> buildPackageList() throws IOException {
        ClassListBuilder plb = new ClassListBuilder(new SnifferLogger());
        apply(plb);
        return plb.getPackages();
    }

    private void apply(ClassFileVisitor v) throws IOException {
        v.process(new File(project.getBuild().getOutputDirectory()));
        for (Artifact artifact : project.getArtifacts()) {
            if (artifact.getArtifactHandler().isAddedToClasspath()) {
                if (Artifact.SCOPE_COMPILE.equals(artifact.getScope()) ||
                        Artifact.SCOPE_PROVIDED.equals(artifact.getScope()) ||
                        Artifact.SCOPE_SYSTEM.equals(artifact.getScope())) {
                    v.process(artifact.getFile());
                }
            }
        }
    }

    class SnifferLogger implements Logger {
        public void info(String message) {
            getLog().info(message);
        }

        public void info(String message, Throwable t) {
            getLog().info(message, t);
        }

        public void debug(String message) {
            getLog().debug(message);
        }

        public void debug(String message, Throwable t) {
            getLog().debug(message, t);
        }

        public void warn(String message) {
            getLog().warn(message);
        }

        public void warn(String message, Throwable t) {
            getLog().warn(message, t);
        }

        public void error(String message) {
                getLog().error(message);
        }

        public void error(String message, Throwable t) {
            getLog().error(message, t);
        }
    }
}