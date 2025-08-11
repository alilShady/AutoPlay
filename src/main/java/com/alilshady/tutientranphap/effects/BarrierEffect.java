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

    /**
     * Phương thức này giờ sẽ để trống vì logic đã được chuyển sang một phương thức
     * riêng để gọi mỗi tick.
     */
    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        // Logic đã được chuyển đi
    }

    /**
     * Phương thức mới xử lý logic đẩy lùi, được gọi mỗi tick từ EffectHandler.
     */
    public void applyBarrierPush(Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities) {
        if (nearbyEntities == null) return;

        double radius = formation.getRadius();
        double radiusSquared = radius * radius;
        double force = EffectUtils.getDoubleFromConfig(config, "value", 1.5);
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

            if (distanceSquared > radiusSquared) {
                Vector pushVector = entityLoc.toVector().subtract(center.toVector()).normalize().multiply(force);
                pushVector.setY(0.2);
                entity.setVelocity(pushVector);
                entity.getWorld().spawnParticle(Particle.CRIT_MAGIC, entity.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0);
            }
            else if (distanceSquared > radiusSquared * 0.8) {
                Vector pushVector = center.toVector().subtract(entityLoc.toVector()).normalize().multiply(force * 0.7);
                pushVector.setY(0.1);
                entity.setVelocity(entity.getVelocity().add(pushVector));
                entity.getWorld().spawnParticle(Particle.CRIT_MAGIC, entity.getLocation().add(0, 1, 0), 1, 0.1, 0.1, 0.1, 0);
            }
        }
    }
}