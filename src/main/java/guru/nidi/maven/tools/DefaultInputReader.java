package guru.nidi.maven.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 */
public class DefaultInputReader implements InputReader {
    @Override
    public String readInput() throws IOException {
        return new BufferedReader(new InputStreamReader(System.in)).readLine();
    }
}