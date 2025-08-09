package com.alilshady.tutientranphap;

import com.alilshady.tutientranphap.listeners.ActivationListener;
import com.alilshady.tutientranphap.managers.ConfigManager;
import com.alilshady.tutientranphap.managers.EffectHandler;
import com.alilshady.tutientranphap.managers.FormationManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class TuTienTranPhap extends JavaPlugin {

    private ConfigManager configManager;
    private FormationManager formationManager;
    private EffectHandler effectHandler;

    @Override
    public void onEnable() {
        // Khởi tạo các trình quản lý
        this.configManager = new ConfigManager(this);
        this.effectHandler = new EffectHandler(this);
        this.formationManager = new FormationManager(this);

        // Tải các trận pháp từ config
        this.formationManager.loadFormations();

        // Đăng ký listener
        getServer().getPluginManager().registerEvents(new ActivationListener(this), this);

        getLogger().info("TuTienTranPhap by AlilShady has been enabled!");
    }

    @Override
    public void onDisable() {
        // Dừng tất cả các hiệu ứng đang chạy
        if (effectHandler != null) {
            effectHandler.stopAllEffects();
        }
        getLogger().info("TuTienTranPhap has been disabled!");
    }

    // Getters để các lớp khác có thể truy cập
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public FormationManager getFormationManager() {
        return formationManager;
    }

    public EffectHandler getEffectHandler() {
        return effectHandler;
    }
}