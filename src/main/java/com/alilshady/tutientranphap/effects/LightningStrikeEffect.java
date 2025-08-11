package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
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

public class LightningStrikeEffect implements FormationEffect {

    @Override
    public String getType() {
        return "LIGHTNING";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyEntities == null) return;

        boolean visualOnly = EffectUtils.getBooleanFromConfig(config, "visual_only", false);
        String targetType = EffectUtils.getStringFromConfig(config, "target", "DAMAGEABLE").toUpperCase();
        Player owner = (ownerId != null) ? Bukkit.getPlayer(ownerId) : null;

        for (LivingEntity entity : nearbyEntities) {
            if (entity instanceof Player && ((Player) entity).getGameMode() != GameMode.SURVIVAL) continue;

            boolean shouldStrike = false;
            switch (targetType) {
                case "OWNER":
                    if (owner != null && entity.getUniqueId().equals(owner.getUniqueId())) {
                        shouldStrike = true;
                    }
                    break;
                case "ALL":
                    shouldStrike = true;
                    break;
                case "MOBS":
                    if (entity instanceof Monster) {
                        shouldStrike = true;
                    }
                    break;
                case "DAMAGEABLE":
                    if (entity instanceof Monster || (entity instanceof Player && owner != null && !plugin.getTeamManager().isAlly(owner, (Player) entity))) {
                        shouldStrike = true;
                    }
                    break;
                case "UNDAMAGEABLE":
                    if (entity instanceof Animals || (entity instanceof Player && owner != null && plugin.getTeamManager().isAlly(owner, (Player) entity))) {
                        shouldStrike = true;
                    }
                    break;
            }

            if (shouldStrike) {
                if (visualOnly) {
                    entity.getWorld().strikeLightningEffect(entity.getLocation());
                } else {
                    entity.getWorld().strikeLightning(entity.getLocation());
                }
            }
        }
    }
}