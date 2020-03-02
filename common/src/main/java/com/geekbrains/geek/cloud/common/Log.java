package com.geekbrains.geek.cloud.common;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class Log {
    private static final Logger logger = Logger.getLogger("");

    public static Logger getLogger() throws IOException {

        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");

        Handler handler = new FileHandler("server/log.txt", true);
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return record.getLevel() + "\t" + record.getMessage() + "\t" + dateFormat.format(new Date()) + "\n";
            }
        });

        logger.addHandler(handler);
        return logger;
    }
}
