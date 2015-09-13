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
package guru.nidi.maven.tools;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.animal_sniffer.SignatureChecker;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

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

    public void backportFiles(File dir, String base) throws IOException {
        log.info("Backporting classes in " + dir.getAbsolutePath());
        doBackportFiles(dir, base);
    }

    private void doBackportFiles(File dir, String base) throws IOException {
        final File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    doBackportFiles(file, base);
                } else if (file.isFile() && file.getName().endsWith(".class")) {
                    final String filename = file.getAbsolutePath().substring(base.length() + 1);
                    RandomAccessFile raf = null;
                    boolean converted = false;
                    try {
                        raf = new RandomAccessFile(file, "rw");
                        raf.seek(6);
                        final short major = raf.readShort();
                        if (major > 0x33) {
                            throw new IllegalStateException(filename + " has a version > 7. Cannot be converted.");
                        } else if (major == 0x33) {
                            raf.seek(6);
                            raf.writeShort(0x32);
                            log.info(filename + " converted.");
                            converted = true;
                        }
                    } finally {
                        if (raf != null) {
                            raf.close();
                        }
                    }
                    if (converted) {
                        checker.process(file);
                    }
                }
            }
        }
//        if (checker.isSignatureBroken()) {
//            throw new IllegalStateException("File contains illegal signature.");
//        }
    }

    public void backportJar(File jar) throws IOException {
        final File target = targetFile(jar);
        log.info("Backporting " + jar.getAbsolutePath() + " to " + target.getAbsolutePath());
        final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(target));
        final ZipFile in = new ZipFile(jar);
        final Enumeration<? extends ZipEntry> entries = in.entries();
        boolean converted = false;
        while (entries.hasMoreElements()) {
            final ZipEntry entry = entries.nextElement();
            out.putNextEntry(new ZipEntry(entry.getName()));
            converted |= convertCopy(in.getInputStream(entry), out, entry.getName());
            out.closeEntry();
        }
        in.close();
        out.close();
        if (!converted) {
            log.info("No backport needed.");
            target.delete();
        }
    }

    private File targetFile(File jar) {
        final String targetName = jar.getName().substring(0, jar.getName().length() - 4);
        return new File(jar.getParentFile(), targetName + "-backported7to6.jar");
    }

    private boolean convertCopy(InputStream in, OutputStream out, String name) throws IOException {
        final byte[] buf = new byte[10000];
        int read;
        boolean first = true;
        boolean converted = false;
        while ((read = in.read(buf)) > 0) {
            if (name.endsWith(".class") && first) {
                if (buf[7] > 0x33) {
                    throw new IllegalStateException(name + " has a version > 7. Cannot be converted.");
                } else if (buf[7] == 0x33) {
                    buf[7] = 0x32;
                    log.info(name + " converted.");
                    converted = true;
                }
            }
            first = false;
            out.write(buf, 0, read);
        }
        in.close();
        return converted;
    }
}
