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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * Create a graphics for the dependencies of a project.
 * The <a href="http://www.graphviz.org/">Graphviz</a> library must be installed and be available on the path.
 *
 * @goal dependency
 */
public class DependencyMojo extends AbstractDependencyMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (project.isExecutionRoot()) {
            try {
                deleteOutput();
                createFiles(project.getArtifact());
                if (!simple) {
                    final Server server = new Server(8888);
                    final ServletContextHandler handler = new ServletContextHandler();
                    final ServletHolder sh = new ServletHolder(new ServingServlet());
                    handler.addServlet(sh, "/*");
                    server.setHandler(handler);
                    server.setStopAtShutdown(true);
                    server.start();
                    server.join();
                }
            } catch (Exception e) {
                throw new MojoExecutionException("Could not execute goal", e);
            }
        }
    }

    private void createFiles(Artifact artifact) {
        File file = new File(htmlDir(), toString(artifact) + ".html");
        try {
            writeComplete(artifact);
            final File[] files = findDotFiles();
            executeDots(files);
            createHtmls(files);
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
        protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
            final String path = req.getPathInfo() == null || req.getPathInfo().equals("/")
                    ? DependencyMojo.this.toString(project.getArtifact())
                    : req.getPathInfo().substring(1);
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
                    final String[] parts = query.split(":");
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
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
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