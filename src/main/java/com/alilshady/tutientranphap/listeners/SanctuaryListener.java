// src/main/java/com/alilshady/tutientranphap/listeners/SanctuaryListener.java
package com.alilshady.tutientranphap.listeners;

import com.alilshady.tutientranphap.TuTienTranPhap;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class SanctuaryListener implements Listener {

    private final TuTienTranPhap plugin;

    public SanctuaryListener(TuTienTranPhap plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Chỉ chặn quái sinh ra tự nhiên
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }

        Location spawnLocation = event.getLocation();

        // Kiểm tra xem vị trí sinh ra có nằm trong một trận pháp "SANCTUARY" nào không
        if (plugin.getFormationManager().isLocationInActiveFormation(spawnLocation, "SANCTUARY")) {
            event.setCancelled(true);
        }
    }
}