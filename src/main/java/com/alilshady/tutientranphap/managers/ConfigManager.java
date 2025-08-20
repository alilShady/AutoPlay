package com.alilshady.tutientranphap.managers;

import com.alilshady.tutientranphap.EssenceArrays;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ConfigManager {

    private final EssenceArrays plugin;
    private FileConfiguration formationConfig;
    private FileConfiguration mainConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration customEffectsConfig;

    private boolean debugLogging;
    private int effectCheckInterval;
    private boolean blueprintRequiresMaterials; // <-- BIẾN MỚI

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, Map<String, Object>> customEffects = new HashMap<>();

    public ConfigManager(EssenceArrays plugin) {
        this.plugin = plugin;
        reloadConfigs();
    }

    public void reloadConfigs() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        mainConfig = plugin.getConfig();
        debugLogging = mainConfig.getBoolean("debug-logging", true);
        effectCheckInterval = mainConfig.getInt("effect-check-interval-ticks", 20);
        blueprintRequiresMaterials = mainConfig.getBoolean("blueprint.require-materials", true); // <-- DÒNG MỚI

        File formationFile = new File(plugin.getDataFolder(), "formations.yml");
        if (!formationFile.exists()) {
            plugin.saveResource("formations.yml", false);
        }
        formationConfig = YamlConfiguration.loadConfiguration(formationFile);

        String lang = mainConfig.getString("language", "en").toLowerCase();
        String langFileName = lang + ".yml";
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        File messagesFile = new File(langDir, langFileName);
        plugin.saveResource("lang/" + langFileName, false);
        if (!messagesFile.exists()) {
            plugin.getLogger().warning("Language file '" + langFileName + "' not found. Defaulting to 'en'.");
            langFileName = "en.yml";
            messagesFile = new File(langDir, langFileName);
            plugin.saveResource("lang/en.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        InputStream defMessagesStream = plugin.getResource("lang/" + langFileName);
        if (defMessagesStream != null) {
            Reader defMessagesReader = new InputStreamReader(defMessagesStream, StandardCharsets.UTF_8);
            YamlConfiguration defMessages = YamlConfiguration.loadConfiguration(defMessagesReader);
            messagesConfig.setDefaults(defMessages);
        }

        File customEffectsFile = new File(plugin.getDataFolder(), "custom_effects.yml");
        if (!customEffectsFile.exists()) {
            plugin.saveResource("custom_effects.yml", false);
        }
        customEffectsConfig = YamlConfiguration.loadConfiguration(customEffectsFile);
        loadCustomEffects();
    }

    private void loadCustomEffects() {
        customEffects.clear();
        if (customEffectsConfig == null) return;

        ConfigurationSection section = customEffectsConfig.getConfigurationSection("");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                Map<String, Object> effectData = section.getConfigurationSection(id).getValues(true);
                customEffects.put(id, effectData);
            }
        }
        if (debugLogging) {
            plugin.getLogger().info("Loaded " + customEffects.size() + " custom base effects.");
        }
    }

    public Map<String, Object> getCustomEffect(String id) {
        return customEffects.get(id);
    }

    public String getMessage(String path, String... replacements) {
        String messageTemplate = messagesConfig.getString(path, "<red>Missing message: " + path);

        if (messageTemplate != null && !messageTemplate.isEmpty()) {
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

    // --- HÀM GETTER MỚI ---
    public boolean isBlueprintRequiresMaterials() {
        return blueprintRequiresMaterials;
    }

    public int getEffectCheckInterval() {
        return effectCheckInterval;
    }

    public MiniMessage getMiniMessage() {
        return this.miniMessage;
    }
}