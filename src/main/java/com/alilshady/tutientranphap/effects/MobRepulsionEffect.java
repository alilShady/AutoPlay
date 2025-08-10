// src/main/java/com/alilshady/tutientranphap/effects/MobRepulsionEffect.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MobRepulsionEffect implements FormationEffect {

    @Override
    public String getType() {
        return "REPEL";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        if (nearbyEntities == null) return;

        double force = EffectUtils.getDoubleFromConfig(config, "value", 0.8);
        Vector centerVector = center.toVector();

        for (LivingEntity entity : nearbyEntities) {
            if (entity instanceof Monster) {
                Vector awayVector = entity.getLocation().toVector().subtract(centerVector).normalize().multiply(force);
                awayVector.setY(0.1); // Đẩy nhẹ lên để tránh kẹt đất
                entity.setVelocity(awayVector);
            }
        }
    }
}