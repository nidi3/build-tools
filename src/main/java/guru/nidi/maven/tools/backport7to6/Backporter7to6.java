/*
 * Copyright (C) 2014 Stefan Niederhauser (nidin@gmx.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package guru.nidi.maven.tools.backport7to6;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.animal_sniffer.SignatureChecker;

import java.io.File;
import java.io.IOException;

/**
 *
 */
public class Backporter7to6 {
    private final SignatureChecker checker;
    private final Log log;

    public Backporter7to6(SignatureChecker checker, Log log) {
        this.checker = checker;
        this.log = log;
    }

    public boolean backportFiles(File dir, String base) throws IOException {
        log.info("Backporting classes in " + dir.getAbsolutePath());
        final boolean converted = doBackportFiles(dir, base);
        if (checker.isSignatureBroken()) {
            throw new IllegalStateException("There are illegal signatures.");
        }
        return converted;
    }

    private boolean doBackportFiles(File dir, String base) throws IOException {
        final File[] files = dir.listFiles();
        boolean converted = false;
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    converted |= doBackportFiles(file, base);
                } else {
                    converted |= backportFile(file, base);
                }
            }
        }
        return converted;
    }

    private boolean backportFile(File file, String base) throws IOException {
        if (file.isFile() && file.getName().endsWith(".class")) {
            final String filename = file.getAbsolutePath().substring(base.length() + 1);
            if (Transform7to6.transform(file, filename)) {
                log.info(filename + " converted.");
                checker.process(file);
                return true;
            }
        }
        return false;
    }
}
