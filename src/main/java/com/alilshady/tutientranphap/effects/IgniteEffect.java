package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.EssenceArrays;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IgniteEffect implements FormationEffect {

    @Override
    public String getType() {
        return "IGNITE";
    }

    @Override
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyEntities == null) return;

        int durationTicks = (int) EffectUtils.parseDurationToTicks(EffectUtils.getStringFromConfig(config, "duration", "5s"));
        String targetType = EffectUtils.getStringFromConfig(config, "target", "DAMAGEABLE").toUpperCase();
        Player owner = (ownerId != null) ? Bukkit.getPlayer(ownerId) : null;

        for (LivingEntity entity : nearbyEntities) {
            boolean shouldIgnite = false;
            switch (targetType) {
                case "OWNER":
                    if (owner != null && entity.getUniqueId().equals(owner.getUniqueId())) {
                        shouldIgnite = true;
                    }
                    break;
                case "ALL":
                    shouldIgnite = true;
                    break;
                case "MOBS":
                    if (entity instanceof Monster) {
                        shouldIgnite = true;
                    }
                    break;
                case "DAMAGEABLE":
                    if (entity instanceof Monster || (entity instanceof Player && owner != null && !plugin.getTeamManager().isAlly(owner, (Player) entity))) {
                        shouldIgnite = true;
                    }
                    break;
                case "UNDAMAGEABLE":
                    if (entity instanceof Animals || (entity instanceof Player && owner != null && plugin.getTeamManager().isAlly(owner, (Player) entity))) {
                        shouldIgnite = true;
                    }
                    break;
            }

            if (shouldIgnite) {
                entity.setFireTicks(durationTicks);
            }
        }
    }
}