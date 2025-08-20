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

public class LightningStrikeEffect implements FormationEffect {

    @Override
    public String getType() {
        return "LIGHTNING";
    }

    @Override
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyEntities == null) return;

        boolean visualOnly = EffectUtils.getBooleanFromConfig(config, "visual_only", false);
        String targetType = EffectUtils.getStringFromConfig(config, "target", "DAMAGEABLE").toUpperCase();

        for (LivingEntity entity : nearbyEntities) {
            if (EffectUtils.shouldApplyToEntity(plugin, entity, targetType, ownerId)) {
                if (visualOnly) {
                    entity.getWorld().strikeLightningEffect(entity.getLocation());
                } else {
                    entity.getWorld().strikeLightning(entity.getLocation());
                }
            }
        }
    }
}