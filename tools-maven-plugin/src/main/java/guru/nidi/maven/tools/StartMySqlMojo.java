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
package guru.nidi.maven.tools;

import guru.nidi.tools.docker.DockerContainer;
import guru.nidi.tools.docker.StartResult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.List;

import static guru.nidi.tools.Maps.map;
import static java.nio.charset.StandardCharsets.UTF_8;

@Mojo(name = "startMySql")
public class StartMySqlMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(property = "mysql.version", defaultValue = "5.7")
    private String version;

    @Parameter(property = "mysql.label", defaultValue = "maven-mysql")
    private String label;

    @Parameter(property = "mysql.password", defaultValue = "root")
    private String password;

    @Parameter(property = "mysql.port", defaultValue = "3306")
    private int port;

    @Parameter(property = "mysql.stopIfRunning", defaultValue = "true")
    private boolean stopIfRunning;

    @Parameter(property = "mysql.database", required = true)
    private String database;

    @Parameter(property = "mysql.scripts")
    private List<String> scripts;

    @Parameter(property = "mysql.failOnError", defaultValue = "false")
    private boolean failOnError;

    public static void main(String[] args) throws MojoExecutionException {
        final StartMySqlMojo m = new StartMySqlMojo();
        m.version = "5.6";
        m.label = "test";
        m.password = "hula";
        m.port = 3300;
        m.database = "db";
        m.stopIfRunning = true;
        m.execute();
    }

    public void execute() throws MojoExecutionException {
        final DockerContainer container = new DockerContainer("mysql:" + version, label);
        if (stopIfRunning) {
            container.stop();
        }
        final StartResult result = container.start(
                100, 5 * 60 * 60 * 1000L,
                map("MYSQL_ROOT_PASSWORD", password, "MYSQL_DATABASE", database),
                map(port, 3306),
                map("--max_allowed_packet", "16384", "--bind-address", "0.0.0.0"),
                (msg, age) -> {
                    if (age == 0) {
                        getLog().info("[Docker] " + msg);
                        return StartResult.waiting();
                    }
                    try (final Connection c = DriverManager.getConnection("jdbc:mysql://127.0.0.1:" + port + "/" + database, "root", password)) {
                        if (scripts != null) {
                            runScripts(c);
                        }
                        return StartResult.ok();
                    } catch (SQLException e) {
                        return StartResult.waiting();
                    }
                });
        if (result.state == StartResult.State.FAIL) {
            throw new MojoExecutionException("Problem starting container", result.exception);
        }
    }

    private void runScripts(Connection c) {
        try {
            doRunScripts(c);
        } catch (IOException e) {
            throw new RuntimeException("Problem reading script files", e);
        }
    }

    private void doRunScripts(Connection c) throws IOException {
        for (final String script : scripts) {
            final List<File> files = FileUtils.getFiles(project.getBasedir(), script, null);
            for (final File file : files) {
                getLog().info("Executing SQL script " + project.getBasedir().toPath().relativize(file.toPath()));
                final EncodedResource resource = new EncodedResource(new FileSystemResource(file), UTF_8);
                ScriptUtils.executeSqlScript(c, resource, !failOnError, !failOnError, "--", ";", "/*", "*/");
            }
        }
    }
}
