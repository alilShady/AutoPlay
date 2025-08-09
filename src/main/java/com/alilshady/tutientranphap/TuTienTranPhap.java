package com.alilshady.tutientranphap;

import com.alilshady.tutientranphap.commands.CommandManager;
import com.alilshady.tutientranphap.listeners.ActivationListener;
import com.alilshady.tutientranphap.managers.ConfigManager;
import com.alilshady.tutientranphap.managers.EffectHandler;
import com.alilshady.tutientranphap.managers.FormationManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

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

        // Tải config và các trận pháp
        reloadPluginConfigs();

        // Đăng ký listener và command
        getServer().getPluginManager().registerEvents(new ActivationListener(this), this);
        Objects.requireNonNull(getCommand("tutientranphap")).setExecutor(new CommandManager(this));

        if (configManager.isDebugLoggingEnabled()) {
            getLogger().info("TuTienTranPhap by AlilShady has been enabled!");
        }
    }

    @Override
    public void onDisable() {
        // Dừng tất cả các hiệu ứng đang chạy
        if (effectHandler != null) {
            effectHandler.stopAllEffects();
        }
        if (configManager != null && configManager.isDebugLoggingEnabled()) {
            getLogger().info("TuTienTranPhap has been disabled!");
        }
    }

    /**
     * Tải lại tất cả các tệp cấu hình và làm mới dữ liệu của plugin.
     */
    public void reloadPluginConfigs() {
        // Tải lại các file config từ đĩa
        configManager.reloadConfigs();
        // Tải lại các trận pháp một cách bất đồng bộ
        formationManager.loadFormations();
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