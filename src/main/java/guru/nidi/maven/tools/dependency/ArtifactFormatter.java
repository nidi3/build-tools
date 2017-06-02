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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ArtifactFormatter {
    private final Map<String, String> formatted = new HashMap<String, String>();
    private final String formats;
    private final MavenContext mavenContext;

    public ArtifactFormatter(String formats, MavenContext mavenContext) {
        this.formats = formats;
        this.mavenContext = mavenContext;
    }

    public String toString(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() +
                ("jar" .equals(artifact.getType()) ? "" : (":" + artifact.getType())) +
                (empty(artifact.getClassifier()) ? "" : (":" + artifact.getClassifier()));
    }

    public String quoted(Artifact artifact) {
        return "\"" + toString(artifact) + "\"";
    }

    public String label(Artifact artifact) {
        final String key = toString(artifact);
        String res = formatted.get(key);
        if (res == null) {
            res = calcLabel(artifact);
            formatted.put(key, res);
        }
        return res;
    }

    public String filenameFor(Artifact artifact, String suffix) {
        return toString(artifact).replace(":", "$") + suffix;
    }

    private boolean empty(String s) {
        return s == null || s.length() == 0;
    }

    private String calcLabel(Artifact artifact) {
        for (final String format : formats.split(",")) {
            final String[] f = format.split("->");
            if (new ArtifactMatcher(f[0]).matches(artifact)) {
                return "<" + format(artifact, f[1]) + ">";
            }
        }
        return quoted(artifact);
    }

    private String format(Artifact artifact, String format) {
        final String s = format
                .replace("%g", artifact.getGroupId())
                .replace("%a", artifact.getArtifactId())
                .replace("%t", artifact.getType())
                .replace("%v", artifact.getVersion())
                .replace("%n", "<br/>");
        final Matcher m = Pattern.compile("%(\\d*)d(s?)").matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            try {
                final MavenProject project = mavenContext.projectFromArtifact(artifact);
                String desc = project == null || project.getDescription() == null
                        ? ""
                        : project.getDescription();
                if (m.group(1).length() > 0) {
                    desc = linebreak(desc, Integer.parseInt(m.group(1)));
                }
                final int pos = find(desc, "\\.\\s");
                final String shortDesc = pos < 0 ? desc : desc.substring(0, pos + 1);
                m.appendReplacement(sb, m.group(2).length() == 0 ? desc : shortDesc);
            } catch (ProjectBuildingException e) {
                //ignore
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private int find(String s, String toFind) {
        final Matcher matcher = Pattern.compile(toFind).matcher(s);
        return matcher.find() ? matcher.start() : -1;
    }

    private String linebreak(String s, int minLen) {
        final StringBuilder sb = new StringBuilder();
        int start = 0, end;
        while (true) {
            end = s.indexOf(" ", start + minLen);
            if (end < 0) {
                sb.append(s.substring(start));
                break;
            }
            sb.append(s.substring(start, end)).append("<br/>");
            start = end + 1;
        }
        return sb.toString();
    }

}
