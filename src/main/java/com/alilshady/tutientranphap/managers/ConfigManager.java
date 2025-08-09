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
    // Thêm config cho tin nhắn
    private FileConfiguration messagesConfig;

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

        // Chuẩn bị và tải formations.yml
        File formationFile = new File(plugin.getDataFolder(), "formations.yml");
        if (!formationFile.exists()) {
            plugin.saveResource("formations.yml", false);
        }
        formationConfig = YamlConfiguration.loadConfiguration(formationFile);

        // Chuẩn bị và tải messages.yml
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Đảm bảo các tin nhắn mặc định được tải nếu file mới được tạo
        Reader defMessagesStream = new InputStreamReader(plugin.getResource("messages.yml"), StandardCharsets.UTF_8);
        if (defMessagesStream != null) {
            YamlConfiguration defMessages = YamlConfiguration.loadConfiguration(defMessagesStream);
            messagesConfig.setDefaults(defMessages);
        }
    }

    /**
     * Lấy một tin nhắn từ file messages.yml và thay thế các placeholder.
     * @param path Đường dẫn đến tin nhắn (ví dụ: "commands.reload.success")
     * @param replacements Các cặp key-value để thay thế (ví dụ: "%player%", "Steve")
     * @return Tin nhắn đã được định dạng màu và thay thế placeholder.
     */
    public String getMessage(String path, String... replacements) {
        // Lấy tin nhắn gốc từ config, nếu không có thì trả về đường dẫn
        String message = messagesConfig.getString(path, "&cMissing message: " + path);

        // Thêm prefix vào trước tin nhắn (trừ khi tin nhắn để trống)
        if (!message.isEmpty()) {
            String prefix = messagesConfig.getString("prefix", "");
            message = prefix + message;
        }

        // Thay thế các placeholder
        if (replacements.length > 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                if (i + 1 < replacements.length) {
                    message = message.replace(replacements[i], replacements[i + 1]);
                }
            }
        }

        // Dịch mã màu
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // Phiên bản không có placeholder để cho tiện lợi
    public String getMessage(String path) {
        return getMessage(path, new String[0]);
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