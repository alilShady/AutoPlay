package com.alilshady.tutientranphap.listeners;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
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

    private final TuTienTranPhap plugin;
    // Tạo một key duy nhất để lưu trữ ID trận pháp trên vật phẩm
    private final NamespacedKey formationIdKey;

    public BlueprintListener(TuTienTranPhap plugin) {
        this.plugin = plugin;
        this.formationIdKey = new NamespacedKey(plugin, "formation_id");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Chỉ xử lý khi nhấp chuột phải vào một khối và bằng tay chính
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

        // Kiểm tra xem vật phẩm có phải là Trận Đồ không
        if (data.has(formationIdKey, PersistentDataType.STRING)) {
            // Hủy sự kiện để tránh các hành động mặc định (ví dụ: mở crafting table)
            event.setCancelled(true);

            String formationId = data.get(formationIdKey, PersistentDataType.STRING);
            Formation formation = plugin.getFormationManager().getFormationById(formationId);

            if (formation != null) {
                // Gọi hàm xây dựng trận pháp
                boolean success = plugin.getFormationManager().buildFormation(formation, Objects.requireNonNull(event.getClickedBlock()).getLocation(), event.getPlayer());
                if (success) {
                    // Trừ vật phẩm sau khi xây thành công
                    item.setAmount(item.getAmount() - 1);
                }
            }
        }
    }
}