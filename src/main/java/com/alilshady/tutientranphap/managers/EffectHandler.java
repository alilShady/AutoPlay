package com.alilshady.tutientranphap.managers;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EffectHandler {
    private final TuTienTranPhap plugin;
    private final Map<Location, BukkitTask> activeEffectTasks = new HashMap<>();

    public EffectHandler(TuTienTranPhap plugin) {
        this.plugin = plugin;
    }

    public void startFormationEffects(Formation formation, Location center) {
        BukkitTask task = new BukkitRunnable() {
            int ticksLived = 0;

            @Override
            public void run() {
                if (ticksLived >= formation.getDurationSeconds() * 20) {
                    // Hết thời gian, dừng hiệu ứng
                    stopEffect(center);
                    World world = center.getWorld();
                    if (world != null) {
                        world.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
                        world.spawnParticle(Particle.SMOKE_NORMAL, center.clone().add(0.5, 1, 0.5), 50);
                    }
                    return;
                }

                // Áp dụng các hiệu ứng mỗi giây (20 ticks)
                if (ticksLived % 20 == 0) {
                    applyEffects(formation, center);
                }

                ticksLived++;
            }
        }.runTaskTimer(plugin, 0L, 1L); // Chạy mỗi tick

        activeEffectTasks.put(center, task);
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
    }

    private void applyEffects(Formation formation, Location center) {
        for (Map<?, ?> effectMap : formation.getEffects()) {
            String type = (String) effectMap.get("type");
            switch (type.toUpperCase()) {
                case "POTION":
                    applyPotionEffect(formation, center, effectMap);
                    break;
                case "PARTICLE_SHIELD":
                    applyParticleShield(formation, center, effectMap);
                    break;
                case "CROP_GROWTH":
                    // Logic này có thể được tối ưu hóa, hiện tại chỉ là ví dụ
                    // applyCropGrowth(formation, center, effectMap);
                    break;
                // Thêm các case khác ở đây
            }
        }
    }

    private void applyPotionEffect(Formation formation, Location center, Map<?, ?> effectMap) {
        String targetType = ((String) effectMap.get("target")).toUpperCase();
        PotionEffectType potionType = PotionEffectType.getByName(((String) effectMap.get("potion_effect")).toUpperCase());
        int amplifier = (int) effectMap.get("amplifier");

        if (potionType == null) return;

        for (Entity entity : Objects.requireNonNull(center.getWorld()).getNearbyEntities(center, formation.getRadius(), formation.getRadius(), formation.getRadius())) {
            if (entity instanceof LivingEntity) {
                boolean apply = false;
                if (targetType.equals("PLAYERS") && entity instanceof Player) apply = true;
                if (targetType.equals("HOSTILE_MOBS") && entity instanceof Monster) apply = true;
                if (targetType.equals("ALL")) apply = true;

                if (apply) {
                    ((LivingEntity) entity).addPotionEffect(new PotionEffect(potionType, 40, amplifier, true, false)); // 2 giây
                }
            }
        }
    }

    private void applyParticleShield(Formation formation, Location center, Map<?, ?> effectMap) {
        // Ví dụ đơn giản vẽ một vòng tròn hạt
        Particle particle = Particle.valueOf(((String) effectMap.get("particle_type")).toUpperCase());
        World world = center.getWorld();
        if (world == null) return;

        for (int i = 0; i < 360; i += 10) {
            double angle = Math.toRadians(i);
            double x = center.getX() + 0.5 + formation.getRadius() * Math.cos(angle);
            double z = center.getZ() + 0.5 + formation.getRadius() * Math.sin(angle);
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
        for (BukkitTask task : activeEffectTasks.values()) {
            task.cancel();
        }
        activeEffectTasks.clear();
    }
}