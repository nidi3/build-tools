package stni.maven.tools;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @goal consoleInput
 */
public class ConsoleInputMojo extends AbstractMojo {
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter expression="${prompt}"
     * @required
     */
    protected String prompt;

    /**
     * @parameter expression="${targetProperty}"
     * @required
     */
    protected String targetProperty;

    /**
     * @parameter expression="${defaultValue}"
     */
    protected String defaultValue;

    /**
     * @parameter expression="${showIfTargetSet}"
     */
    protected boolean showIfTargetSet;

    public void execute() throws MojoExecutionException, MojoFailureException {
        String property = project.getProperties().getProperty(targetProperty);
        if (property == null) {
            property = System.getProperty(targetProperty);
            project.getProperties().setProperty(targetProperty, property);
        }
        if (showIfTargetSet || property == null) {
            System.out.print(prompt);
            if (defaultValue != null) {
                System.out.print(" (" + defaultValue + ")");
            }
            System.out.print(": ");
            try {
                String input = new BufferedReader(new InputStreamReader(System.in)).readLine();
                if ((input == null || input.length() == 0) && defaultValue != null) {
                    input = defaultValue;
                }
                project.getProperties().setProperty(targetProperty, input);
            } catch (IOException e) {
                throw new MojoExecutionException("Problem reading input", e);
            }
        }
    }
}
