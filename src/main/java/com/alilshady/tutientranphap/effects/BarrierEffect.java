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
        // Tăng lực đẩy mặc định để hiệu quả hơn và làm cho nó có thể cấu hình
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

            // Nếu thực thể ở bên ngoài bán kính, đẩy chúng ra xa một cách dứt khoát.
            if (distanceSquared > radiusSquared) {
                Vector pushVector = entityLoc.toVector().subtract(center.toVector()).normalize().multiply(force);
                pushVector.setY(0.2); // Hơi đẩy lên để tránh kẹt trong block
                entity.setVelocity(pushVector);
                // Thêm hiệu ứng hạt để làm rõ việc bị chặn
                entity.getWorld().spawnParticle(Particle.CRIT_MAGIC, entity.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0);
            }
            // Nếu thực thể ở bên trong nhưng ở trong vùng đệm 20% gần ranh giới, đẩy chúng vào trong.
            else if (distanceSquared > radiusSquared * 0.8) { // Vùng đệm 80% -> 100% bán kính
                Vector pushVector = center.toVector().subtract(entityLoc.toVector()).normalize().multiply(force * 0.7); // Đẩy ngược vào tâm với lực nhẹ hơn
                pushVector.setY(0.1);
                // Cộng dồn vào vận tốc hiện tại để tạo hiệu ứng mượt mà, không bị giật
                entity.setVelocity(entity.getVelocity().add(pushVector));
                // Hiệu ứng hạt nhẹ hơn để báo hiệu
                entity.getWorld().spawnParticle(Particle.CRIT_MAGIC, entity.getLocation().add(0, 1, 0), 1, 0.1, 0.1, 0.1, 0);
            }
        }
    }
}