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

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.util.List;

/**
 * @requiresDependencyResolution test
 */
public abstract class AbstractRunnerMojo extends AbstractMojo {
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter expression="${failOnError}"
     */
    private boolean failOnError;

    protected abstract void run() throws Throwable;

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("******************** If using IntelliJ, consider using grep console plugin ********************");
        LogConfiguration.useLogConfig("logback-blue.xml");

        try {
            MavenUtil.extendPluginClasspath(testClasspathElements());
            run();
        } catch (Throwable e) {
            handleException("Problem running.", e);
        } finally {
            LogConfiguration.useLogConfig("logback.xml");
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> testClasspathElements() throws DependencyResolutionRequiredException {
        return project.getTestClasspathElements();
    }

    protected void handleException(String msg, Throwable e) throws MojoExecutionException {
        if (failOnError) {
            throw new MojoExecutionException(msg, e);
        } else {
            getLog().error(msg + " Continuing anyways...", e);
        }
    }
}
