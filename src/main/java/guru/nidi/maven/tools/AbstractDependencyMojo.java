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
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.repository.RepositorySystem;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @requiresDependencyResolution test
 */
public abstract class AbstractDependencyMojo extends AbstractMojo {

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

    /**
     * @component
     * @required
     * @readonly
     */
    protected ProjectBuilder projectBuilder;

    /**
     * Maximum depth of displayed dependencies.
     *
     * @parameter expression="${maxDepth}"
     */
    protected int maxDepth = 3;

    /**
     * Display optional dependencies.
     *
     * @parameter expression="${optional}"
     */
    protected boolean optional = false;

    /**
     * A comma separated list of scopes to be displayed.
     *
     * @parameter expression="${scopes}"
     */
    protected String scopes;

    /**
     * Create a simple image or a html file with a clickable image map.
     * If true, a server will be started on port 8888.
     *
     * @parameter expression="${simple}"
     */
    protected boolean simple;

    /**
     * Clear already calculated images.
     *
     * @parameter expression="${clear}"
     */
    protected boolean clear = false;


    private ArtifactFilter artifactFilter;

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

    protected String toString(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() +
                ("jar".equals(artifact.getType()) ? "" : (":" + artifact.getType())) +
                (empty(artifact.getClassifier()) ? "" : (":" + artifact.getClassifier()));
    }

    private boolean empty(String s) {
        return s == null || s.length() == 0;
    }

    private File outputDir() {
        return new File(System.getProperty("java.io.tmpdir") + "/dependencyGraph", optional + "-" + simple + "-" + scopesString() + "-" + maxDepth);
    }

    private String scopesString() {
        return scopes == null ? "[]" : Arrays.asList(scopes.split(",")).toString().replace(" ", "");
    }

    protected File htmlDir() {
        return new File(outputDir(), "html");
    }

    protected void deleteOutput() throws IOException {
        outputDir().mkdirs();
        if (clear) {
            deleteAll(outputDir());
        }
        htmlDir().mkdirs();
    }

