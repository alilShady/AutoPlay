package com.alilshady.tutientranphap.managers;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import com.alilshady.tutientranphap.utils.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

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

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ConfigManager(TuTienTranPhap plugin) {
        this.plugin = plugin;
        reloadConfigs();
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
        String messageTemplate = messagesConfig.getString(path, "<red>Missing message: " + path);

        if (!messageTemplate.isEmpty()) {
            String prefix = messagesConfig.getString("prefix", "");
            messageTemplate = prefix + messageTemplate;
        }

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                messageTemplate = messageTemplate.replace(replacements[i], replacements[i + 1]);
            }
        }

        Component component = miniMessage.deserialize(messageTemplate);
        return LegacyComponentSerializer.legacySection().serialize(component);
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
                if (id.equalsIgnoreCase("config-version")) continue;

                try {
                    String path = id + ".";
                    // Giữ nguyên displayName dưới dạng chuỗi MiniMessage gốc
                    String displayName = formationConfig.getString(path + "display_name", id);

                    ItemStack activationItem;
                    ConfigurationSection itemSection = formationConfig.getConfigurationSection(path + "activation_item");
                    if (itemSection != null) {
                        activationItem = ItemFactory.createFromConfig(itemSection);
                    } else {
                        Material material = Material.valueOf(formationConfig.getString(path + "activation_item", "STONE").toUpperCase());
                        activationItem = new ItemStack(material);
                    }

                    if (activationItem == null) {
                        plugin.getLogger().warning("Invalid activation_item for formation '" + id + "'. Skipping.");
                        continue;
                    }

                    String duration = formationConfig.getString(path + "duration", "30m");
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

                    Map<String, Object> particleConfig = new HashMap<>();
                    ConfigurationSection particleSection = formationConfig.getConfigurationSection(path + "particles");
                    if (particleSection != null) {
                        for (String key : particleSection.getKeys(false)) {
                            particleConfig.put(key, particleSection.get(key));
                        }
                    }

                    // Truyền trực tiếp chuỗi MiniMessage vào constructor, không chuyển đổi sang legacy
                    formations.add(new Formation(id, displayName, activationItem, duration, radius, keyMap, shape, effects, particleConfig));

                    if (debugLogging) {
                        String finalDisplayName = displayName;
                        Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger().info("Loaded formation: " + finalDisplayName));
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

    public MiniMessage getMiniMessage() {
        return this.miniMessage;
    }
}