// src/main/java/com/alilshady/tutientranphap/effects/CleanseEffect.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.managers.EffectHandler;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CleanseEffect implements FormationEffect {

    @Override
    public String getType() {
        return "CLEANSE";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        if (nearbyEntities == null) return;

        String targetType = EffectUtils.getStringFromConfig(config, "target", "PLAYERS").toUpperCase();

        for (LivingEntity entity : nearbyEntities) {
            boolean shouldCleanse = (targetType.equals("PLAYERS") && entity instanceof Player) || targetType.equals("ALL");

            if (shouldCleanse) {
                // Sử dụng danh sách debuff tĩnh từ EffectHandler
                EffectHandler.DEBUFFS_TO_CLEANSE.forEach(entity::removePotionEffect);
            }
        }
    }
}