package dev.chrona.common.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.MarkerFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;

import java.nio.file.Files;
import java.nio.file.Path;

public final class LoggingBootstrap {

    public static void init(Path pluginDir, boolean dev) {
        var ctx = (LoggerContext) LogManager.getContext(false);
        Configuration cfg = ctx.getConfiguration();

        Path logDir = pluginDir.resolve("logs");
        try {
            Files.createDirectories(logDir);
        } catch (Exception ignore) {}

        var pattern = dev
                ? "%d{HH:mm:ss.SSS} %-5level [%logger{1}] [%X{player.name}/%X{world}/%X{corr}] %msg%n"
                : "%d{ISO8601} %-5level %marker [%X{player.id} %X{player.name} %X{world} %X{corr}] %msg%n";

        // Helper-Fabrik für Appender
        AppenderFactory add = (name, file, marker) -> {
            var layout = PatternLayout.newBuilder()
                    .withPattern(pattern)
                    .withConfiguration(cfg)
                    .build();

            var policy = CompositeTriggeringPolicy.createPolicy(
                    SizeBasedTriggeringPolicy.createPolicy("50MB"),
                    TimeBasedTriggeringPolicy.newBuilder().withInterval(1).build()
            );

            var strategy = DefaultRolloverStrategy.newBuilder()
                    .withMax("30")
                    .withConfig(cfg)
                    .build();

            var builder = RollingFileAppender.newBuilder()
                    .setConfiguration(cfg)
                    .withName(name)
                    .withFileName(logDir.resolve(file).toString())
                    .withFilePattern(logDir.resolve(file + ".%d{yyyy-MM-dd}.%i.gz").toString())
                    .withPolicy(policy)
                    .withStrategy(strategy)
                    .setLayout(layout);

            if (marker != null) {
                builder.setFilter(MarkerFilter.createFilter(marker, Filter.Result.NEUTRAL, Filter.Result.DENY));
            }

            var app = builder.build();
            app.start();
            cfg.addAppender(app);
            return app;
        };

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            var log = ChronaLog.get(LoggingBootstrap.class);
            log.error("Uncaught exception in thread {}: {}", thread.getName(), ex.getMessage(), ex);
        });

        var def   = add.run("chrona", "chrona.log", null);
        var econ  = add.run("economy", "economy.log", "ECON");
        var quest = add.run("quests", "quests.log", "QUEST");
        var event = add.run("events", "events.log", "EVENT");
        var audit = add.run("audit", "audit.log", "AUDIT");

        addRefs(cfg.getRootLogger(), def, econ, quest, event, audit);
        addRefs(cfg.getLoggerConfig("dev.chrona"), def, econ, quest, event, audit);

        ctx.updateLoggers();
    }

    @FunctionalInterface
    private interface AppenderFactory {
        Appender run(String name, String file, String marker);
    }

    private static void addRefs(LoggerConfig logger, Appender... apps) {
        for (var a : apps) {
            logger.addAppender(a, null, null);
        }
    }
}
