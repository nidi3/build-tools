package guru.nidi.maven.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @goal consolePasswordInput
 */
public class ConsolePasswordInputMojo extends AbstractInputMojo {

    protected String doReadInput() throws IOException {
        return (System.console() != null) ? consoleRead() : systemInRead();
    }

    private String consoleRead() {
        return new String(System.console().readPassword());
    }

    private String systemInRead() throws IOException {
        System.out.println();
        Deleter d = new Deleter();
        try {
            return new BufferedReader(new InputStreamReader(System.in)).readLine();
        } finally {
            d.end();
        }
    }

    private static class Deleter extends Thread {
        private volatile boolean run = true;

        public Deleter() {
            setDaemon(true);
            start();
        }

        public void end() {
            run = false;
        }

        @Override
        public void run() {
            while (run) {
                System.out.print((char) 13);
                System.out.print('*');
                System.out.print((char) 13);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
            }
        }
    }
}