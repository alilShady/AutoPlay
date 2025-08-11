package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
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

public class PotionEffectStrategy implements FormationEffect {

    @Override
    public String getType() {
        return "POTION";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyEntities == null) return;

        String potionName = EffectUtils.getStringFromConfig(config, "potion_effect", "");
        if (potionName.isEmpty()) return;

        PotionEffectType potionType = PotionEffectType.getByName(potionName.toUpperCase());
        if (potionType == null) return;

        int amplifier = EffectUtils.getIntFromConfig(config, "value", 0);
        int durationTicks = plugin.getConfigManager().getEffectCheckInterval() + 40;
        PotionEffect effect = new PotionEffect(potionType, durationTicks, amplifier, true, false);

        String targetType = EffectUtils.getStringFromConfig(config, "target", "OWNER").toUpperCase();
        Player owner = (ownerId != null) ? Bukkit.getPlayer(ownerId) : null;

        for (LivingEntity entity : nearbyEntities) {
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
                entity.addPotionEffect(effect);
                plugin.getEffectHandler().trackAffectedEntity(center, entity.getUniqueId());
            }
        }
    }
}