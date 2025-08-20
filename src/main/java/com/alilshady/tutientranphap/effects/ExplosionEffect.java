package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.EssenceArrays;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ExplosionEffect implements FormationEffect {

    @Override
    public String getType() {
        return "EXPLOSION";
    }

    @Override
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyEntities == null) return;
        World world = center.getWorld();
        if (world == null) return;

        float power = (float) EffectUtils.getDoubleFromConfig(config, "value", 2.0);
        String targetType = EffectUtils.getStringFromConfig(config, "target", "DAMAGEABLE").toUpperCase();

        for (LivingEntity entity : nearbyEntities) {
            if (EffectUtils.shouldApplyToEntity(plugin, entity, targetType, ownerId)) {
                world.createExplosion(entity.getLocation(), power, true, false);
            }
        }
    }
}