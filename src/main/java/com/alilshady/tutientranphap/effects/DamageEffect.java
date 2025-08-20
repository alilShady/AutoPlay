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

public class DamageEffect implements FormationEffect {

    @Override
    public String getType() {
        return "DAMAGE";
    }

    @Override
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyEntities == null) return;

        double damage = EffectUtils.getDoubleFromConfig(config, "value", 1.0);
        String targetType = EffectUtils.getStringFromConfig(config, "target", "MOBS").toUpperCase();

        for (LivingEntity entity : nearbyEntities) {
            if (EffectUtils.shouldApplyToEntity(plugin, entity, targetType, ownerId)) {
                entity.damage(damage);
            }
        }
    }
}