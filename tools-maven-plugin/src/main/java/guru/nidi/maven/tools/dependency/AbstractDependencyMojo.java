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
package guru.nidi.maven.tools.dependency;

import guru.nidi.tools.dependency.*;
import guru.nidi.tools.maven.MavenContext;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.repository.RepositorySystem;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractDependencyMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    protected MavenSession session;

    @Component
    protected RepositorySystem repository;

    @Component
    protected ProjectBuilder projectBuilder;

    /**
     * The root artifact for the dependency tree. Default is the current project.
     * Must be of the form {@code [groupId]:[artifactId]:[type]:[version]}.
     */
    @Parameter(property = "rootArtifact")
    protected String rootArtifact;

    /**
     * Maximum depth of displayed dependencies.
     */
    @Parameter(property = "maxDepth", defaultValue = "3")
    protected int maxDepth;

    /**
     * If the dot file should be interpreted client side in the browser.
     * If false, graphViz must be installed on the machine and available on PATH.
     */
    @Parameter(property = "clientSide", defaultValue = "false")
    protected boolean clientSide;

    /**
     * Display optional dependencies.
     */
    @Parameter(property = "optional", defaultValue = "false")
    protected boolean optional;

    /**
     * A comma separated list of scopes to be displayed.
     */
    @Parameter(property = "scopes", defaultValue = "compile,provided,system,import,runtime")
    protected String scopes;

    /**
     * Create a simple image or a html file with a clickable image map.
     * If true, a server will be started on port 8888.
     */
    @Parameter(property = "simple", defaultValue = "false")
    protected boolean simple;

    /**
     * Clear already calculated images.
     */
    @Parameter(property = "clear", defaultValue = "false")
    protected boolean clear;

    /**
     * Dependencies that should NOT be shown. Is a regex of the form
     * {@code [groupId]:[artifactId]:[type]:[version]}
     */
    @Parameter(property = "excludes")
    protected String excludes;

    /**
     * Dependencies that should be shown takes precedence over excludes. Is a regex of the form
     * {@code [groupId]:[artifactId]:[type]:[version]}
     */
    @Parameter(property = "includes")
    protected String includes;

    /**
     * Formatting the name of the artifacts. A format has the form {@code [filter]->[format]}.<br>
     * {@code [filter]} is a regex of the same form as includes/excludes. <br>
     * {@code [format]} is a kind of printf format with the following tags:
     * <ul>
     * <li>%g groupId</li>
     * <li>%a artifactId</li>
     * <li>%t type</li>
     * <li>%v version</li>
     * <li>%n newline</li>
     * <li>%[len]d[s] The description of the project with linebreaks after at least [len] characters. If [s] is given, only the first sentence of the description is used.</li>
     * </ul>
     * Example: "guru\.nidi.*-&gt;%a%n%20d" Format everything with a groupId starting with "guru.nidi" as artifactId, newline, description with linebreaks every 20 characters.
     */
    @Parameter(property = "formats")
    protected List<String> formats;

    /**
     */
    @Parameter(property = "outputFile", defaultValue = "target/dependencies.png")
    protected File outputFile;

    private ArtifactFormatter formatter;

    protected File dotDir() {
        return new File(System.getProperty("java.io.tmpdir") + "/dependencyGraph", optional + "-" + simple + "-" + scopesString() + "-" + maxDepth);
    }

    private String scopesString() {
        return scopes == null ? "[]" : Arrays.asList(scopes.split(",")).toString().replace(" ", "");
    }

    protected File htmlDir() {
        return new File(dotDir(), "html");
    }

    protected void deleteOutput() {
        dotDir().mkdirs();
        if (clear) {
            IoUtil.deleteAll(dotDir());
        }
        htmlDir().mkdirs();
    }

    protected ArtifactFormatter formatter() {
        if (formatter == null) {
            formatter = new ArtifactFormatter(formats, context());
        }
        return formatter;
    }

    protected MavenContext context() {
        return new MavenContext(session, repository, projectBuilder);
    }

    protected DotCreatorParameters parameters() {
        return new DotCreatorParameters(simple, maxDepth, optional, scopes, excludes, includes);
    }
}
