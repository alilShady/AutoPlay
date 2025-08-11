// src/main/java/com/alilshady/tutientranphap/TuTienTranPhap.java
package com.alilshady.tutientranphap;

import com.alilshady.tutientranphap.commands.CommandManager;
import com.alilshady.tutientranphap.commands.CommandTabCompleter;
import com.alilshady.tutientranphap.listeners.ActivationListener;
import com.alilshady.tutientranphap.listeners.BlueprintListener;
import com.alilshady.tutientranphap.listeners.SanctuaryListener;
import com.alilshady.tutientranphap.managers.ConfigManager;
import com.alilshady.tutientranphap.managers.EffectHandler;
import com.alilshady.tutientranphap.managers.FormationManager;
import com.alilshady.tutientranphap.managers.TeamManager; // Thêm import
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class TuTienTranPhap extends JavaPlugin {

    private ConfigManager configManager;
    private FormationManager formationManager;
    private EffectHandler effectHandler;
    private TeamManager teamManager; // Thêm trường mới

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.effectHandler = new EffectHandler(this);
        this.formationManager = new FormationManager(this);
        this.teamManager = new TeamManager(this); // Khởi tạo TeamManager

        reloadPluginConfigs();

        // Đăng ký listener
        getServer().getPluginManager().registerEvents(new ActivationListener(this), this);
        getServer().getPluginManager().registerEvents(new BlueprintListener(this), this);
        getServer().getPluginManager().registerEvents(new SanctuaryListener(this), this);

        // Đăng ký command executor và tab completer
        Objects.requireNonNull(getCommand("tutientranphap")).setExecutor(new CommandManager(this));
        Objects.requireNonNull(getCommand("tutientranphap")).setTabCompleter(new CommandTabCompleter(this));

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

    // Thêm getter cho TeamManager
    public TeamManager getTeamManager() {
        return teamManager;
    }
}