package com.alilshady.tutientranphap;

import com.alilshady.tutientranphap.commands.CommandManager;
import com.alilshady.tutientranphap.listeners.ActivationListener;
// Thêm import cho listener mới
import com.alilshady.tutientranphap.listeners.BlueprintListener;
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
        this.configManager = new ConfigManager(this);
        this.effectHandler = new EffectHandler(this);
        this.formationManager = new FormationManager(this);

        reloadPluginConfigs();

        // Đăng ký listener và command
        getServer().getPluginManager().registerEvents(new ActivationListener(this), this);
        // Đăng ký listener mới cho Trận Đồ
        getServer().getPluginManager().registerEvents(new BlueprintListener(this), this);

        Objects.requireNonNull(getCommand("tutientranphap")).setExecutor(new CommandManager(this));

        if (configManager.isDebugLoggingEnabled()) {
            getLogger().info("TuTienTranPhap by AlilShady has been enabled!");
        }
    }

    @Override
    public void onDisable() {
        if (effectHandler != null) {
            effectHandler.stopAllEffects();
        }
        if (configManager != null && configManager.isDebugLoggingEnabled()) {
            getLogger().info("TuTienTranPhap has been disabled!");
        }
    }

    public void reloadPluginConfigs() {
        configManager.reloadConfigs();
        formationManager.loadFormations();
    }

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