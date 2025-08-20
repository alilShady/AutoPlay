package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.EssenceArrays;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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

public class DamageEffect implements FormationEffect {

    @Override
    public String getType() {
        return "DAMAGE";
    }

    @Override
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyEntities == null) return;

        double damage = EffectUtils.getDoubleFromConfig(config, "value", 1.0);
        String targetType = EffectUtils.getStringFromConfig(config, "target", "MOBS").toUpperCase();
        Player owner = (ownerId != null) ? Bukkit.getPlayer(ownerId) : null;

        for (LivingEntity entity : nearbyEntities) {
            if (entity instanceof Player && ((Player) entity).getGameMode() != GameMode.SURVIVAL) continue;

            boolean shouldApply = false;
            switch (targetType) {
                case "OWNER":
                    if (owner != null && entity.getUniqueId().equals(owner.getUniqueId())) {
                        shouldApply = true;
                    }
                    break;
                case "ALL":
                    shouldApply = true;
                    break;
                case "MOBS":
                    if (entity instanceof Monster) {
                        shouldApply = true;
                    }
                    break;
                case "DAMAGEABLE":
                    if (entity instanceof Monster || (entity instanceof Player && owner != null && !plugin.getTeamManager().isAlly(owner, (Player) entity))) {
                        shouldApply = true;
                    }
                    break;
                case "UNDAMAGEABLE":
                    if (entity instanceof Animals || (entity instanceof Player && owner != null && plugin.getTeamManager().isAlly(owner, (Player) entity))) {
                        shouldApply = true;
                    }
                    break;
            }

            if (shouldApply) {
                entity.damage(damage);
            }
        }
    }
}