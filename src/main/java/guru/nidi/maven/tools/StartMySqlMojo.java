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

import static guru.nidi.maven.tools.Maps.map;
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

    public void execute() throws MojoExecutionException {
        final DockerContainer container = new DockerContainer("mysql:" + version, label);
        if (stopIfRunning) {
            container.stop();
        }
        container.start(
                map("MYSQL_ROOT_PASSWORD", password, "MYSQL_DATABASE", database),
                map(3306, port),
                map("--max_allowed_packet", "16384", "--bind-address", "0.0.0.0"),
                log -> log.contains("starting as process 1"));
        if (scripts != null) {
            runScripts();
        }
    }

    private void runScripts() throws MojoExecutionException {
        try {
            doRunScripts();
        } catch (IOException e) {
            throw new MojoExecutionException("Problem reading script files", e);
        } catch (SQLException e) {
            throw new MojoExecutionException("Could not connect to database", e);
        }
    }

    private void doRunScripts() throws SQLException, IOException {
        final Connection c = DriverManager.getConnection("jdbc:mysql://127.0.0.1/" + database, "root", password);
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
