package com.alilshady.tutientranphap.utils;

import com.alilshady.tutientranphap.EssenceArrays;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigUpdater {

    public static void updateFile(EssenceArrays plugin, String fileName) throws IOException {
        File userFile = new File(plugin.getDataFolder(), fileName);

        // Nếu file không tồn tại, chỉ cần tạo mới và thoát.
        if (!userFile.exists()) {
            plugin.saveResource(fileName, false);
            return;
        }

        // Tải cấu hình của người dùng và cấu hình mặc định.
        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(userFile);
        InputStream defaultConfigStream = plugin.getResource(fileName);
        if (defaultConfigStream == null) {
            plugin.getLogger().warning("Could not find default file in jar: " + fileName);
            return;
        }
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));

        // Lấy phiên bản từ plugin.yml (qua pom.xml) và phiên bản trong file config của người dùng.
        String pluginVersion = plugin.getDescription().getVersion();
        String configVersion = userConfig.getString("config-version");

        // Nếu phiên bản giống nhau, không cần cập nhật.
        if (pluginVersion.equals(configVersion)) {
            return;
        }

        plugin.getLogger().info("An older version of " + fileName + " was found. Updating...");

        // 1. Sao lưu file cũ của người dùng để đề phòng lỗi.
        File backupFile = new File(userFile.getParent(), userFile.getName() + ".old");
        if(backupFile.exists()) {
            backupFile.delete();
        }
        userFile.renameTo(backupFile);

        // 2. Tạo lại file đó từ file mặc định mới nhất trong JAR.
        // Thao tác này đảm bảo tất cả các chú thích mới đều được ghi lại.
        plugin.saveResource(fileName, false);

        // 3. Tải lại file vừa tạo...
        FileConfiguration newConfig = YamlConfiguration.loadConfiguration(userFile);

        // 4. ...và ghi đè lại các giá trị mà người dùng đã cài đặt.
        for (String key : userConfig.getKeys(true)) {
            if (!defaultConfig.contains(key) || userConfig.isSet(key)) {
                // Chỉ ghi đè lại những giá trị mà người dùng đã thực sự thay đổi,
                // hoặc những giá trị tùy chỉnh không có trong file default.
                newConfig.set(key, userConfig.get(key));
            }
        }

        // 5. Cập nhật phiên bản config trong file mới.
        newConfig.set("config-version", pluginVersion);

        // 6. Lưu file lần cuối cùng.
        newConfig.save(userFile);
        plugin.getLogger().info(fileName + " has been successfully updated to the latest version!");
    }
}