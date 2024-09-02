package com.outsider.mop.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class Formatter {
    public static String formatJoinDate(JsonNode after, String key) {
        long joinDateEpoch = after.get(key).asLong();
        LocalDate joinDate = LocalDate.ofEpochDay(joinDateEpoch);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // 원하는 포맷 설정
        return joinDate.format(formatter);
    }
}
