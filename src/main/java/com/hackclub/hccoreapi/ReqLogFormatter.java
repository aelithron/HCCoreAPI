package com.hackclub.hccoreapi;

import java.time.ZoneId;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class ReqLogFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        return String.format("[%1$tF %1$tT] %2$s%n", record.getInstant().atZone(ZoneId.of("UTC")), record.getMessage());
    }
}
