package one.armelin.distale.utils.logger;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LoggerFactory implements ILoggerFactory {
    private final ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<>();

    @Override
    public Logger getLogger(String name) {
        return loggers.computeIfAbsent(name, JDALogger::new);
    }
}