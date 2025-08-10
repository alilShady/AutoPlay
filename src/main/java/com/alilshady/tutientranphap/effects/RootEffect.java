// src/main/java/com/alilshady/tutientranphap/effects/RootEffect.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
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

public class RootEffect implements FormationEffect {

    @Override
    public String getType() {
        return "ROOT";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        if (nearbyEntities == null) return;

        int durationTicks = (int) EffectUtils.parseDurationToTicks(EffectUtils.getStringFromConfig(config, "duration", "40t"));
        int amplifier = EffectUtils.getIntFromConfig(config, "value", 7);
        PotionEffect rootEffect = new PotionEffect(PotionEffectType.SLOW, durationTicks, amplifier, true, false);
        String targetType = EffectUtils.getStringFromConfig(config, "target", "HOSTILE_MOBS").toUpperCase();

        for (LivingEntity entity : nearbyEntities) {
            boolean shouldRoot = (targetType.equals("HOSTILE_MOBS") && entity instanceof Monster) ||
                    (targetType.equals("ALL") && !(entity instanceof Player));
            if (shouldRoot) {
                entity.addPotionEffect(rootEffect);
            }
        }
    }
}