package cn.nulladev.modularmagic.spell;

import org.apache.logging.log4j.*;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;

import java.util.ArrayList;
import java.util.List;

/** A per-compilation diagnostic sink; no global logger changes or shared mutable capture. */
final class ValidationLog extends ExtendedLoggerWrapper {
    private final List<String> errors = new ArrayList<>();

    ValidationLog() {
        super((ExtendedLogger) LogManager.getLogger("modularmagic.validation"), "modularmagic.validation", null);
    }

    List<String> errors() { return List.copyOf(errors); }

    @Override public Level getLevel() { return Level.ALL; }
    @Override public boolean isEnabled(Level l, Marker m, String text) { return true; }
    @Override public boolean isEnabled(Level l, Marker m, String text, Throwable t) { return true; }
    @Override public boolean isEnabled(Level l, Marker m, String text, Object... args) { return true; }
    @Override public boolean isEnabled(Level l, Marker m, CharSequence text, Throwable t) { return true; }
    @Override public boolean isEnabled(Level l, Marker m, Object text, Throwable t) { return true; }
    @Override public boolean isEnabled(Level l, Marker m, Message text, Throwable t) { return true; }

    @Override public void logMessage(String fqcn, Level level, Marker marker, Message message, Throwable throwable) {
        if (!level.isMoreSpecificThan(Level.ERROR) || errors.size() >= 24) return;
        String text = message == null ? "Invalid expression" : message.getFormattedMessage();
        if (throwable != null && throwable.getMessage() != null) text += ": " + throwable.getMessage();
        errors.add(text.substring(0, Math.min(text.length(), 2048)));
    }
}
