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
package guru.nidi.maven.tools.dependency;

import java.io.*;

import static guru.nidi.maven.tools.dependency.IoUtils.fileEnding;

public class HtmlCreator {
    private final File inputDir;
    private final File outputDir;
    private final boolean clientSide;

    public HtmlCreator(File inputDir, File outputDir, boolean clientSide) {
        this.inputDir = inputDir;
        this.outputDir = outputDir;
        this.clientSide = clientSide;
    }

    public void createHtmls(File[] files) throws IOException {
        for (final File f : files) {
            createHtml(f);
        }
    }

    private void createHtml(File f) throws IOException {
        final File output = fileEnding(f, outputDir, ".html");
        if (!output.exists()) {
            final FileOutputStream fos = new FileOutputStream(output);
            final PrintWriter out = new PrintWriter(new OutputStreamWriter(fos, "utf-8"));
            out.println("<html><body>");
            if (clientSide) {
                out.println("<script src='http://mdaines.github.io/viz.js/viz.js'></script>");
                out.print("<script>document.body.innerHTML += Viz('");
                out.flush();
                final BufferedReader svg = new BufferedReader(new InputStreamReader(new FileInputStream(fileEnding(f, inputDir, ".dot")), "utf-8"));
                String line;
                while ((line = svg.readLine()) != null) {
                    out.write(line.replace("'", "\\'"));
                }
                svg.close();
                out.println("', {format: 'svg'});</script>");
            } else {
                out.flush();
                final FileInputStream map = new FileInputStream(fileEnding(f, inputDir, ".map"));
                IoUtils.copy(map, fos);
                map.close();
                out.println("<img src='./" + fileEnding(f, ".png") + "' usemap='#" + fileEnding(f, "").replace('$', ':') + "'></img>");
            }
            out.println("</body></html>");
            out.close();
        }
    }

}
