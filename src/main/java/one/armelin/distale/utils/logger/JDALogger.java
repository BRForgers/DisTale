package one.armelin.distale.utils.logger;

import one.armelin.distale.DisTale;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;

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

        switch (level) {
            case TRACE:
                DisTale.LOGGER.atFinest().log("[%s] %s", this.getClassOnlyName(), message);
                break;
            case DEBUG:
                DisTale.LOGGER.atFine().log("[%s] %s", this.getClassOnlyName(), message);
                break;
            case INFO:
                DisTale.LOGGER.atInfo().log("[%s] %s", this.getClassOnlyName(), message);
                break;
            case WARN:
                DisTale.LOGGER.atWarning().log("[%s] %s", this.getClassOnlyName(), message);
                break;
            case ERROR:
                if (throwable != null) {
                    DisTale.LOGGER.atSevere().withCause(throwable).log("[%s] %s", this.getClassOnlyName(), message);
                } else {
                    DisTale.LOGGER.atSevere().log("[%s] %s", this.getClassOnlyName(), message);
                }
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
