package guru.nidi.maven.tools;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @goal confirmation
 */
public class ConfirmationMojo extends AbstractMojo {
    /**
     * @parameter expression="${prompt}"
     * @required
     */
    private String prompt;

    public void execute() throws MojoExecutionException, MojoFailureException {
        System.out.print(prompt);
        String in = readInput();
        if (!in.equalsIgnoreCase("y") && !in.equalsIgnoreCase("yes")) {
            throw new MojoExecutionException("User did not confirm question '" + prompt + "'");
        }
    }

    private String readInput() throws MojoExecutionException {
        try {
            return new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (IOException e) {
            throw new MojoExecutionException("Problem reading input", e);
        }
    }
}