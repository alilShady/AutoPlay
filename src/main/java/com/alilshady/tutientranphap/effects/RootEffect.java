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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RootEffect implements FormationEffect {

    @Override
    public String getType() {
        return "ROOT";
    }

    @Override
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyEntities == null) return;

        int durationTicks = (int) EffectUtils.parseDurationToTicks(EffectUtils.getStringFromConfig(config, "duration", "40t"));
        int amplifier = EffectUtils.getIntFromConfig(config, "value", 7);
        PotionEffect rootEffect = new PotionEffect(PotionEffectType.SLOW, durationTicks, amplifier, true, false);
        String targetType = EffectUtils.getStringFromConfig(config, "target", "DAMAGEABLE").toUpperCase();
        Player owner = (ownerId != null) ? Bukkit.getPlayer(ownerId) : null;

        for (LivingEntity entity : nearbyEntities) {
            boolean shouldRoot = false;
            switch (targetType) {
                case "OWNER":
                    if (owner != null && entity.getUniqueId().equals(owner.getUniqueId())) {
                        shouldRoot = true;
                    }
                    break;
                case "ALL":
                    shouldRoot = true;
                    break;
                case "MOBS":
                    if (entity instanceof Monster) {
                        shouldRoot = true;
                    }
                    break;
                case "DAMAGEABLE":
                    if (entity instanceof Monster || (entity instanceof Player && owner != null && !plugin.getTeamManager().isAlly(owner, (Player) entity))) {
                        shouldRoot = true;
                    }
                    break;
                case "UNDAMAGEABLE":
                    if (entity instanceof Animals || (entity instanceof Player && owner != null && plugin.getTeamManager().isAlly(owner, (Player) entity))) {
                        shouldRoot = true;
                    }
                    break;
            }

            if (shouldRoot) {
                entity.addPotionEffect(rootEffect);
            }
        }
    }
}