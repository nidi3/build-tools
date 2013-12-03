package stni.maven.tools;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.slf4j.LoggerFactory;

/**
 *
 */
class LogConfiguration {
    private LogConfiguration() {
    }

    public static void useLogConfig(String name) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
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
