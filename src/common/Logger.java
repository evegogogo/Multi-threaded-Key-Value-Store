package common;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;

public class Logger {
    // Format the Logger Date in milliseconds precision.
    private java.util.logging.Logger logger;
    public Logger() {
        logger = java.util.logging.Logger.getLogger(Logger.class.getName());
//        try {
//            InputStream stream = Logger.class.getClassLoader().getResourceAsStream("resources/resource.properties");
//            LogManager.getLogManager().readConfiguration(stream);
//            logger = java.util.logging.Logger.getLogger(Logger.class.getName());
//        } catch (IOException e) {
//            logger = null;
//        }
    }

    public void log(Level currentLevel, String message) {
        if (logger != null) {
            logger.log(currentLevel, message);
        } else {
            System.out.println("Logger is not initialized.");
        }
    }
}

