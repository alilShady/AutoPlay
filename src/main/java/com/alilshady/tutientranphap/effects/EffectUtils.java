// src/main/java/com/alilshady/tutientranphap/effects/EffectUtils.java
package com.alilshady.tutientranphap.effects;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EffectUtils {

    private EffectUtils() {
        // Private constructor to prevent instantiation
    }

    public static String getStringFromConfig(Map<?, ?> config, String key, String defaultValue) {
        Object value = config.get(key);
        return (value instanceof String) ? (String) value : defaultValue;
    }

    public static int getIntFromConfig(Map<?, ?> config, String key, int defaultValue) {
        Object value = config.get(key);
        return (value instanceof Number) ? ((Number) value).intValue() : defaultValue;
    }

    public static double getDoubleFromConfig(Map<?, ?> config, String key, double defaultValue) {
        Object value = config.get(key);
        return (value instanceof Number) ? ((Number) value).doubleValue() : defaultValue;
    }

    public static boolean getBooleanFromConfig(Map<?, ?> config, String key, boolean defaultValue) {
        Object value = config.get(key);
        return (value instanceof Boolean) ? (Boolean) value : defaultValue;
    }

    public static long parseDurationToTicks(String durationStr) {
        if (durationStr == null || durationStr.isEmpty()) return 0;
        try {
            Pattern pattern = Pattern.compile("(\\d+)([tsmh]?)");
            Matcher matcher = pattern.matcher(durationStr.toLowerCase());
            if (matcher.matches()) {
                long value = Long.parseLong(matcher.group(1));
                String unit = matcher.group(2);
                switch (unit) {
                    case "s": return value * 20;
                    case "m": return value * 20 * 60;
                    case "h": return value * 20 * 60 * 60;
                    case "t": default: return value;
                }
            }
            return Long.parseLong(durationStr);
        } catch (NumberFormatException e) {
            // It's better to log this in the calling context if needed
            return 0;
        }
    }
}