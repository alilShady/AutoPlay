package com.alilshady.tutientranphap.managers;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ConfigManager {

    private final TuTienTranPhap plugin;
    private FileConfiguration formationConfig;
    private FileConfiguration mainConfig;

    // Các giá trị config được cache lại
    private boolean debugLogging;
    private int effectCheckInterval;

    public ConfigManager(TuTienTranPhap plugin) {
        this.plugin = plugin;
    }

    public void reloadConfigs() {
        // Tải config.yml
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        mainConfig = plugin.getConfig();
        debugLogging = mainConfig.getBoolean("debug-logging", true);
        effectCheckInterval = mainConfig.getInt("effect-check-interval-ticks", 20);

        // Chuẩn bị formations.yml
        File formationFile = new File(plugin.getDataFolder(), "formations.yml");
        if (!formationFile.exists()) {
            plugin.saveResource("formations.yml", false);
        }
        formationConfig = YamlConfiguration.loadConfiguration(formationFile);
    }

    public CompletableFuture<List<Formation>> loadFormationsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<Formation> formations = new ArrayList<>();
            // Phải lấy lại section trong thread async vì config có thể đã được reload
            ConfigurationSection formationSection = formationConfig.getConfigurationSection("");
            if (formationSection == null) return formations;

            for (String id : formationSection.getKeys(false)) {
                try {
                    String path = id + ".";
                    String displayName = ChatColor.translateAlternateColorCodes('&', formationConfig.getString(path + "display_name", id));
                    Material activationItem = Material.valueOf(formationConfig.getString(path + "activation_item", "STONE").toUpperCase());
                    int duration = formationConfig.getInt(path + "duration_seconds");
                    int radius = formationConfig.getInt(path + "radius");

                    Map<Character, Material> keyMap = new HashMap<>();
                    ConfigurationSection keySection = formationConfig.getConfigurationSection(path + "pattern.key");
                    if (keySection != null) {
                        for (String keyChar : keySection.getKeys(false)) {
                            keyMap.put(keyChar.charAt(0), Material.valueOf(keySection.getString(keyChar).toUpperCase()));
                        }
                    }

                    List<String> shape = formationConfig.getStringList(path + "pattern.shape");
                    List<Map<?, ?>> effects = formationConfig.getMapList(path + "effects");

                    formations.add(new Formation(id, displayName, activationItem, duration, radius, keyMap, shape, effects));
                    if (debugLogging) {
                        // Log phải được thực hiện trên thread chính để đảm bảo an toàn
                        Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger().info("Loaded formation: " + displayName));
                    }

                } catch (Exception e) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getLogger().warning("Failed to load formation with id '" + id + "'. Please check the configuration.");
                        e.printStackTrace();
                    });
                }
            }
            return formations;
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
    }


    // Getters cho các giá trị đã cache
    public boolean isDebugLoggingEnabled() {
        return debugLogging;
    }

    public int getEffectCheckInterval() {
        return effectCheckInterval;
    }
}