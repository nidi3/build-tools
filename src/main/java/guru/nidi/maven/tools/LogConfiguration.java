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

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.JoranException;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

class LogConfiguration {
    private LogConfiguration() {
    }

    public static void useLogConfig(String name) {
        final ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        if (factory instanceof ContextBase) {
            ContextBase context = (ContextBase) factory;
            try {
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(context);
                context.reset();
                configurator.doConfigure(LogConfiguration.class.getClassLoader().getResourceAsStream(name));
            } catch (JoranException je) {
                throw new IllegalStateException(je);
            }
        }
    }
}
