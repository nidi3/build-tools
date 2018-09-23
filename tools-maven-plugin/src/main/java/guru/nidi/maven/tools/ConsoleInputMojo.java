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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;

@Mojo(name = "consoleInput")
public class ConsoleInputMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = "input.prompt", required = true)
    private String prompt;

    @Parameter(property = "input.targetProperty", required = true)
    private String targetProperty;

    @Parameter(property = "input.defaultValue")
    protected String defaultValue;

    @Parameter(property = "input.showIfTargetSet")
    private boolean showIfTargetSet;

    @Parameter(property = "input.showInput")
    private boolean showInput = true;

    public void execute() throws MojoExecutionException {
        String property = findProperty();
        if (showIfTargetSet || property == null) {
            showPrompt();
            setProjectProperty(readInput());
        }
    }

    private String findProperty() {
        String property = project.getProperties().getProperty(targetProperty);
        if (property == null) {
            property = System.getProperty(targetProperty);
            setProjectProperty(property);
        }
        return property;
    }

    private void setProjectProperty(String value) {
        if (value != null) {
            project.getProperties().setProperty(targetProperty, value);
        }
    }

    private void showPrompt() {
        System.out.print(prompt);
        if (defaultValue != null) {
            System.out.print(" (" + defaultValue + ")");
        }
        System.out.print(": ");
    }

    protected String readInput() throws MojoExecutionException {
        try {
            final InputReader inputReader = (showInput ? new DefaultInputReader() : new HiddenInputReader());
            final String input = inputReader.readInput();
            if ((input == null || input.length() == 0) && defaultValue != null) {
                return defaultValue;
            }
            return input;
        } catch (IOException e) {
            throw new MojoExecutionException("Problem reading input", e);
        }
    }
}