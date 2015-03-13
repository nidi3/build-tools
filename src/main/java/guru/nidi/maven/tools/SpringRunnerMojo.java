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

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @goal runSpring
 */
public class SpringRunnerMojo extends AbstractRunnerMojo {
    private final static String APP_CONTEXT_CLASS = "org.springframework.context.support.FileSystemXmlApplicationContext";
    /**
     * @parameter expression="${contextFile}"
     * @required
     */
    private File contextFile;

    /**
     * @parameter expression="${profiles}"
     * @required
     */
    private String profiles;

    @Override
    public void run() throws Throwable {
        System.setProperty("spring.profiles.active", profiles);
        System.setProperty("basedir", project.getBasedir().getAbsolutePath());
        runSpringUsingReflection();
    }

    private void runSpringUsingReflection() throws Throwable {
        try {
            getLog().info("Starting spring context...");
            final Class<?> appContextClass = Thread.currentThread().getContextClassLoader().loadClass(APP_CONTEXT_CLASS);
            final Constructor<?> constructor = appContextClass.getConstructor(String.class);
            final Object appContext = constructor.newInstance("file:" + contextFile.getAbsolutePath());
            getLog().info("Started. Stopping spring context...");
            appContextClass.getMethod("stop").invoke(appContext);
            getLog().info("Stopped.");
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } catch (Exception e) {
            handleException("Problem starting spring. Assure that spring is in project's classpath and " +
                    "'constructor " + APP_CONTEXT_CLASS + "(String)' is existing.", e);
        }
    }
}
