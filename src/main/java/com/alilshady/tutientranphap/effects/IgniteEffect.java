// src/main/java/com/alilshady/tutientranphap/effects/IgniteEffect.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class IgniteEffect implements FormationEffect {

    @Override
    public String getType() {
        return "IGNITE";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        if (nearbyEntities == null) return;

        // Lấy thời gian cháy (tính bằng tick) từ config
        int durationTicks = (int) EffectUtils.parseDurationToTicks(EffectUtils.getStringFromConfig(config, "duration", "5s"));
        String targetType = EffectUtils.getStringFromConfig(config, "target", "HOSTILE_MOBS").toUpperCase();

        for (LivingEntity entity : nearbyEntities) {
            boolean shouldIgnite = false;
            switch (targetType) {
                case "PLAYERS":
                    if (entity instanceof Player) shouldIgnite = true;
                    break;
                case "HOSTILE_MOBS":
                    if (entity instanceof Monster) shouldIgnite = true;
                    break;
                case "ALL":
                    shouldIgnite = true;
                    break;
            }

            if (shouldIgnite) {
                // Đốt cháy thực thể trong một khoảng thời gian
                entity.setFireTicks(durationTicks);
            }
        }
    }
}