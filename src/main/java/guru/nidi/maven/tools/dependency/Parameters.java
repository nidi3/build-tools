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
package guru.nidi.maven.tools.dependency;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

/**
 *
 */
public class Parameters implements ArtifactFilter {
    private final boolean simple;
    private final int maxDepth;
    private final boolean optional;
    private final String scopes;
    private final String excludes;
    private final String includes;

    public Parameters(boolean simple, int maxDepth, boolean optional, String scopes, String excludes, String includes) {
        this.simple = simple;
        this.maxDepth = maxDepth;
        this.optional = optional;
        this.scopes = scopes;
        this.excludes = excludes;
        this.includes = includes;
    }

    public boolean isSimple() {
        return simple;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public boolean isInScope(String s) {
        return scopes == null || scopes.contains(s);
    }

    @Override
    public boolean include(Artifact artifact) {
        if (!optional && artifact.isOptional()) {
            return false;
        }
        if (artifact.getScope() != null && (scopes != null && !scopes.contains(artifact.getScope()))) {
            return false;
        }
        if (includes != null && !new ArtifactMatcher(includes).matches(artifact)) {
            return false;
        }
        if (new ArtifactMatcher(excludes).matches(artifact)) {
            return false;
        }
        return true;
    }
}
