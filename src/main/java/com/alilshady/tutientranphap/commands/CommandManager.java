package com.alilshady.tutientranphap.commands;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import java.util.Objects;

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
                return handleGiveBlueprint(sender, args, label);
            case "giveitem":
                return handleGiveItem(sender, args, label);
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

    private boolean handleGiveBlueprint(CommandSender sender, String[] args, String label) {
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

        String itemName = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(blueprint.getItemMeta().displayName()));

        sender.sendMessage(plugin.getConfigManager().getMessage("commands.give.success",
                "%amount%", String.valueOf(amount),
                "%item_name%", itemName,
                "%player%", target.getName()));

        return true;
    }

    private boolean handleGiveItem(CommandSender sender, String[] args, String label) {
        if (!sender.hasPermission("tutientranphap.giveitem")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.reload.no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "Sử dụng: /" + label + " giveitem <tên người chơi> <ID trận pháp> [số lượng]");
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

        ItemStack activationItem = formation.getActivationItem().clone();
        activationItem.setAmount(amount);

        target.getInventory().addItem(activationItem);

        String itemName = activationItem.hasItemMeta() && activationItem.getItemMeta().displayName() != null
                ? PlainTextComponentSerializer.plainText().serialize(activationItem.getItemMeta().displayName())
                : activationItem.getType().name();

        sender.sendMessage(plugin.getConfigManager().getMessage("commands.give.success",
                "%amount%", String.valueOf(amount),
                "%item_name%", itemName,
                "%player%", target.getName()));

        return true;
    }

    private boolean handleTest(CommandSender sender, String[] args, String label) {
        // --- ĐÂY LÀ PHẦN ĐÃ ĐƯỢC SỬA LỖI ---
        if (!(sender instanceof Player)) {
            sender.sendMessage("Lệnh này chỉ dành cho người chơi.");
            return true;
        }
        Player player = (Player) sender;
        // --- KẾT THÚC PHẦN SỬA LỖI ---

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
        location.getBlock().setType(formation.getCenterBlock());

        plugin.getEffectHandler().startFormationEffects(formation, location, player.getUniqueId());

        player.sendMessage(ChatColor.GREEN + "Đã kích hoạt thử nghiệm trận pháp: " + formation.getDisplayName());
        return true;
    }

    private ItemStack createBlueprintItem(Formation formation, int amount) {
        ItemStack item = new ItemStack(Material.PAPER, amount);
        ItemMeta meta = item.getItemMeta();

        assert meta != null;
        meta.displayName(plugin.getConfigManager().getMiniMessage().deserialize("<aqua>Trận Đồ: " + formation.getDisplayName()));
        meta.lore(Collections.singletonList(
                plugin.getConfigManager().getMiniMessage().deserialize("<gray>Nhấp chuột phải xuống đất để xây dựng.")
        ));

        meta.getPersistentDataContainer().set(formationIdKey, PersistentDataType.STRING, formation.getId());

        item.setItemMeta(meta);
        return item;
    }
}