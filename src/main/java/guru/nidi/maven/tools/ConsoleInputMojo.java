package guru.nidi.maven.tools;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.IOException;

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
    protected String defaultValue;

    /**
     * @parameter expression="${showIfTargetSet}"
     */
    private boolean showIfTargetSet;

    /**
     * @parameter expression="${showInput}"
     */
    private boolean showInput = true;

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

    protected String readInput() throws MojoExecutionException {
        try {
            final InputReader inputReader = (showInput ? new DefaultInputReader() : new HiddenInputReader());
            final String input = inputReader.readInput();
            if ((input == null || input.length() == 0) && defaultValue != null) {
                return defaultValue;
            }
            return input;
        } catch (IOException e) {
            throw new MojoExecutionException("Problem reading input", e);
        }
    }
}