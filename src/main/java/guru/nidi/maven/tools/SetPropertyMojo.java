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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "setProperty")
public class SetPropertyMojo extends AbstractMojo {
    @Parameter(property = "set.properties", required = true)
    private String properties;

    public void execute() throws MojoExecutionException, MojoFailureException {
        String[] split = properties.split(",");
        for (String s : split) {
            String[] parts = s.split("=");
            getLog().info("Setting Property " + parts[0] + "=" + parts[1]);
            System.setProperty(parts[0], parts[1]);
        }
    }
}
