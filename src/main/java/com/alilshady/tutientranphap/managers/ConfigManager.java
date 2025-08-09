package com.alilshady.tutientranphap.managers;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigManager {

    private final TuTienTranPhap plugin;
    private FileConfiguration formationConfig;
    private File formationFile;

    public ConfigManager(TuTienTranPhap plugin) {
        this.plugin = plugin;
        setupFormationConfig();
    }

    private void setupFormationConfig() {
        formationFile = new File(plugin.getDataFolder(), "formations.yml");
        if (!formationFile.exists()) {
            plugin.saveResource("formations.yml", false);
        }
        formationConfig = YamlConfiguration.loadConfiguration(formationFile);
    }

    public List<Formation> loadFormations() {
        List<Formation> formations = new ArrayList<>();
        ConfigurationSection formationSection = formationConfig.getConfigurationSection("");
        if (formationSection == null) return formations;

        for (String id : formationSection.getKeys(false)) {
            try {
                String path = id + ".";
                String displayName = ChatColor.translateAlternateColorCodes('&', formationConfig.getString(path + "display_name"));
                Material activationItem = Material.valueOf(formationConfig.getString(path + "activation_item").toUpperCase());
                int duration = formationConfig.getInt(path + "duration_seconds");
                int radius = formationConfig.getInt(path + "radius");

                // Đọc pattern
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
                plugin.getLogger().info("Loaded formation: " + displayName);

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load formation with id '" + id + "'. Please check the configuration.");
                e.printStackTrace();
            }
        }
        return formations;
    }
}