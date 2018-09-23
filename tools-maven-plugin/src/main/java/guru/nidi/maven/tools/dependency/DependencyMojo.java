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
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ShutdownHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.http.*;
import java.io.*;

/**
 * Create a graphics for the dependencies of a project.
 * The <a href="http://www.graphviz.org/">Graphviz</a> library must be installed and be available on the path.
 */
@Mojo(name = "dependency", requiresDependencyResolution = ResolutionScope.TEST)
public class DependencyMojo extends AbstractDependencyMojo {
    private static final int PORT = 8888;

    public void execute() throws MojoExecutionException {
        if (project.isExecutionRoot() || rootArtifact != null) {
            try {
                deleteOutput();
                createFiles(rootArtifact());
                if (simple) {
                    copyPng();
                } else {
                    startServer();
                }
            } catch (Exception e) {
                throw new MojoExecutionException("Could not execute goal", e);
            }
        }
    }

    private Artifact rootArtifact() {
        if (rootArtifact == null) {
            return project.getArtifact();
        }
        final String[] parts = rootArtifact.split(":");
        if (parts.length == 3) {
            return new DefaultArtifact(parts[0], parts[1], parts[2], null, "jar", "", new DefaultArtifactHandler());
        }
        if (parts.length == 4) {
            return new DefaultArtifact(parts[0], parts[1], parts[3], null, parts[2], "", new DefaultArtifactHandler());
        }
        throw new IllegalArgumentException("Invalid root artifact format " + rootArtifact);
    }

    private void startServer() throws Exception {
        final Server server = new Server(PORT);
        server.setHandler(createHandler());
        server.setStopAtShutdown(true);
        server.start();
        getLog().info("Please open a browser at http://localhost:" + PORT);
        server.join();
    }

    private Handler createHandler() {
        final ServletContextHandler main = new ServletContextHandler();
        main.addServlet(new ServletHolder(new ServingServlet()), "/*");
        final HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{new ShutdownHandler("666", true, true), main});
        return handlers;
    }

    private void copyPng() throws IOException {
        final File png = new File(htmlDir(), formatter().filenameFor(rootArtifact(), ".png"));
        if (!png.exists()) {
            getLog().warn(png + " was not generated.");
        } else {
            final InputStream in = new FileInputStream(png);
            outputFile.getParentFile().mkdirs();
            final OutputStream out = new FileOutputStream(outputFile);
            IoUtil.copy(in, out);
            in.close();
            out.close();
        }
    }

    private void createFiles(Artifact artifact) {
        File file = new File(htmlDir(), formatter().filenameFor(artifact, ".html"));
        try {
            getLog().info("Creating dot files...");
            final DotCreator dotCreator = new DotCreator(getLog(), dotDir(), formatter(), parameters(), context());
            dotCreator.writeComplete(artifact);
            final DotProcessor dotProcessor = new DotProcessor(dotDir(), htmlDir(), simple);
            final File[] files = dotProcessor.findDotFiles();
            if (!clientSide) {
                getLog().info("Processing dot files...");
                dotProcessor.executeDots(files);
            }
            if (!simple) {
                getLog().info("Creating HTML files...");
                new HtmlCreator(dotDir(), htmlDir(), clientSide).createHtmls(files);
            }
        } catch (Exception e) {
            e.printStackTrace();
            writeNotFound(file);
        }
        if (!file.exists()) {
            writeNotFound(file);
        }
    }

    private void writeNotFound(File file) {
        try {
            final FileOutputStream out = new FileOutputStream(file);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ServingServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            final String path = (req.getPathInfo() == null || req.getPathInfo().equals("/")
                    ? formatter().toString(rootArtifact())
                    : req.getPathInfo().substring(1)).replace(':', '$');
            if (path.endsWith(".png")) {
                serveResource(path, res);
            } else {
                applyParams(req);
                deleteOutput();
                final String query = path.endsWith(".html")
                        ? path.substring(0, path.length() - 5)
                        : path;
                final String file = query + ".html";

                final File source = new File(htmlDir(), file);
                if (!source.exists()) {
                    final String[] parts = query.split("\\$");
                    if (parts.length < 3) {
                        res.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    createFiles(parts);
                    getLog().info("Files created.");
                }
                serveResource(file, res);
            }
        }

        private void applyParams(HttpServletRequest req) {
            final String pOptional = req.getParameter("optional");
            if (pOptional != null) {
                optional = Boolean.parseBoolean(pOptional);
            }
            final String pSimple = req.getParameter("simple");
            if (pSimple != null) {
                simple = Boolean.parseBoolean(pSimple);
            }
            final String pScopes = req.getParameter("scopes");
            if (pScopes != null) {
                scopes = pScopes;
            }
            final String pMaxDepth = req.getParameter("maxDepth");
            if (pMaxDepth != null) {
                maxDepth = Integer.parseInt(pMaxDepth);
            }
        }

        private void createFiles(String[] parts) {
            final DefaultArtifact artifact = new DefaultArtifact(parts[0], parts[1], parts[2], null,
                    parts.length > 3 ? parts[3] : "jar", parts.length > 4 ? parts[4] : "", null);
            DependencyMojo.this.createFiles(artifact);
        }

        private void serveResource(String path, HttpServletResponse res) throws IOException {
            final File source = new File(htmlDir(), path);
            if (source.length() == 0) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND, source.getAbsolutePath() + " not found.");
                return;
            }
            final FileInputStream in = new FileInputStream(source);
            copy(in, res.getOutputStream());
            in.close();
            res.flushBuffer();
        }

        private void copy(InputStream in, OutputStream out) throws IOException {
            final byte[] buf = new byte[10000];
            int read;
            while ((read = in.read(buf)) > 0) {
                out.write(buf, 0, read);
            }
        }
    }

}