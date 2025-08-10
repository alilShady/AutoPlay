// src/main/java/com/alilshady/tutientranphap/commands/CommandManager.java
package com.alilshady.tutientranphap.commands;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class CommandManager implements CommandExecutor {

    private final TuTienTranPhap plugin;
    private final NamespacedKey formationIdKey;

    public CommandManager(TuTienTranPhap plugin) {
        this.plugin = plugin;
        this.formationIdKey = new NamespacedKey(plugin, "formation_id");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.usage", "%command%", label));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "give":
                return handleGive(sender, args, label);
            case "test":
                return handleTest(sender, args, label);
            default:
                sender.sendMessage(plugin.getConfigManager().getMessage("commands.usage", "%command%", label));
                return true;
        }
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("tutientranphap.reload")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.reload.no-permission"));
            return true;
        }
        plugin.getEffectHandler().stopAllEffects();
        plugin.reloadPluginConfigs();
        sender.sendMessage(plugin.getConfigManager().getMessage("commands.reload.success"));
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args, String label) {
        if (!sender.hasPermission("tutientranphap.give")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.reload.no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.give.usage", "%command%", label));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.give.player-not-found", "%player%", args[1]));
            return true;
        }

        String formationId = args[2];
        Formation formation = plugin.getFormationManager().getFormationById(formationId);
        if (formation == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.give.formation-not-found", "%id%", formationId));
            return true;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Số lượng không hợp lệ.");
                return true;
            }
        }

        ItemStack blueprint = createBlueprintItem(formation, amount);
        target.getInventory().addItem(blueprint);

        sender.sendMessage(plugin.getConfigManager().getMessage("commands.give.success",
                "%amount%", String.valueOf(amount),
                "%item_name%", blueprint.getItemMeta().getDisplayName(),
                "%player%", target.getName()));

        return true;
    }

    /**
     * Xử lý lệnh /ttp test <id> để nhanh chóng kích hoạt một trận pháp.
     */
    private boolean handleTest(CommandSender sender, String[] args, String label) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Lệnh này chỉ dành cho người chơi.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("tutientranphap.test")) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.reload.no-permission"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Sử dụng: /" + label + " test <ID trận pháp>");
            return true;
        }

        String formationId = args[1];
        Formation formation = plugin.getFormationManager().getFormationById(formationId);

        if (formation == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.give.formation-not-found", "%id%", formationId));
            return true;
        }

        Location location = player.getLocation().getBlock().getLocation();

        // Đặt khối trung tâm để trận pháp không tự hủy ngay lập tức
        location.getBlock().setType(formation.getCenterBlock());

        plugin.getEffectHandler().startFormationEffects(formation, location);

        player.sendMessage(ChatColor.GREEN + "Đã kích hoạt thử nghiệm trận pháp: " + formation.getDisplayName());
        return true;
    }

    private ItemStack createBlueprintItem(Formation formation, int amount) {
        ItemStack item = new ItemStack(Material.PAPER, amount);
        ItemMeta meta = item.getItemMeta();

        assert meta != null;
        meta.setDisplayName(ChatColor.AQUA + "Trận Đồ: " + formation.getDisplayName());
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "Nhấp chuột phải xuống đất để xây dựng."));

        meta.getPersistentDataContainer().set(formationIdKey, PersistentDataType.STRING, formation.getId());

        item.setItemMeta(meta);
        return item;
    }
}