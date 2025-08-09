package com.alilshady.tutientranphap.commands;

import com.alilshady.tutientranphap.TuTienTranPhap;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class CommandManager implements CommandExecutor {

    private final TuTienTranPhap plugin;

    public CommandManager(TuTienTranPhap plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("tutientranphap.reload")) {
                sender.sendMessage(ChatColor.RED + "Bạn không có quyền sử dụng lệnh này.");
                return true;
            }

            // Dừng tất cả hiệu ứng cũ trước khi reload
            plugin.getEffectHandler().stopAllEffects();
            // Tải lại config
            plugin.reloadPluginConfigs();

            sender.sendMessage(ChatColor.GREEN + "Plugin TuTienTranPhap đã được tải lại thành công!");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Sử dụng: /" + label + " reload");
        return true;
    }
}