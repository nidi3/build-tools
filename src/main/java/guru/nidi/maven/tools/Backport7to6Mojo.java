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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @goal backport7to6
 * @phase prepare-package
 */
public class Backport7to6Mojo extends AbstractMojo {
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        final File classes = new File(project.getBuild().getOutputDirectory());
        try {
            backport(classes, project.getBasedir().getParentFile().getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Could not backport", e);
        }
    }

    private void backport(File dir, String base) throws IOException {
        final File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    backport(file, base);
                } else if (file.isFile() && file.getName().endsWith(".class")) {
                    final RandomAccessFile raf = new RandomAccessFile(file, "rw");
                    raf.seek(6);
                    raf.writeShort(0x32);
                    raf.close();
                    getLog().info(file.getAbsolutePath().substring(base.length() + 1) + " converted.");
                }
            }
        }
    }
}
