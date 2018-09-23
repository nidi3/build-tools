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
package guru.nidi.tools.dependency;

import org.apache.maven.artifact.Artifact;

class ArtifactMatcher {
    private final String filter;

    ArtifactMatcher(String filter) {
        this.filter = filter;
    }

    boolean matches(Artifact artifact) {
        if (filter == null) {
            return false;
        }
        final String[] parts = filter.split(":");
        if (parts.length > 0 && !artifact.getGroupId().matches(parts[0])) {
            return false;
        }
        if (parts.length > 1 && !artifact.getArtifactId().matches(parts[1])) {
            return false;
        }
        if (parts.length > 2 && !artifact.getType().matches(parts[2])) {
            return false;
        }
        if (parts.length > 3 && !artifact.getVersion().matches(parts[3])) {
            return false;
        }
        return true;
    }

}
