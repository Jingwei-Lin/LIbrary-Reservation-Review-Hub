package com.comp5619.libraryreservationreviewhub.utils;

import java.util.regex.Pattern;

/**
 * SQL 注入防护工具类
 */
public class SqlInjectionUtil {

    // 仅匹配危险的 SQL 关键字或注释符号，而不误伤英文单词 update@example.com
    private static final Pattern SQL_PATTERN = Pattern.compile(
            "(?:')|(?:--)|(/\\*(?:.|[\\n\\r])*?\\*/)|" +
                    "\\b(select|insert|delete|drop|truncate|exec|union|alter|create|shutdown)\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * 检查输入是否含有 SQL 注入风险
     */
    public static void checkSqlInjection(String input, String fieldName) {
        if (input == null) return;
        if (SQL_PATTERN.matcher(input).find()) {
            throw new RuntimeException("Illegal input detected in field: " + fieldName);
        }
    }


}
