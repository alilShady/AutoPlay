package com.alilshady.tutientranphap.managers;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class EffectHandler {
    private final TuTienTranPhap plugin;
    private final Map<Location, BukkitTask> activeEffectTasks = new HashMap<>();

    public EffectHandler(TuTienTranPhap plugin) {
        this.plugin = plugin;
    }

    public void startFormationEffects(Formation formation, Location center) {
        final int checkInterval = plugin.getConfigManager().getEffectCheckInterval();

        BukkitTask task = new BukkitRunnable() {
            int ticksLived = 0;

            @Override
            public void run() {
                if (center.getWorld() == null || center.getBlock().getType() != formation.getCenterBlock()) {
                    stopEffect(center);
                    return;
                }

                if (ticksLived >= formation.getDurationSeconds() * 20) {
                    stopEffect(center);
                    World world = center.getWorld();
                    if (world != null) {
                        world.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
                        world.spawnParticle(Particle.SMOKE_NORMAL, center.clone().add(0.5, 1, 0.5), 50);
                    }
                    return;
                }

                if (ticksLived % checkInterval == 0) {
                    applyEffects(formation, center);
                }

                ticksLived++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeEffectTasks.put(center, task);
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
    }

    private void applyEffects(Formation formation, Location center) {
        World world = center.getWorld();
        if (world == null) return;

        Collection<LivingEntity> nearbyEntities = world.getNearbyLivingEntities(center, formation.getRadius());

        for (Map<?, ?> effectMap : formation.getEffects()) {
            String type = (String) effectMap.get("type");
            if (type == null) continue;

            switch (type.toUpperCase()) {
                case "POTION":
                    applyPotionEffect(nearbyEntities, effectMap);
                    break;
                case "PARTICLE_SHIELD":
                    applyParticleShield(formation, center, effectMap);
                    break;
                case "CROP_GROWTH":
                    break;
            }
        }
    }

    private void applyPotionEffect(Collection<LivingEntity> entities, Map<?, ?> effectMap) {
        String targetType = ((String) effectMap.get("target")).toUpperCase();
        PotionEffectType potionType = PotionEffectType.getByName(((String) effectMap.get("potion_effect")).toUpperCase());
        int amplifier = ((Number) effectMap.getOrDefault("amplifier", 0)).intValue();
        int durationTicks = plugin.getConfigManager().getEffectCheckInterval() + 40;

        if (potionType == null) return;

        // Tạo PotionEffect với 3 tham số cơ bản
        PotionEffect effect = new PotionEffect(potionType, durationTicks, amplifier);

        for (LivingEntity entity : entities) {
            boolean apply = false;
            switch(targetType) {
                case "PLAYERS":
                    if (entity instanceof Player) apply = true;
                    break;
                case "HOSTILE_MOBS":
                    if (entity instanceof Monster) apply = true;
                    break;
                case "ALL":
                    apply = true;
                    break;
            }

            if (apply) {
                // SỬA LỖI TẠI ĐÂY:
                // Sử dụng phương thức addPotionEffect(effect, force)
                // Tham số `true` sẽ ghi đè hiệu ứng cũ, đảm bảo hiệu ứng luôn được áp dụng đúng cách
                entity.addPotionEffect(effect, true);
            }
        }
    }

    private void applyParticleShield(Formation formation, Location center, Map<?, ?> effectMap) {
        Particle particle;
        try {
            particle = Particle.valueOf(((String) effectMap.get("particle_type")).toUpperCase());
        } catch (IllegalArgumentException e) {
            return;
        }

        World world = center.getWorld();
        if (world == null) return;

        double radius = formation.getRadius();
        for (int i = 0; i < 360; i += 15) {
            double angle = Math.toRadians(i);
            double x = center.getX() + 0.5 + radius * Math.cos(angle);
            double z = center.getZ() + 0.5 + radius * Math.sin(angle);
            world.spawnParticle(particle, x, center.getY() + 1, z, 1, 0, 0, 0, 0);
        }
    }

    public void stopEffect(Location center) {
        if (activeEffectTasks.containsKey(center)) {
            activeEffectTasks.get(center).cancel();
            activeEffectTasks.remove(center);
            plugin.getFormationManager().deactivateFormation(center);
        }
    }

    public void stopAllEffects() {
        new ArrayList<>(activeEffectTasks.keySet()).forEach(this::stopEffect);
        activeEffectTasks.clear();
    }
}