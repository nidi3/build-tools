package stni.maven.tools;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

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
    protected MavenProject project;

    /**
     * @parameter expression="${contextFile}"
     * @required
     */
    protected File contextFile;

    /**
     * @parameter expression="${failOnError}"
     */
    protected boolean failOnError;

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("******************** If using IntelliJ, consider using grep console plugin ********************");
        LogConfiguration.useLogConfig("logback-blue.xml");

        try {
            MavenClasspathUtil.extendPluginClasspath(project.getTestClasspathElements());
            System.setProperty("basedir", project.getBasedir().getAbsolutePath());
            runSpringUsingReflection();
        } catch (Throwable e) {
            handleException("Problem running spring.", e);
        } finally {
            LogConfiguration.useLogConfig("logback.xml");
        }
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
