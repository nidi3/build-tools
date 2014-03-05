package guru.nidi.maven.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @goal consoleInput
 */
public class ConsoleInputMojo extends AbstractInputMojo {
    protected String doReadInput() throws IOException {
        return new BufferedReader(new InputStreamReader(System.in)).readLine();
    }
}