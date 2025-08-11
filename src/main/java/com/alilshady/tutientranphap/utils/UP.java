package com.alilshady.tutientranphap.utils;

import com.alilshady.tutientranphap.TuTienTranPhap;
import org.bukkit.Bukkit;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UP {
    // URL này sẽ trỏ đến file chứa phiên bản mới nhất của plugin AutoPlay
    private static final String VERSION_URL = "https://vniteam.fun/AutoPlay.txt";

    public static void checkVersion(String currentVersion) {
        // Chạy tác vụ kiểm tra bất đồng bộ để không làm lag server
        Bukkit.getScheduler().runTaskAsynchronously(TuTienTranPhap.getInstance(), () -> {
            try {
                URL url = new URL(VERSION_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String latestVersion = reader.readLine().trim();

                    // So sánh phiên bản hiện tại với phiên bản mới nhất
                    if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                        TuTienTranPhap.getInstance().getLogger().warning("You are using an outdated version! Current: " + currentVersion + ", Latest: " + latestVersion);
                    } else {
                        TuTienTranPhap.getInstance().getLogger().info("You are using the latest version!");
                    }
                }
            } catch (IOException e) {
                TuTienTranPhap.getInstance().getLogger().severe("Could not check for updates: " + e.getMessage());
            }
        });
    }
}