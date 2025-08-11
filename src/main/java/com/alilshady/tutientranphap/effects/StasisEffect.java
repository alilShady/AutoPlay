package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StasisEffect implements FormationEffect {

    @Override
    public String getType() {
        return "STASIS";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyEntities == null) return;

        int slowAmplifier = EffectUtils.getIntFromConfig(config, "value", 3);
        int durationTicks = plugin.getConfigManager().getEffectCheckInterval() + 40;
        String targetType = EffectUtils.getStringFromConfig(config, "target", "DAMAGEABLE").toUpperCase();
        PotionEffect slowEffect = new PotionEffect(PotionEffectType.SLOW, durationTicks, slowAmplifier - 1, true, false);
        Player owner = (ownerId != null) ? Bukkit.getPlayer(ownerId) : null;

        for (LivingEntity entity : nearbyEntities) {
            if (entity instanceof Player && ((Player) entity).getGameMode() != GameMode.SURVIVAL) {
                continue;
            }

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
                entity.addPotionEffect(slowEffect);
            }
        }
    }

    public void applyToProjectiles(World world, Location center, double radius, int slowAmplifier) {
        if (world == null) return;

        double slowFactor = 1.0 / slowAmplifier;
        Collection<Entity> allEntities = world.getNearbyEntities(center, radius, radius, radius);

        for (Entity entity : allEntities) {
            if (entity instanceof Projectile) {
                Projectile projectile = (Projectile) entity;
                Vector velocity = projectile.getVelocity();
                if (!velocity.isZero()) {
                    projectile.setVelocity(velocity.multiply(slowFactor).add(new Vector(0, 0.03, 0)));
                    world.spawnParticle(Particle.END_ROD, projectile.getLocation(), 1, 0, 0, 0, 0);
                }
            }
        }
    }
}