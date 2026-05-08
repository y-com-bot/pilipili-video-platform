package com.yuan.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class AppLogger {
    private static final Logger logger = Logger.getLogger("platform");
    private static final Path LOG_DIR = resolveLogDir();
    private static final Path ERROR_LOG_FILE = LOG_DIR.resolve("app-error.log");

    static {
        try {
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.ALL);

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            consoleHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(consoleHandler);

            Files.createDirectories(LOG_DIR);
            FileHandler fileHandler = new FileHandler(ERROR_LOG_FILE.toString(), true);
            fileHandler.setLevel(Level.WARNING);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("创建日志文件失败: " + e.getMessage());
        }
    }

    public static Logger getLogger() {
        return logger;
    }

    public static Path getErrorLogFile() {
        return ERROR_LOG_FILE;
    }

    private static Path resolveLogDir() {
        String configuredDir = System.getProperty("app.log.dir");
        if (configuredDir != null && !configuredDir.trim().isEmpty()) {
            return Paths.get(configuredDir.trim()).toAbsolutePath().normalize();
        }
        return findProjectRoot().resolve("logs");
    }

    private static Path findProjectRoot() {
        Path userDir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        if (looksLikeProjectRoot(userDir)) {
            return userDir;
        }

        try {
            Path codeSource = Paths.get(AppLogger.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .toAbsolutePath()
                    .normalize();
            Path current = Files.isDirectory(codeSource) ? codeSource : codeSource.getParent();
            while (current != null) {
                if (looksLikeProjectRoot(current)) {
                    return current;
                }
                current = current.getParent();
            }
        } catch (URISyntaxException ignored) {
        }

        return userDir;
    }

    private static boolean looksLikeProjectRoot(Path path) {
        return Files.exists(path.resolve("pom.xml"))
                && Files.isDirectory(path.resolve("application"))
                && Files.isDirectory(path.resolve("framework"));
    }
}
