// src/main/java/com/alilshady/tutientranphap/effects/BarrierEffect.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BarrierEffect implements FormationEffect {

    @Override
    public String getType() {
        return "BARRIER";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        if (nearbyEntities == null) return;

        double radius = formation.getRadius();
        double radiusSquared = radius * radius;
        double force = EffectUtils.getDoubleFromConfig(config, "value", 0.8);
        String targetType = EffectUtils.getStringFromConfig(config, "target", "ALL").toUpperCase();

        for (LivingEntity entity : nearbyEntities) {
            boolean shouldApply = false;
            switch (targetType) {
                case "PLAYERS":
                    if (entity instanceof Player) shouldApply = true;
                    break;
                case "HOSTILE_MOBS":
                    if (entity instanceof Monster) shouldApply = true;
                    break;
                case "ALL":
                default:
                    shouldApply = true;
                    break;
            }

            if (!shouldApply) continue;

            Location entityLoc = entity.getLocation();
            double distanceSquared = center.distanceSquared(entityLoc);

            // Chỉ tác động khi thực thể ở gần ranh giới
            if (Math.abs(distanceSquared - radiusSquared) < 4) {
                Vector pushVector = entityLoc.toVector().subtract(center.toVector()).normalize();

                if (distanceSquared < radiusSquared) {
                    pushVector.multiply(-1);
                }

                pushVector.multiply(force);
                pushVector.setY(0.1);
                entity.setVelocity(pushVector);

                // SỬA LỖI: Sử dụng entity.getWorld() thay vì biến "player" không tồn tại
                entity.getWorld().spawnParticle(Particle.CRIT_MAGIC, entity.getLocation().add(0, 1, 0), 5);
            }
        }
    }
}