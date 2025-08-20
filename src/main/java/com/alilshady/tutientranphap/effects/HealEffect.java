package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.EssenceArrays;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HealEffect implements FormationEffect {

    @Override
    public String getType() {
        return "HEAL";
    }

    @Override
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyEntities == null) return;

        String targetType = EffectUtils.getStringFromConfig(config, "target", "UNDAMAGEABLE").toUpperCase();
        double healAmount = EffectUtils.getDoubleFromConfig(config, "value", 1.0);

        for (LivingEntity entity : nearbyEntities) {
            if (EffectUtils.shouldApplyToEntity(plugin, entity, targetType, ownerId)) {
                AttributeInstance maxHealthAttribute = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                double maxHealth = (maxHealthAttribute != null) ? maxHealthAttribute.getValue() : 20.0;
                double newHealth = Math.min(entity.getHealth() + healAmount, maxHealth);
                entity.setHealth(newHealth);
            }
        }
    }
}