    private void deleteAll(File dir) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                deleteAll(f);
            } else {
                f.delete();
            }
        }
    }

    protected void writeComplete(Artifact artifact) throws IOException {
        final Collection<Artifact> res = calcDependencies(artifact);
        if (res != null) {
            final PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileFor(artifact, ".dot")), "utf-8"));
            out.println("digraph " + quoted(artifact) + "{");
            out.println("node [shape=box];");
            out.println("subgraph {");
            out.println("label=Legend;");
            out.println("node [shape=plaintext];");
            String label = "";
            for (Map.Entry<String, DepInfo> dep : deps.entrySet()) {
                if (dep.getValue().order > 2) {
                    out.println("edge [color=" + dep.getValue().color + "]; \"" + dep.getKey() + "\"->\"" + label + "\";");
                    label += " ";
                }
            }
            out.println("}");
            out.println("rankdir=LR;");
            out.println(quoted(artifact) + " [URL=\"/" + toString(artifact)+ ".html\"];");
            try {
                final MavenProject project = MavenUtil.projectFromArtifact(session, projectBuilder, artifact, false);
                final Artifact parent = project.getParentArtifact();
                if (parent != null) {
                    out.println("{ rank=same; " + quoted(artifact) + "; " + quoted(parent) + "; }");
                    out.println(quoted(parent) + " [URL=\"/" + toString(parent)+".html\"];");
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
                getLog().info(e.getMessage());
            }
            writeDependencies(out, artifact, res, new HashSet<String>(), maxDepth);
            out.println("}");
            out.close();
        }
    }

    private File fileFor(Artifact artifact, String suffix) {
        return new File(outputDir(), filenameFor(artifact, suffix));
    }

    protected String filenameFor(Artifact artifact, String suffix) {
        return toString(artifact).replace(":", "$") + suffix;
    }

    private Collection<Artifact> calcDependencies(Artifact artifact) {
        artifact.setScope(null);
        final ArtifactResolutionResult res = MavenUtil.resolveArtifact(session, repository, artifact, true, artifactFilter());
        for (Iterator<Artifact> it = res.getArtifacts().iterator(); it.hasNext(); ) {
            final Artifact a = it.next();
            if (a.equals(artifact) || !artifactFilter().include(a) || a.getDependencyTrail().size() != 2) {
                it.remove();
            }
        }
        if (res.getArtifacts().isEmpty() && !res.getMissingArtifacts().isEmpty()) {
            return null;
        }
        return res.getArtifacts();
    }

    private String quoted(Artifact artifact) {
        return "\"" + toString(artifact) + "\"";
    }

    private void writeDependencies(PrintWriter out, Artifact artifact, Collection<Artifact> res, Set<String> traversed, int depth) throws IOException {
        if (res != null) {
            for (Artifact a : ordered(res)) {
                out.println("edge [" + styleOf(a) + "];");
                out.println(quoted(a) + " [URL=\"/" + toString(a)+ ".html\"];");
                out.println(quoted(artifact) + "->" + quoted(a) + ";");
                if (!traversed.contains(toString(a)) && depth > 1) {
                    traversed.add(toString(a));
                    writeDependencies(out, a, calcDependencies(a), traversed, depth - 1);
                    if (!simple && !fileFor(a, ".dot").exists()) {
                        writeComplete(a);
                    }
                }
            }
        }
    }

    private String styleOf(Artifact artifact) {
        final DepInfo di = deps.get(artifact.getScope());
        String style = "color=" + (di == null ? "black" : di.color);
        style += " style=" + (artifact.isOptional() ? "dotted" : "solid");
        return style;
    }

    private Collection<Artifact> ordered(Collection<Artifact> artifacts) {
        final ArrayList<Artifact> res = new ArrayList<Artifact>(artifacts);
        Collections.sort(res, new Comparator<Artifact>() {
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

    protected File[] findDotFiles() {
        return outputDir().listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".dot");
            }
        });
    }

    protected void executeDots(File[] files) {
        getLog().info("Executing dot files...");
        final ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (final File f : files) {
            es.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        executeDot(f);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        es.shutdown();
        try {
            es.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void executeDot(File f) throws IOException, InterruptedException {
        final File png = fileEnding(f, htmlDir(), ".png");
        final File map = fileEnding(f, outputDir(), ".map");
        if (!png.exists() || (!simple && !map.exists())) {
            final List<String> args = new ArrayList<String>(Arrays.asList("dot", f.getName(), "-Tpng", "-o" + png.getAbsolutePath()));
            if (!simple) {
                args.addAll(Arrays.asList("-Tcmapx", "-o" + map.getAbsolutePath()));
            }
            final Process dot = new ProcessBuilder(args).directory(f.getParentFile()).redirectErrorStream(true).start();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                final InputStream in = dot.getInputStream();
//                byte[] buf = new byte[10000];
//                while (true) {
//                    try {
//                        int read = in.read(buf);
//                        if (read > 0) {
//                            System.out.print(new String(buf, 0, read));
//                        }
//                        Thread.sleep(50);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }).start();
            dot.waitFor();
        }
    }

    private File fileEnding(File file, File baseDir, String ending) {
        return new File(baseDir, fileEnding(file, ending));
    }

    private String fileEnding(File file, String ending) {
        return file.getName().substring(0, file.getName().length() - 4) + ending;
    }

    protected void createHtmls(File[] files) throws IOException {
        for (final File f : files) {
            createHtml(f);
        }
    }

    private void createHtml(File f) throws IOException {
        if (!simple) {
            final File output = fileEnding(f, htmlDir(), ".html");
            if (!output.exists()) {
                final FileOutputStream fos = new FileOutputStream(output);
                final PrintWriter out = new PrintWriter(new OutputStreamWriter(fos, "utf-8"));
                out.println("<html><body>");
                out.flush();
                final FileInputStream map = new FileInputStream(fileEnding(f, outputDir(), ".map"));
                copy(map, fos);
                map.close();
                out.println("<img src='./" + fileEnding(f, ".png") + "' usemap='#" + fileEnding(f, "").replace('$', ':') + "'></img>");
                out.println("</body></html>");
                out.close();
            }
        }
    }

    protected void copy(InputStream in, OutputStream out) throws IOException {
        final byte[] buf = new byte[10000];
        int read;
        while ((read = in.read(buf)) > 0) {
            out.write(buf, 0, read);
        }
    }

    private ArtifactFilter artifactFilter() {
        if (artifactFilter == null) {
            artifactFilter = new ArtifactFilter() {
                @Override
                public boolean include(Artifact artifact) {
                    if (!optional && artifact.isOptional()) {
                        return false;
                    }
                    if (artifact.getScope() != null && (scopes != null && !scopes.contains(artifact.getScope()))) {
                        return false;
                    }
                    return true;
                }
            };
        }
        return artifactFilter;
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
