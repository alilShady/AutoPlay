package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.EssenceArrays;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
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
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyEntities == null) return;

        int slowAmplifier = EffectUtils.getIntFromConfig(config, "value", 3);
        int durationTicks = plugin.getConfigManager().getEffectCheckInterval() + 40;
        String targetType = EffectUtils.getStringFromConfig(config, "target", "DAMAGEABLE").toUpperCase();
        PotionEffect slowEffect = new PotionEffect(PotionEffectType.SLOW, durationTicks, slowAmplifier - 1, true, false);

        for (LivingEntity entity : nearbyEntities) {
            if (EffectUtils.shouldApplyToEntity(plugin, entity, targetType, ownerId)) {
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
                    world.spawnParticle(org.bukkit.Particle.END_ROD, projectile.getLocation(), 1, 0, 0, 0, 0);
                }
            }
        }
    }
}