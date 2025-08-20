package com.alilshady.tutientranphap.listeners;

import com.alilshady.tutientranphap.EssenceArrays;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority; // <-- THÊM IMPORT MỚI
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
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

    /**
     * Lắng nghe sự kiện người chơi nhấn phím F (tráo đổi vật phẩm) để xoay Trận Đồ.
     * Đặt ở mức ưu tiên HIGHEST để đảm bảo event.setCancelled(true) hoạt động chính xác.
     */
    @EventHandler(priority = EventPriority.HIGHEST) // <-- SỬA Ở ĐÂY
    public void onPlayerSwapHand(PlayerSwapHandItemsEvent event) {
        ItemStack mainHandItem = event.getMainHandItem();

        if (isBlueprint(mainHandItem)) {
            // Hủy sự kiện để ngăn vật phẩm bị tráo đổi sang tay phụ.
            event.setCancelled(true);
            // Thực hiện logic xoay.
            plugin.getPreviewManager().rotatePreview(event.getPlayer());
        }
    }

    /**
     * Lắng nghe sự kiện người chơi nhấp chuột phải để xây dựng Trận Đồ.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isBlueprint(item)) {
            return;
        }

        event.setCancelled(true);
        plugin.getPreviewManager().clearPreview(event.getPlayer());

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        String formationId = data.get(formationIdKey, PersistentDataType.STRING);
        Formation formation = plugin.getFormationManager().getFormationById(formationId);

        if (formation != null) {
            Location buildLocation = Objects.requireNonNull(event.getClickedBlock()).getLocation();
            int rotation = plugin.getPreviewManager().getRotation(event.getPlayer());
            boolean success = plugin.getFormationManager().buildFormation(formation, buildLocation, event.getPlayer(), rotation);

            if (success) {
                item.setAmount(item.getAmount() - 1);
            }
        } else {
            event.getPlayer().sendMessage(plugin.getConfigManager().getMessage("commands.give.formation-not-found", "%id%", formationId));
        }
    }

    /**
     * Hàm tiện ích để kiểm tra một vật phẩm có phải là Trận Đồ hợp lệ hay không.
     */
    private boolean isBlueprint(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(formationIdKey, PersistentDataType.STRING);
    }
}