// src/main/java/com/alilshady/tutientranphap/listeners/StasisListener.java
package com.alilshady.tutientranphap.listeners;

import com.alilshady.tutientranphap.TuTienTranPhap;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

public class StasisListener implements Listener {

    private final TuTienTranPhap plugin;

    public StasisListener(TuTienTranPhap plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();

        // Kiểm tra xem vật thể bay có được phóng ra từ trong một trường STASIS không
        if (plugin.getFormationManager().isLocationInActiveFormation(projectile.getLocation(), "STASIS")) {
            // Lấy thông tin trận pháp để biết mức độ làm chậm
            int slowValue = plugin.getFormationManager().getFormationEffectValue(projectile.getLocation(), "STASIS", "value", 2);

            // Giảm vận tốc của vật thể bay
            Vector velocity = projectile.getVelocity();
            projectile.setVelocity(velocity.multiply(1.0 / slowValue));

            // Đối với mũi tên, cần giảm cả sát thương
            if (projectile instanceof Arrow) {
                Arrow arrow = (Arrow) projectile;
                arrow.setDamage(arrow.getDamage() / slowValue);
            }
        }
    }
}