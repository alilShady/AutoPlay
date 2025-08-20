package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.EssenceArrays;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IgniteEffect implements FormationEffect {

    @Override
    public String getType() {
        return "IGNITE";
    }

    @Override
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyEntities == null) return;

        int durationTicks = (int) EffectUtils.parseDurationToTicks(EffectUtils.getStringFromConfig(config, "duration", "5s"));
        String targetType = EffectUtils.getStringFromConfig(config, "target", "DAMAGEABLE").toUpperCase();

        for (LivingEntity entity : nearbyEntities) {
            if (EffectUtils.shouldApplyToEntity(plugin, entity, targetType, ownerId)) {
                entity.setFireTicks(durationTicks);
            }
        }
    }
}