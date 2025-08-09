package com.alilshady.tutientranphap.managers;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EffectHandler {

    private final TuTienTranPhap plugin;
    private final Map<Location, BukkitTask> activeEffectTasks = new ConcurrentHashMap<>();

    public EffectHandler(TuTienTranPhap plugin) {
        this.plugin = plugin;
    }

    public void startFormationEffects(Formation formation, Location center) {
        if (activeEffectTasks.containsKey(center)) {
            stopEffect(center);
        }

        final int checkInterval = plugin.getConfigManager().getEffectCheckInterval();
        final World world = center.getWorld();

        if (world == null) {
            plugin.getLogger().warning("Không thể kích hoạt trận pháp tại một world không tồn tại!");
            return;
        }

        BukkitTask task = new BukkitRunnable() {
            private long ticksLived = 0;
            private final long maxDurationTicks = formation.getDurationSeconds() * 20L;

            @Override
            public void run() {
                if (ticksLived >= maxDurationTicks || world.getBlockAt(center).getType() != formation.getCenterBlock()) {
                    stopEffect(center);
                    world.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
                    world.spawnParticle(Particle.SMOKE_LARGE, center.clone().add(0.5, 1, 0.5), 50, 0.5, 0.5, 0.5, 0);
                    return;
                }

                if (ticksLived % checkInterval == 0) {
                    applyEffects(formation, center);
                }

                ticksLived++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeEffectTasks.put(center, task);
        world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
    }

    private void applyEffects(Formation formation, Location center) {
        World world = center.getWorld();
        if (world == null) return;

        List<Map<?, ?>> effectConfigs = formation.getEffects();
        Collection<LivingEntity> nearbyEntities = world.getNearbyLivingEntities(center, formation.getRadius());
        List<Block> nearbyBlocks = getBlocksInRadius(center, (int) Math.ceil(formation.getRadius()));

        for (Map<?, ?> effectMap : effectConfigs) {
            Object typeObj = effectMap.get("type");
            if (!(typeObj instanceof String)) {
                plugin.getLogger().warning("Phát hiện một hiệu ứng không có 'type' hợp lệ trong trận pháp: " + formation.getId());
                continue;
            }
            String typeStr = (String) typeObj;

            switch (typeStr.toUpperCase()) {
                case "POTION":
                    applyPotionEffect(nearbyEntities, effectMap);
                    break;
                case "PARTICLE_SHIELD":
                    applyParticleShield(formation, center, effectMap);
                    break;
                case "CROP_GROWTH":
                    applyCropGrowth(nearbyBlocks, effectMap);
                    break;
                default:
                    plugin.getLogger().warning("Loại hiệu ứng không xác định '" + typeStr + "' trong trận pháp: " + formation.getId());
                    break;
            }
        }
    }

    private void applyPotionEffect(Collection<LivingEntity> entities, Map<?, ?> config) {
        // --- Lấy dữ liệu an toàn ---
        String targetType = "PLAYERS";
        if (config.get("target") instanceof String) {
            targetType = ((String) config.get("target")).toUpperCase();
        }

        if (!(config.get("potion_effect") instanceof String)) {
            plugin.getLogger().warning("Hiệu ứng POTION không có 'potion_effect'");
            return;
        }
        String potionName = (String) config.get("potion_effect");

        int amplifier = 0;
        if (config.get("amplifier") instanceof Number) {
            amplifier = ((Number) config.get("amplifier")).intValue();
        }
        // --- Kết thúc lấy dữ liệu an toàn ---

        int durationTicks = plugin.getConfigManager().getEffectCheckInterval() + 40;
        PotionEffectType potionType = PotionEffectType.getByName(potionName.toUpperCase());
        if (potionType == null) {
            plugin.getLogger().warning("Tên hiệu ứng thuốc không hợp lệ: " + potionName);
            return;
        }

        PotionEffect effect = new PotionEffect(potionType, durationTicks, amplifier, true, false);

        for (LivingEntity entity : entities) {
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
                entity.addPotionEffect(effect);
            }
        }
    }

    private void applyParticleShield(Formation formation, Location center, Map<?, ?> config) {
        if (!(config.get("particle_type") instanceof String)) return;
        String particleName = (String) config.get("particle_type");

        Particle particle;
        try {
            particle = Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Tên particle không hợp lệ: " + particleName);
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

    private void applyCropGrowth(List<Block> blocks, Map<?, ?> config) {
        double multiplier = 1.5;
        if (config.get("multiplier") instanceof Number) {
            multiplier = ((Number) config.get("multiplier")).doubleValue();
        }

        Random random = new Random();
        for (Block block : blocks) {
            if (block.getBlockData() instanceof Ageable) {
                Ageable ageable = (Ageable) block.getBlockData();
                if (ageable.getAge() < ageable.getMaximumAge()) {
                    if (random.nextDouble() < (0.1 * multiplier)) {
                        ageable.setAge(ageable.getAge() + 1);
                        block.setBlockData(ageable);
                    }
                }
            }
        }
    }

    public void stopEffect(Location center) {
        BukkitTask task = activeEffectTasks.remove(center);
        if (task != null) {
            task.cancel();
        }
        plugin.getFormationManager().deactivateFormation(center);
    }

    public void stopAllEffects() {
        new ArrayList<>(activeEffectTasks.keySet()).forEach(this::stopEffect);
        activeEffectTasks.clear();
    }

    private List<Block> getBlocksInRadius(Location center, int radius) {
        List<Block> blocks = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) return blocks;
        int cX = center.getBlockX();
        int cY = center.getBlockY();
        int cZ = center.getBlockZ();
        for (int x = cX - radius; x <= cX + radius; x++) {
            for (int z = cZ - radius; z <= cZ + radius; z++) {
                if (center.distanceSquared(new Location(world, x, cY, z)) <= radius * radius) {
                    blocks.add(world.getBlockAt(x, cY, z));
                }
            }
        }
        return blocks;
    }
}