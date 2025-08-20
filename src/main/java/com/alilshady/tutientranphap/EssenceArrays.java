package com.alilshady.tutientranphap;

import com.alilshady.tutientranphap.commands.CommandManager;
import com.alilshady.tutientranphap.commands.CommandTabCompleter;
import com.alilshady.tutientranphap.listeners.ActivationListener;
import com.alilshady.tutientranphap.listeners.BlueprintListener;
import com.alilshady.tutientranphap.listeners.SanctuaryListener;
import com.alilshady.tutientranphap.managers.*;
import com.alilshady.tutientranphap.utils.ConfigUpdater;
import com.alilshady.tutientranphap.utils.UP;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Objects;

public final class EssenceArrays extends JavaPlugin {

    private static EssenceArrays instance;

    private ConfigManager configManager;
    private FormationManager formationManager;
    private EffectHandler effectHandler;
    private TeamManager teamManager;
    private PreviewManager previewManager;

    @Override
    public void onEnable() {
        instance = this;
        logAsciiArt();

        try {
            ConfigUpdater.updateFile(this, "config.yml");
            ConfigUpdater.updateFile(this, "formations.yml");
            ConfigUpdater.updateFile(this, "lang/en.yml");
            ConfigUpdater.updateFile(this, "lang/vi.yml");
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
        this.previewManager = new PreviewManager(this);

        this.formationManager.loadFormations();
        this.previewManager.startPreviewTask();

        getServer().getPluginManager().registerEvents(new ActivationListener(this), this);
        getServer().getPluginManager().registerEvents(new BlueprintListener(this), this);
        getServer().getPluginManager().registerEvents(new SanctuaryListener(this), this);

        Objects.requireNonNull(getCommand("essencearrays")).setExecutor(new CommandManager(this));
        Objects.requireNonNull(getCommand("essencearrays")).setTabCompleter(new CommandTabCompleter(this));

        if (configManager.isDebugLoggingEnabled()) {
            getLogger().info("EssenceArrays by AlilShady has been enabled!");
        }

        UP.checkVersion(getDescription().getVersion());
    }

    private void logAsciiArt() {
        getLogger().info("____________________________________________________________");
        getLogger().info("");
        getLogger().info("                   ███████╗   █████╗   ");
        getLogger().info("                   ██╔════╝  ██╔══██╗  ");
        getLogger().info("                   █████╗    ███████║  ");
        getLogger().info("                   ██╔══╝    ██╔══██║  ");
        getLogger().info("                   ███████╗  ██║  ██║  ");
        getLogger().info("                   ╚══════╝  ╚═╝  ╚═╝  ");
        getLogger().info("");
        getLogger().info("            EssenceArrays plugin by AlilShady");
        getLogger().info("____________________________________________________________");
    }

    @Override
    public void onDisable() {
        if (previewManager != null) {
            previewManager.stopPreviewTask();
        }
        if (effectHandler != null) {
            effectHandler.stopAllEffects();
        }
        if (configManager != null && configManager.isDebugLoggingEnabled()) {
            getLogger().info("EssenceArrays has been disabled!");
        }
    }

    public void reloadPluginConfigs() {
        configManager.reloadConfigs();
        formationManager.loadFormations();
    }

    public static EssenceArrays getInstance() {
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

    public PreviewManager getPreviewManager() {
        return previewManager;
    }
}