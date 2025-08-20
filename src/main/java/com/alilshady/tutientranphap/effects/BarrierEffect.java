package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.EssenceArrays;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BarrierEffect implements FormationEffect {

    @Override
    public String getType() {
        return "BARRIER";
    }

    @Override
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        // Logic được gọi mỗi tick trong applyBarrierPush, không cần code ở đây
    }

    public void applyBarrierPush(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, UUID ownerId) {
        if (nearbyEntities == null) return;

        double radius = formation.getRadius();
        double radiusSquared = radius * radius;
        double force = EffectUtils.getDoubleFromConfig(config, "value", 1.5);
        String targetType = EffectUtils.getStringFromConfig(config, "target", "ALL").toUpperCase();

        for (LivingEntity entity : nearbyEntities) {
            // Thay thế khối switch bằng một lời gọi phương thức duy nhất.
            if (!EffectUtils.shouldApplyToEntity(plugin, entity, targetType, ownerId)) {
                continue;
            }

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