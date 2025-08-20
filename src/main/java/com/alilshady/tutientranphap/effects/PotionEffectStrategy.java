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

public class PotionEffectStrategy implements FormationEffect {

    @Override
    public String getType() {
        return "POTION";
    }

    @Override
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyEntities == null) return;

        String potionName = EffectUtils.getStringFromConfig(config, "potion_effect", "");
        if (potionName.isEmpty()) return;

        PotionEffectType potionType = PotionEffectType.getByName(potionName.toUpperCase());
        if (potionType == null) return;

        int amplifier = EffectUtils.getIntFromConfig(config, "value", 0);
        int durationTicks = plugin.getConfigManager().getEffectCheckInterval() + 40;
        PotionEffect effect = new PotionEffect(potionType, durationTicks, amplifier, true, false);

        String targetType = EffectUtils.getStringFromConfig(config, "target", "OWNER").toUpperCase();

        for (LivingEntity entity : nearbyEntities) {
            if (EffectUtils.shouldApplyToEntity(plugin, entity, targetType, ownerId)) {
                entity.addPotionEffect(effect);
                plugin.getEffectHandler().trackAffectedEntity(center, entity.getUniqueId());
            }
        }
    }
}