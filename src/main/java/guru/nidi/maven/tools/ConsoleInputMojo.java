package guru.nidi.maven.tools;

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
    private MavenProject project;

    /**
     * @parameter expression="${prompt}"
     * @required
     */
    private String prompt;

    /**
     * @parameter expression="${targetProperty}"
     * @required
     */
    private String targetProperty;

    /**
     * @parameter expression="${defaultValue}"
     */
    private String defaultValue;

    /**
     * @parameter expression="${showIfTargetSet}"
     */
    private boolean showIfTargetSet;

    public void execute() throws MojoExecutionException, MojoFailureException {
        String property = findProperty();
        if (showIfTargetSet || property == null) {
            showPrompt();
            setProjectProperty(readInput());
        }
    }

    private String findProperty() {
        String property = project.getProperties().getProperty(targetProperty);
        if (property == null) {
            property = System.getProperty(targetProperty);
            setProjectProperty(property);
        }
        return property;
    }

    private void setProjectProperty(String value) {
        if (value != null) {
            project.getProperties().setProperty(targetProperty, value);
        }
    }

    private void showPrompt() {
        System.out.print(prompt);
        if (defaultValue != null) {
            System.out.print(" (" + defaultValue + ")");
        }
        System.out.print(": ");
    }

    private String readInput() throws MojoExecutionException {
        try {
            String input = new BufferedReader(new InputStreamReader(System.in)).readLine();
            if ((input == null || input.length() == 0) && defaultValue != null) {
                return defaultValue;
            }
            return input;
        } catch (IOException e) {
            throw new MojoExecutionException("Problem reading input", e);
        }
    }
}