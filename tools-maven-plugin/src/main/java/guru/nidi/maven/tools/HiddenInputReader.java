/*
 * Copyright © 2014 Stefan Niederhauser (nidin@gmx.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package guru.nidi.maven.tools;

import java.io.*;

class HiddenInputReader implements InputReader {

    public String readInput() throws IOException {
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
                    //ignore
                }
            }
        }
    }
}