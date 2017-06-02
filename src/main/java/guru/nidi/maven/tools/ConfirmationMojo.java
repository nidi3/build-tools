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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Mojo(name = "confirmation")
public class ConfirmationMojo extends AbstractMojo {
    @Parameter(property = "confirmation.prompt", required = true)
    private String prompt;

    public void execute() throws MojoExecutionException, MojoFailureException {
        System.out.print(prompt);
        String in = readInput();
        if (!in.equalsIgnoreCase("y") && !in.equalsIgnoreCase("yes")) {
            throw new MojoExecutionException("User did not confirm question '" + prompt + "'");
        }
    }

    private String readInput() throws MojoExecutionException {
        try {
            return new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (IOException e) {
            throw new MojoExecutionException("Problem reading input", e);
        }
    }
}