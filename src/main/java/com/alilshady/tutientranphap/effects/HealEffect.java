// src/main/java/com/alilshady/tutientranphap/effects/HealEffect.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class HealEffect implements FormationEffect {

    @Override
    public String getType() {
        return "HEAL";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        if (nearbyEntities == null) return;

        String targetType = EffectUtils.getStringFromConfig(config, "target", "PLAYERS").toUpperCase();
        double healAmount = EffectUtils.getDoubleFromConfig(config, "value", 1.0);

        for (LivingEntity entity : nearbyEntities) {
            boolean shouldHeal = false;
            switch (targetType) {
                case "PLAYERS":
                    if (entity instanceof Player) shouldHeal = true;
                    break;
                case "ALL":
                    shouldHeal = true;
                    break;
            }

            if (shouldHeal) {
                AttributeInstance maxHealthAttribute = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                double maxHealth = (maxHealthAttribute != null) ? maxHealthAttribute.getValue() : entity.getHealth(); // Fallback to current health if no attribute
                double newHealth = Math.min(entity.getHealth() + healAmount, maxHealth);
                entity.setHealth(newHealth);
            }
        }
    }
}