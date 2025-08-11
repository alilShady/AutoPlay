package com.alilshady.tutientranphap;

import com.alilshady.tutientranphap.commands.CommandManager;
import com.alilshady.tutientranphap.commands.CommandTabCompleter;
import com.alilshady.tutientranphap.listeners.ActivationListener;
import com.alilshady.tutientranphap.listeners.BlueprintListener;
import com.alilshady.tutientranphap.listeners.SanctuaryListener;
import com.alilshady.tutientranphap.managers.ConfigManager;
import com.alilshady.tutientranphap.managers.EffectHandler;
import com.alilshady.tutientranphap.managers.FormationManager;
import com.alilshady.tutientranphap.managers.TeamManager;
import com.alilshady.tutientranphap.utils.ConfigUpdater;
import com.alilshady.tutientranphap.utils.UP;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Objects;

public final class TuTienTranPhap extends JavaPlugin {

    private static TuTienTranPhap instance;

    private ConfigManager configManager;
    private FormationManager formationManager;
    private EffectHandler effectHandler;
    private TeamManager teamManager;

    @Override
    public void onEnable() {
        instance = this;

        // --- BẮT ĐẦU HIỂN THỊ LOGO KHỞI ĐỘNG ---
        logAsciiArt();
        // --- KẾT THÚC HIỂN THỊ LOGO ---

        try {
            ConfigUpdater.updateFile(this, "config.yml");
            ConfigUpdater.updateFile(this, "formations.yml");
            ConfigUpdater.updateFile(this, "messages.yml");
        } catch (IOException e) {
            getLogger().severe("Could not update configuration files!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.configManager = new ConfigManager(this);
        this.effectHandler = new EffectHandler(this);
        this.formationManager = new FormationManager(this);
        this.teamManager = new TeamManager(this);

        reloadPluginConfigs();

        getServer().getPluginManager().registerEvents(new ActivationListener(this), this);
        getServer().getPluginManager().registerEvents(new BlueprintListener(this), this);
        getServer().getPluginManager().registerEvents(new SanctuaryListener(this), this);

        Objects.requireNonNull(getCommand("tutientranphap")).setExecutor(new CommandManager(this));
        Objects.requireNonNull(getCommand("tutientranphap")).setTabCompleter(new CommandTabCompleter(this));

        if (configManager.isDebugLoggingEnabled()) {
            getLogger().info("TuTienTranPhap by AlilShady has been enabled!");
        }

        UP.checkVersion(getDescription().getVersion());
    }

    // --- PHƯƠNG THỨC MỚI ĐỂ HIỂN THỊ LOGO ---
    private void logAsciiArt() {
        getLogger().info("____________________________________________________________");
        getLogger().info("");
        getLogger().info("      ████████╗████████╗████████╗██████╗  ");
        getLogger().info("      ╚══██╔══╝╚══██╔══╝╚══██╔══╝██╔══██╗ ");
        getLogger().info("         ██║      ██║      ██║   ██████╔╝ ");
        getLogger().info("         ██║      ██║      ██║   ██╔═══╝  ");
        getLogger().info("         ██║      ██║      ██║   ██║      ");
        getLogger().info("         ╚═╝      ╚═╝      ╚═╝   ╚═╝      ");
        getLogger().info("");
        getLogger().info("         TuTienTranPhap plugin by AlilShady");
        getLogger().info("____________________________________________________________");
    }
    // --- KẾT THÚC PHƯƠNG THỨC MỚI ---

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

    public static TuTienTranPhap getInstance() {
        return instance;
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

    public TeamManager getTeamManager() {
        return teamManager;
    }
}