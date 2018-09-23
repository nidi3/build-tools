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
package guru.nidi.tools.dependency;

import guru.nidi.tools.maven.MavenContext;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DotCreator {
    private static final Map<String, DepInfo> deps = new LinkedHashMap<String, DepInfo>() {{
        put(Artifact.SCOPE_RUNTIME_PLUS_SYSTEM, new DepInfo("magenta", 1));
        put(Artifact.SCOPE_COMPILE_PLUS_RUNTIME, new DepInfo("grey", 2));
        put(Artifact.SCOPE_RUNTIME, new DepInfo("grey50", 3));
        put(Artifact.SCOPE_IMPORT, new DepInfo("green", 4));
        put(Artifact.SCOPE_SYSTEM, new DepInfo("magenta4", 5));
        put(Artifact.SCOPE_PROVIDED, new DepInfo("blue", 6));
        put(Artifact.SCOPE_TEST, new DepInfo("red", 7));
        put(Artifact.SCOPE_COMPILE, new DepInfo("black", 8));
    }};

    private final Log log;
    private final File outputDir;
    private final ArtifactFormatter formatter;
    private final DotCreatorParameters parameters;
    private final MavenContext mavenContext;

    public DotCreator(Log log, File outputDir, ArtifactFormatter formatter, DotCreatorParameters parameters, MavenContext mavenContext) {
        this.log = log;
        this.outputDir = outputDir;
        this.formatter = formatter;
        this.parameters = parameters;
        this.mavenContext = mavenContext;
    }

    public void writeComplete(Artifact artifact) throws IOException {
        final File file = fileFor(artifact, ".dot");
        log.debug("Writing " + file.getAbsolutePath());
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), UTF_8));
        out.println("digraph " + quoted(artifact) + "{");
        out.println("node [shape=box];");
        out.println("subgraph {");
        out.println("label=Legend;");
        out.println("node [shape=plaintext];");
        String label = "";
        for (Entry<String, DepInfo> dep : deps.entrySet()) {
            if (dep.getValue().order > 2 && parameters.isInScope(dep.getKey())) {
                out.println("edge [color=" + dep.getValue().color + "]; \"" + dep.getKey() + "\"->\"" + label + "\";");
                label += " ";
            }
        }
        out.println("}");
        out.println("rankdir=LR;");
        out.println(quoted(artifact) + " [label=" + label(artifact) + ",URL=\"/" + toString(artifact) + ".html\"];");
        try {
            final MavenProject project = mavenContext.projectFromArtifact(artifact);
            final Artifact parent = project.getParentArtifact();
            if (parent != null) {
                out.println("{ rank=same; " + quoted(artifact) + "; " + quoted(parent) + "; }");
                out.println(quoted(parent) + " [label=" + label(parent) + ",URL=\"/" + toString(parent) + ".html\"];");
                out.println(quoted(artifact) + "->" + quoted(parent) + ";");
                writeComplete(parent);
            }
            //TODO how to find modules?
//            if (project.getModules()!=null){
//                for(String module: project.getModules()){
//                    new DefaultArtifact(project.getGroupId(),module,)
//                }
//            }
        } catch (ProjectBuildingException e) {
            log.error("Problem processing dependencies", e);
        }
        final Collection<Artifact> res = calcDependencies(artifact);
        if (res != null) {
            writeDependencies(out, artifact, res, new HashSet<>(), parameters.maxDepth);
        }
        out.println("}");
        out.close();
    }

    private void writeDependencies(PrintWriter out, Artifact artifact, Collection<Artifact> res, Set<String> traversed, int depth) throws IOException {
        if (res != null) {
            for (Artifact a : ordered(res)) {
                out.println("edge [" + styleOf(a) + "];");
                out.println(quoted(a) + " [label=" + label(a) + ",URL=\"/" + toString(a) + ".html\"];");
                out.println(quoted(artifact) + "->" + quoted(a) + ";");
                if (!traversed.contains(toString(a)) && depth > 1) {
                    traversed.add(toString(a));
                    writeDependencies(out, a, calcDependencies(a), traversed, depth - 1);
                    if (!parameters.simple && !fileFor(a, ".dot").exists()) {
                        writeComplete(a);
                    }
                }
            }
        }
    }

    private File fileFor(Artifact artifact, String suffix) {
        return new File(outputDir, formatter.filenameFor(artifact, suffix));
    }

    private Collection<Artifact> calcDependencies(Artifact artifact) {
        artifact.setScope(null);
        final ArtifactResolutionResult res = mavenContext.resolveArtifact(artifact, parameters);
        res.getArtifacts().removeIf(a -> a.equals(artifact) || !parameters.include(a) || a.getDependencyTrail().size() != 2);
        if (res.getArtifacts().isEmpty() && !res.getMissingArtifacts().isEmpty()) {
            return null;
        }
        return res.getArtifacts();
    }

    private String quoted(Artifact artifact) {
        return formatter.quoted(artifact);
    }

    private String label(Artifact artifact) {
        return formatter.label(artifact);
    }

    private String toString(Artifact artifact) {
        return formatter.toString(artifact);
    }

    private String styleOf(Artifact artifact) {
        final DepInfo di = deps.get(artifact.getScope());
        String style = "color=" + (di == null ? "black" : di.color);
        style += " style=" + (artifact.isOptional() ? "dotted" : "solid");
        return style;
    }

    private Collection<Artifact> ordered(Collection<Artifact> artifacts) {
        final ArrayList<Artifact> res = new ArrayList<>(artifacts);
        res.sort(new Comparator<Artifact>() {
            @Override
            public int compare(Artifact a1, Artifact a2) {
                return order(a2) - order(a1);
            }

            private int order(Artifact a) {
                final DepInfo di = deps.get(a.getScope());
                return di == null ? 0 : di.order;
            }
        });
        return res;
    }

    private static class DepInfo {
        private final String color;
        private final int order;

        public DepInfo(String color, int order) {
            this.color = color;
            this.order = order;
        }
    }

}
