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

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.lang.reflect.Method;

@Mojo(name = "runMain", requiresDependencyResolution = ResolutionScope.TEST)
public class MainRunnerMojo extends AbstractRunnerMojo {
    @Parameter(property = "runner.mainClass", required = true)
    private String mainClass;

    @Parameter(property = "runner.arguments")
    private String arguments;

    @Override
    public void run() throws Throwable {
        final Class<?> main = Thread.currentThread().getContextClassLoader().loadClass(mainClass);
        final Method mainMethod = main.getMethod("main", String[].class);
        mainMethod.invoke(null, (Object) (arguments == null ? new String[0] : arguments.split(" ")));
    }
}
