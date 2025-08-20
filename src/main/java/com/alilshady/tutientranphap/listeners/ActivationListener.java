package com.alilshady.tutientranphap.listeners;

import com.alilshady.tutientranphap.EssenceArrays;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ActivationListener implements Listener {

    private final EssenceArrays plugin;

    public ActivationListener(EssenceArrays plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Chỉ xử lý khi nhấp chuột phải vào một khối block
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        // Chỉ xử lý với tay chính để tránh sự kiện bị gọi hai lần
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack itemInHand = event.getPlayer().getInventory().getItemInMainHand();
        // Đảm bảo người chơi có cầm vật phẩm
        if (itemInHand == null || itemInHand.getType() == Material.AIR) return;

        plugin.getFormationManager().attemptToActivate(event.getPlayer(), event.getClickedBlock(), itemInHand);
    }
}