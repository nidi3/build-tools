package stni.maven.tools;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.io.File;

/**
 * @goal runSpring
 * @requiresDependencyResolution compile
 */
public class SpringRunnerMojo extends AbstractMojo {
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
            MavenClasspathUtil.extendPluginClasspath(project.getCompileClasspathElements());
            System.setProperty("basedir", project.getBasedir().getAbsolutePath());
            final FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext("file:" + contextFile.getAbsolutePath());
            context.start();
            getLog().info("Started. Stopping spring context...");
            context.stop();
            getLog().info("Stopped.");
        } catch (Exception e) {
            if (failOnError) {
                throw new MojoExecutionException("Problem running spring", e);
            } else {
                getLog().error("Problem running spring, but continuing", e);
            }
        } finally {
            LogConfiguration.useLogConfig("logback.xml");
        }
    }
}
