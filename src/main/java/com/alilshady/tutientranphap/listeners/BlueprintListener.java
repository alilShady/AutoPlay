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
import org.bukkit.event.player.PlayerSwapHandItemsEvent; // <-- IMPORT MỚI
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

    // --- HÀM MỚI ĐỂ XỬ LÝ SỰ KIỆN XOAY ---
    @EventHandler
    public void onPlayerSwapHand(PlayerSwapHandItemsEvent event) {
        // Chỉ xử lý với vật phẩm ở tay chính
        ItemStack mainHandItem = event.getMainHandItem();
        if (mainHandItem == null || mainHandItem.getType() != Material.PAPER) {
            return;
        }

        ItemMeta meta = mainHandItem.getItemMeta();
        if (meta == null) {
            return;
        }

        if (meta.getPersistentDataContainer().has(formationIdKey, PersistentDataType.STRING)) {
            // Hủy sự kiện để không tráo đổi vật phẩm
            event.setCancelled(true);
            // Gọi hàm xoay từ PreviewManager
            plugin.getPreviewManager().rotatePreview(event.getPlayer());
        }
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
            plugin.getPreviewManager().clearPreview(event.getPlayer());

            String formationId = data.get(formationIdKey, PersistentDataType.STRING);
            Formation formation = plugin.getFormationManager().getFormationById(formationId);

            if (formation != null) {
                Location buildLocation = Objects.requireNonNull(event.getClickedBlock()).getLocation();
                // Lấy góc xoay hiện tại từ PreviewManager và truyền vào
                int rotation = plugin.getPreviewManager().getRotation(event.getPlayer());
                boolean success = plugin.getFormationManager().buildFormation(formation, buildLocation, event.getPlayer(), rotation);
                if (success) {
                    item.setAmount(item.getAmount() - 1);
                }
            } else {
                event.getPlayer().sendMessage(plugin.getConfigManager().getMessage("commands.give.formation-not-found", "%id%", formationId));
            }
        }
    }
}