// src/main/java/com/alilshady/tutientranphap/effects/PotionEffectStrategy.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.managers.EffectHandler;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PotionEffectStrategy implements FormationEffect {

    @Override
    public String getType() {
        return "POTION";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        if (nearbyEntities == null) return;

        String potionName = EffectUtils.getStringFromConfig(config, "potion_effect", "");
        if (potionName.isEmpty()) return;

        PotionEffectType potionType = PotionEffectType.getByName(potionName.toUpperCase());
        if (potionType == null) return;

        int amplifier = EffectUtils.getIntFromConfig(config, "value", 0);
        int durationTicks = plugin.getConfigManager().getEffectCheckInterval() + 40; // Add buffer to ensure effect lasts until next check
        PotionEffect effect = new PotionEffect(potionType, durationTicks, amplifier, true, false);

        String targetType = EffectUtils.getStringFromConfig(config, "target", "PLAYERS").toUpperCase();

        for (LivingEntity entity : nearbyEntities) {
            boolean shouldApply = false;
            switch (targetType) {
                case "PLAYERS":
                    if (entity instanceof Player) shouldApply = true;
                    break;
                case "HOSTILE_MOBS":
                    if (entity instanceof Monster) shouldApply = true;
                    break;
                case "ALL":
                    shouldApply = true;
                    break;
            }

            if (shouldApply) {
                entity.addPotionEffect(effect);
                // The tracking of affected entities is now managed by EffectHandler directly.
                plugin.getEffectHandler().trackAffectedEntity(center, entity.getUniqueId());
            }
        }
    }
}