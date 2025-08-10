// src/main/java/com/alilshady/tutientranphap/effects/VortexEffect.java
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

public class VortexEffect implements FormationEffect {

    @Override
    public String getType() {
        return "VORTEX";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        if (nearbyEntities == null) return;

        double force = EffectUtils.getDoubleFromConfig(config, "value", 0.5);
        String targetType = EffectUtils.getStringFromConfig(config, "target", "HOSTILE_MOBS").toUpperCase();
        Vector centerVector = center.toVector();

        for (LivingEntity entity : nearbyEntities) {
            boolean shouldAttract = false;
            if (targetType.equals("HOSTILE_MOBS") && entity instanceof Monster) {
                shouldAttract = true;
            }

            if (shouldAttract) {
                // Logic ngược với REPEL
                Vector toCenterVector = centerVector.clone().subtract(entity.getLocation().toVector()).normalize().multiply(force);
                toCenterVector.setY(0); // Chỉ hút theo mặt phẳng ngang để tránh kéo quái bay lên
                entity.setVelocity(entity.getVelocity().add(toCenterVector));
            }
        }
    }
}