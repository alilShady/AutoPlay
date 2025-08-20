package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.EssenceArrays;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ExplosionEffect implements FormationEffect {

    @Override
    public String getType() {
        return "EXPLOSION";
    }

    @Override
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyEntities == null) return;
        World world = center.getWorld();
        if (world == null) return;

        float power = (float) EffectUtils.getDoubleFromConfig(config, "value", 2.0);
        String targetType = EffectUtils.getStringFromConfig(config, "target", "DAMAGEABLE").toUpperCase();
        Player owner = (ownerId != null) ? Bukkit.getPlayer(ownerId) : null;

        for (LivingEntity entity : nearbyEntities) {
            boolean shouldExplode = false;
            switch (targetType) {
                case "OWNER":
                    if (owner != null && entity.getUniqueId().equals(owner.getUniqueId())) {
                        shouldExplode = true;
                    }
                    break;
                case "ALL":
                    shouldExplode = true;
                    break;
                case "MOBS":
                    if (entity instanceof Monster) {
                        shouldExplode = true;
                    }
                    break;
                case "DAMAGEABLE":
                    if (entity instanceof Monster || (entity instanceof Player && owner != null && !plugin.getTeamManager().isAlly(owner, (Player) entity))) {
                        shouldExplode = true;
                    }
                    break;
                case "UNDAMAGEABLE":
                    if (entity instanceof Animals || (entity instanceof Player && owner != null && plugin.getTeamManager().isAlly(owner, (Player) entity))) {
                        shouldExplode = true;
                    }
                    break;
            }

            if (shouldExplode) {
                world.createExplosion(entity.getLocation(), power, true, false);
            }
        }
    }
}