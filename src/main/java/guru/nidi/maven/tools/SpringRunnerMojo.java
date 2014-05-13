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

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * @goal runSpring
 * @requiresDependencyResolution test
 */
public class SpringRunnerMojo extends AbstractMojo {
    private final static String APP_CONTEXT_CLASS = "org.springframework.context.support.FileSystemXmlApplicationContext";

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${contextFile}"
     * @required
     */
    private File contextFile;

    /**
     * @parameter expression="${profiles}"
     * @required
     */
    private String profiles;

    /**
     * @parameter expression="${failOnError}"
     */
    private boolean failOnError;

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("******************** If using IntelliJ, consider using grep console plugin ********************");
        LogConfiguration.useLogConfig("logback-blue.xml");

        try {
            MavenClasspathUtil.extendPluginClasspath(testClasspathElements());
            System.setProperty("spring.profiles.active", profiles);
            System.setProperty("basedir", project.getBasedir().getAbsolutePath());
            runSpringUsingReflection();
        } catch (Throwable e) {
            handleException("Problem running spring.", e);
        } finally {
            LogConfiguration.useLogConfig("logback.xml");
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> testClasspathElements() throws DependencyResolutionRequiredException {
        return project.getTestClasspathElements();
    }

    private void runSpringUsingReflection() throws Throwable {
        try {
            getLog().info("Starting spring context...");
            final Class<?> appContextClass = Thread.currentThread().getContextClassLoader().loadClass(APP_CONTEXT_CLASS);
            final Constructor<?> constructor = appContextClass.getConstructor(String.class);
            final Object appContext = constructor.newInstance("file:" + contextFile.getAbsolutePath());
            getLog().info("Started. Stopping spring context...");
            appContextClass.getMethod("stop").invoke(appContext);
            getLog().info("Stopped.");
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } catch (Exception e) {
            handleException("Problem starting spring. Assure that spring is in project's classpath and " +
                    "'constructor " + APP_CONTEXT_CLASS + "(String)' is existing.", e);
        }
    }

    private void handleException(String msg, Throwable e) throws MojoExecutionException {
        if (failOnError) {
            throw new MojoExecutionException(msg, e);
        } else {
            getLog().error(msg + " Continuing anyways...", e);
        }
    }
}
