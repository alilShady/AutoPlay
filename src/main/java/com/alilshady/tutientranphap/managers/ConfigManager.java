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
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ConfigManager {

    private final TuTienTranPhap plugin;
    private FileConfiguration formationConfig;
    private FileConfiguration mainConfig;
    private FileConfiguration messagesConfig;

    private boolean debugLogging;
    private int effectCheckInterval;

    public ConfigManager(TuTienTranPhap plugin) {
        this.plugin = plugin;
    }

    public void reloadConfigs() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        mainConfig = plugin.getConfig();
        debugLogging = mainConfig.getBoolean("debug-logging", true);
        effectCheckInterval = mainConfig.getInt("effect-check-interval-ticks", 20);

        File formationFile = new File(plugin.getDataFolder(), "formations.yml");
        if (!formationFile.exists()) {
            plugin.saveResource("formations.yml", false);
        }
        formationConfig = YamlConfiguration.loadConfiguration(formationFile);

        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        Reader defMessagesStream = new InputStreamReader(plugin.getResource("messages.yml"), StandardCharsets.UTF_8);
        if (defMessagesStream != null) {
            YamlConfiguration defMessages = YamlConfiguration.loadConfiguration(defMessagesStream);
            messagesConfig.setDefaults(defMessages);
        }
    }

    public String getMessage(String path, String... replacements) {
        String message = messagesConfig.getString(path, "&cMissing message: " + path);

        if (!message.isEmpty()) {
            String prefix = messagesConfig.getString("prefix", "");
            message = prefix + message;
        }

        if (replacements.length > 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                if (i + 1 < replacements.length) {
                    message = message.replace(replacements[i], replacements[i + 1]);
                }
            }
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String path) {
        return getMessage(path, new String[0]);
    }


    public CompletableFuture<List<Formation>> loadFormationsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<Formation> formations = new ArrayList<>();
            ConfigurationSection formationSection = formationConfig.getConfigurationSection("");
            if (formationSection == null) return formations;

            for (String id : formationSection.getKeys(false)) {
                try {
                    String path = id + ".";
                    String displayName = ChatColor.translateAlternateColorCodes('&', formationSection.getString(path + "display_name", id));
                    Material activationItem = Material.valueOf(formationSection.getString(path + "activation_item", "STONE").toUpperCase());

                    String duration = formationSection.getString(path + "duration", "30m");
                    int radius = formationSection.getInt(path + "radius");

                    Map<Character, Material> keyMap = new HashMap<>();
                    ConfigurationSection keySection = formationSection.getConfigurationSection(path + "pattern.key");
                    if (keySection != null) {
                        for (String keyChar : keySection.getKeys(false)) {
                            keyMap.put(keyChar.charAt(0), Material.valueOf(keySection.getString(keyChar).toUpperCase()));
                        }
                    }

                    List<String> shape = formationSection.getStringList(path + "pattern.shape");
                    List<Map<?, ?>> effects = formationSection.getMapList(path + "effects");

                    Map<String, Object> particleConfig = new HashMap<>();
                    ConfigurationSection particleSection = formationSection.getConfigurationSection(path + "particles");
                    if (particleSection != null) {
                        for (String key : particleSection.getKeys(false)) {
                            particleConfig.put(key, particleSection.get(key));
                        }
                    }

                    formations.add(new Formation(id, displayName, activationItem, duration, radius, keyMap, shape, effects, particleConfig));
                    if (debugLogging) {
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


    public boolean isDebugLoggingEnabled() {
        return debugLogging;
    }

    public int getEffectCheckInterval() {
        return effectCheckInterval;
    }
}