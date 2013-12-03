package stni.maven.tools;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal setProperty
 */
public class SetPropertyMojo extends AbstractMojo {
    /**
     * @parameter expression="${properties}"
     * @required
     */
    private String properties;

    public void execute() throws MojoExecutionException, MojoFailureException {
        String[] split = properties.split(",");
        for (String s : split) {
            String[] parts = s.split("=");
            getLog().info("Setting Property " + parts[0] + "=" + parts[1]);
            System.setProperty(parts[0], parts[1]);
        }
    }
}
