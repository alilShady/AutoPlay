package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
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
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        // Logic được gọi mỗi tick trong applyBarrierPush, không cần code ở đây
    }

    // SỬA Ở ĐÂY: Thêm TuTienTranPhap plugin vào tham số
    public void applyBarrierPush(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, UUID ownerId) {
        if (nearbyEntities == null) return;

        double radius = formation.getRadius();
        double radiusSquared = radius * radius;
        double force = EffectUtils.getDoubleFromConfig(config, "value", 1.5);
        String targetType = EffectUtils.getStringFromConfig(config, "target", "ALL").toUpperCase();
        Player owner = (ownerId != null) ? Bukkit.getPlayer(ownerId) : null;

        for (LivingEntity entity : nearbyEntities) {
            boolean shouldApply = false;
            switch (targetType) {
                case "OWNER":
                    if (owner != null && entity.getUniqueId().equals(owner.getUniqueId())) {
                        shouldApply = true;
                    }
                    break;
                case "ALL":
                    shouldApply = true;
                    break;
                case "MOBS":
                    if (entity instanceof Monster) {
                        shouldApply = true;
                    }
                    break;
                case "DAMAGEABLE":
                    // SỬA Ở ĐÂY: Dùng biến plugin đã được truyền vào
                    if (entity instanceof Monster || (entity instanceof Player && owner != null && !plugin.getTeamManager().isAlly(owner, (Player) entity))) {
                        shouldApply = true;
                    }
                    break;
                case "UNDAMAGEABLE":
                    // SỬA Ở ĐÂY: Dùng biến plugin đã được truyền vào
                    if (entity instanceof Animals || (entity instanceof Player && owner != null && plugin.getTeamManager().isAlly(owner, (Player) entity))) {
                        shouldApply = true;
                    }
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