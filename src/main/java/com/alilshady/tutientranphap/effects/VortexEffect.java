package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.EssenceArrays;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VortexEffect implements FormationEffect {

    @Override
    public String getType() {
        return "VORTEX";
    }

    @Override
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyEntities == null) return;

        double force = EffectUtils.getDoubleFromConfig(config, "value", 0.5);
        String targetType = EffectUtils.getStringFromConfig(config, "target", "DAMAGEABLE").toUpperCase();
        Vector centerVector = center.toVector();

        for (LivingEntity entity : nearbyEntities) {
            if (EffectUtils.shouldApplyToEntity(plugin, entity, targetType, ownerId)) {
                Vector toCenterVector = centerVector.clone().subtract(entity.getLocation().toVector()).normalize().multiply(force);
                toCenterVector.setY(0);
                entity.setVelocity(entity.getVelocity().add(toCenterVector));
            }
        }
    }
}