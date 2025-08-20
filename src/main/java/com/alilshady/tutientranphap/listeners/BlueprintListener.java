package com.alilshady.tutientranphap.listeners;

import com.alilshady.tutientranphap.EssenceArrays;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

public class BlueprintListener implements Listener {

    private final EssenceArrays plugin;
    private final NamespacedKey formationIdKey;

    public BlueprintListener(EssenceArrays plugin) {
        this.plugin = plugin;
        this.formationIdKey = new NamespacedKey(plugin, "formation_id");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.PAPER) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer data = meta.getPersistentDataContainer();

        if (data.has(formationIdKey, PersistentDataType.STRING)) {
            event.setCancelled(true);

            String formationId = data.get(formationIdKey, PersistentDataType.STRING);
            Formation formation = plugin.getFormationManager().getFormationById(formationId);

            if (formation != null) {
                Location buildLocation = Objects.requireNonNull(event.getClickedBlock()).getLocation();
                boolean success = plugin.getFormationManager().buildFormation(formation, buildLocation, event.getPlayer());
                if (success) {
                    item.setAmount(item.getAmount() - 1);
                }
            } else {
                // --- PHẦN THÊM MỚI ---
                // Gửi tin nhắn cho người chơi nếu Trận Đồ bị lỗi hoặc không hợp lệ
                event.getPlayer().sendMessage(plugin.getConfigManager().getMessage("commands.give.formation-not-found", "%id%", formationId));
            }
        }
    }
}