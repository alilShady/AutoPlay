// src/main/java/com/alilshady/tutientranphap/effects/DamageEffect.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DamageEffect implements FormationEffect {

    @Override
    public String getType() {
        return "DAMAGE";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        if (nearbyEntities == null) return;

        double damage = EffectUtils.getDoubleFromConfig(config, "value", 1.0);
        String targetType = EffectUtils.getStringFromConfig(config, "target", "HOSTILE_MOBS").toUpperCase();

        for (LivingEntity entity : nearbyEntities) {
            if (entity instanceof Player && ((Player) entity).getGameMode() != GameMode.SURVIVAL) continue;

            boolean shouldDamage = false;
            switch (targetType) {
                case "PLAYERS":
                    if (entity instanceof Player) shouldDamage = true;
                    break;
                case "HOSTILE_MOBS":
                    if (entity instanceof Monster) shouldDamage = true;
                    break;
                case "ALL":
                    shouldDamage = true;
                    break;
            }

            if (shouldDamage) {
                entity.damage(damage);
            }
        }
    }
}