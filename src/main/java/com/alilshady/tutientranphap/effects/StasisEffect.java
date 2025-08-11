// src/main/java/com/alilshady/tutientranphap/effects/StasisEffect.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class StasisEffect implements FormationEffect {

    @Override
    public String getType() {
        return "STASIS";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        if (nearbyEntities == null) return;

        // --- Logic làm chậm Thực thể sống (LivingEntity) ---
        int slowAmplifier = EffectUtils.getIntFromConfig(config, "value", 3);
        int durationTicks = plugin.getConfigManager().getEffectCheckInterval() + 40;
        String targetType = EffectUtils.getStringFromConfig(config, "target", "HOSTILE_MOBS").toUpperCase();
        PotionEffect slowEffect = new PotionEffect(PotionEffectType.SLOW, durationTicks, slowAmplifier - 1, true, false);

        for (LivingEntity entity : nearbyEntities) {
            if (entity instanceof Player && ((Player) entity).getGameMode() != GameMode.SURVIVAL) {
                continue;
            }

            boolean shouldApply = false;
            switch (targetType) {
                case "PLAYERS":
                    if (entity instanceof Player) shouldApply = true;
                    break;
                case "HOSTILE_MOBS":
                    if (entity instanceof Monster) shouldApply = true;
                    break;
                case "ALL":
                    shouldApply = true;
                    break;
            }

            if (shouldApply) {
                entity.addPotionEffect(slowEffect);
            }
        }

        // --- Logic làm chậm Vật thể bay đã được chuyển đi ---
        // Logic này giờ sẽ được gọi mỗi tick từ EffectHandler
    }

    /**
     * Phương thức mới để xử lý vật thể bay, sẽ được gọi mỗi tick.
     */
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