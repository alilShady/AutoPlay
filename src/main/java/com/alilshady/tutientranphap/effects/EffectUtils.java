package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.EssenceArrays;
import org.bukkit.GameMode;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
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
            return 0;
        }
    }

    /**
     * Determines if an effect should be applied to a specific entity.
     * @param plugin The main plugin instance.
     * @param entity The entity being checked.
     * @param targetType The target type string from the config.
     * @param ownerId The UUID of the formation's owner.
     * @return true if the effect should be applied, false otherwise.
     */
    public static boolean shouldApplyToEntity(EssenceArrays plugin, LivingEntity entity, String targetType, UUID ownerId) {
        // Bỏ qua người chơi ở chế độ Creative hoặc Spectator (trừ một số hiệu ứng).
        if (entity instanceof Player && ((Player) entity).getGameMode() != GameMode.SURVIVAL) {
            return false;
        }

        Player owner = (ownerId != null) ? plugin.getServer().getPlayer(ownerId) : null;

        switch (targetType) {
            case "OWNER":
                return owner != null && entity.getUniqueId().equals(owner.getUniqueId());
            case "ALL":
                return true;
            case "MOBS":
                return entity instanceof Monster;
            case "DAMAGEABLE":
                if (entity instanceof Monster) {
                    return true;
                }
                if (entity instanceof Player && owner != null) {
                    return !plugin.getTeamManager().isAlly(owner, (Player) entity);
                }
                return false;
            case "UNDAMAGEABLE":
                if (entity instanceof Animals) {
                    return true;
                }
                if (entity instanceof Player && owner != null) {
                    return plugin.getTeamManager().isAlly(owner, (Player) entity);
                }
                return false;
            default:
                return false;
        }
    }
}