package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.EssenceArrays;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RootEffect implements FormationEffect {

    @Override
    public String getType() {
        return "ROOT";
    }

    @Override
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyEntities == null) return;

        int durationTicks = (int) EffectUtils.parseDurationToTicks(EffectUtils.getStringFromConfig(config, "duration", "40t"));
        int amplifier = EffectUtils.getIntFromConfig(config, "value", 7);
        PotionEffect rootEffect = new PotionEffect(PotionEffectType.SLOW, durationTicks, amplifier, true, false);
        String targetType = EffectUtils.getStringFromConfig(config, "target", "DAMAGEABLE").toUpperCase();

        for (LivingEntity entity : nearbyEntities) {
            if (EffectUtils.shouldApplyToEntity(plugin, entity, targetType, ownerId)) {
                entity.addPotionEffect(rootEffect);
            }
        }
    }
}