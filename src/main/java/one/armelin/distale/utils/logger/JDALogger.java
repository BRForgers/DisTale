package one.armelin.distale.utils.logger;

import com.hypixel.hytale.logger.HytaleLogger;
import one.armelin.distale.DisTale;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;

import java.util.Objects;

public class JDALogger extends LegacyAbstractLogger {

    public JDALogger(String name) {
        this.name = name;
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return name;
    }

    @Override
    protected void handleNormalizedLoggingCall(
            Level level,
            Marker marker,
            String messagePattern,
            Object[] arguments,
            Throwable throwable) {

        String message = MessageFormatter.arrayFormat(messagePattern, arguments).getMessage();
        HytaleLogger logger = HytaleLogger.get(DisTale.NAME + "|JDA");
        if(!Objects.equals(this.getClassOnlyName(), "JDA")){
            logger = logger.getSubLogger(this.getClassOnlyName());
        }

        switch (level) {
            case TRACE:
                logger.atFinest().log(message);
                break;
            case DEBUG:
                logger.atFine().log(message);
                break;
            case INFO:
                logger.atInfo().log(message);
                break;
            case WARN:
                logger.atWarning().log(message);
                break;
            case ERROR:
                var logBuilder = logger.atSevere();
                if (throwable != null) {
                    logBuilder.withCause(throwable).log(message);
                }
                logBuilder.log(message);
                break;
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    public String getClassOnlyName() {
        String className = this.getName();
        if (className.contains(".")) {
            return className.substring(className.lastIndexOf(".") + 1);
        }
        return className;
    }
}
