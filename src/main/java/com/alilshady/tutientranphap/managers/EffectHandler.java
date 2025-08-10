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
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EffectHandler {

    private final TuTienTranPhap plugin;
    private final Map<Location, BukkitTask> activeEffectTasks = new ConcurrentHashMap<>();
    private final Map<Location, Set<UUID>> affectedEntitiesByFormation = new ConcurrentHashMap<>();
    private final Map<Location, Long> animationTickMap = new ConcurrentHashMap<>();

    public EffectHandler(TuTienTranPhap plugin) {
        this.plugin = plugin;
    }

    public void startFormationEffects(Formation formation, Location center) {
        if (activeEffectTasks.containsKey(center)) {
            stopEffect(center);
        }
        final World world = center.getWorld();
        if (world == null) {
            plugin.getLogger().warning("Không thể kích hoạt trận pháp tại một world không tồn tại!");
            return;
        }
        affectedEntitiesByFormation.put(center, new HashSet<>());
        animationTickMap.put(center, 0L);
        BukkitTask task = new BukkitRunnable() {
            private long ticksLived = 0;
            private final long maxDurationTicks = formation.getDurationSeconds() * 20L;
            @Override
            public void run() {
                if (ticksLived >= maxDurationTicks || world.getBlockAt(center).getType() != formation.getCenterBlock()) {
                    removePotionEffects(formation, center);
                    stopEffect(center);
                    world.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
                    world.spawnParticle(Particle.SMOKE_LARGE, center.clone().add(0.5, 1, 0.5), 50, 0.5, 0.5, 0.5, 0);
                    return;
                }
                applyEffects(formation, center);
                animationTickMap.computeIfPresent(center, (k, v) -> v + 1);
                ticksLived++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        activeEffectTasks.put(center, task);
        world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
    }

    private void applyEffects(Formation formation, Location center) {
        World world = center.getWorld();
        if (world == null) return;
        long currentTick = animationTickMap.getOrDefault(center, 0L);
        // Lấy danh sách thực thể và khối chỉ một lần nếu cần
        Collection<LivingEntity> nearbyEntities = null;
        List<Block> nearbyBlocks = null;

        for (Map<?, ?> effectMap : formation.getEffects()) {
            String typeStr = String.valueOf(effectMap.get("type")).toUpperCase();
            switch (typeStr) {
                case "POTION":
                    if (currentTick % plugin.getConfigManager().getEffectCheckInterval() == 0) {
                        if (nearbyEntities == null) nearbyEntities = world.getNearbyLivingEntities(center, formation.getRadius());
                        applyPotionEffect(nearbyEntities, effectMap, center);
                    }
                    break;
                case "PARTICLE_SHIELD":
                    applyCinematicParticleShield(formation, center, effectMap, currentTick);
                    break;
                case "CROP_GROWTH":
                    if (currentTick % plugin.getConfigManager().getEffectCheckInterval() == 0) {
                        if (nearbyBlocks == null) nearbyBlocks = getBlocksInRadius(center, (int) Math.ceil(formation.getRadius()));
                        applyCropGrowth(nearbyBlocks, effectMap);
                    }
                    break;

                // --- THÊM HIỆU ỨNG MỚI TẠI ĐÂY ---
                case "AREA_DAMAGE":
                    if (currentTick % plugin.getConfigManager().getEffectCheckInterval() == 0) {
                        if (nearbyEntities == null) nearbyEntities = world.getNearbyLivingEntities(center, formation.getRadius());
                        applyAreaDamage(nearbyEntities, effectMap);
                    }
                    break;
                case "LIGHTNING_STRIKE":
                    if (currentTick % plugin.getConfigManager().getEffectCheckInterval() == 0) {
                        if (nearbyEntities == null) nearbyEntities = world.getNearbyLivingEntities(center, formation.getRadius());
                        applyLightningStrike(nearbyEntities, effectMap);
                    }
                    break;
                // ------------------------------------

                default:
                    if (currentTick % 200 == 0) {
                        plugin.getLogger().warning("Loại hiệu ứng không xác định '" + typeStr + "' trong trận pháp: " + formation.getId());
                    }
                    break;
            }
        }
    }

    /**
     * Phương thức mới để gọi sét
     * @param entities Các thực thể trong vùng
     * @param config Cấu hình của hiệu ứng
     */
    private void applyLightningStrike(Collection<LivingEntity> entities, Map<?, ?> config) {
        String targetType = getStringFromConfig(config, "target", "HOSTILE_MOBS").toUpperCase();
        boolean visualOnly = getBooleanFromConfig(config, "visual_only", false);

        for (LivingEntity entity : entities) {
            if (entity instanceof Player && ((Player) entity).getGameMode() != GameMode.SURVIVAL) {
                continue;
            }

            boolean shouldStrike = false;
            switch (targetType) {
                case "PLAYERS":
                    if (entity instanceof Player) shouldStrike = true;
                    break;
                case "HOSTILE_MOBS":
                    if (entity instanceof Monster) shouldStrike = true;
                    break;
                case "ALL":
                    shouldStrike = true;
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


    /**
     * Phương thức mới để gây sát thương vùng
     * @param entities Các thực thể trong vùng
     * @param config Cấu hình của hiệu ứng
     */
    private void applyAreaDamage(Collection<LivingEntity> entities, Map<?, ?> config) {
        String targetType = getStringFromConfig(config, "target", "HOSTILE_MOBS").toUpperCase();
        double damage = getDoubleFromConfig(config, "damage", 1.0);

        for (LivingEntity entity : entities) {
            // Không gây sát thương cho chính mình hoặc người chơi ở chế độ sáng tạo/quan sát
            if (entity instanceof Player && ((Player) entity).getGameMode() != GameMode.SURVIVAL) {
                continue;
            }

            boolean shouldDamage = false;
            switch (targetType) {
                case "PLAYERS":
                    if (entity instanceof Player) shouldDamage = true;
                    break;
                case "HOSTILE_MOBS":
                    if (entity instanceof Monster) shouldDamage = true;
                    break;
                case "ALL":
                    shouldDamage = true;
                    break;
            }

            if (shouldDamage) {
                entity.damage(damage);
            }
        }
    }

    private void applyCinematicParticleShield(Formation formation, Location center, Map<?, ?> config, long tick) {
        World world = center.getWorld();
        if (world == null) return;

        double radius = formation.getRadius();
        Location centerPoint = center.clone().add(0.5, 0.1, 0.5);

        double rotationSpeed = getDoubleFromConfig(config, "rotation_speed", 1.5);
        int pillarCount = getIntFromConfig(config, "pillar_count", 5);
        int domeLines = getIntFromConfig(config, "dome_lines", 8);
        int domeDensity = getIntFromConfig(config, "dome_density", 15);
        double rotationAngle = Math.toRadians(tick * rotationSpeed);

        Optional<Particle> mainParticle = getParticleFromConfig(config, "main_particle", tick);
        Optional<Particle> pillarParticle = getParticleFromConfig(config, "pillar_particle", tick);
        Optional<Particle> orbParticle = getParticleFromConfig(config, "orb_particle", tick);
        Optional<Particle> domeParticle = getParticleFromConfig(config, "dome_particle", tick);

        mainParticle.ifPresent(particle -> {
            double angleStep = Math.PI / 32;
            for (double angle = 0; angle < 2 * Math.PI; angle += angleStep) {
                double finalAngle = angle + rotationAngle;
                double x = centerPoint.getX() + radius * Math.cos(finalAngle);
                double z = centerPoint.getZ() + radius * Math.sin(finalAngle);
                world.spawnParticle(particle, x, centerPoint.getY(), z, 1, 0, 0, 0, 0);
            }
        });

        pillarParticle.ifPresent(particle -> {
            for (int i = 0; i < pillarCount; i++) {
                double pillarAngle = (2 * Math.PI / pillarCount) * i + rotationAngle;
                double x = centerPoint.getX() + radius * Math.cos(pillarAngle);
                double z = centerPoint.getZ() + radius * Math.sin(pillarAngle);
                double yOffset = (tick % 40) / 10.0;
                world.spawnParticle(particle, x, centerPoint.getY() + yOffset, z, 1, 0, 0, 0, 0);
            }
        });

        orbParticle.ifPresent(particle -> {
            for (int i = 0; i < 5; i++) {
                Vector dir = new Vector(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).normalize();
                Location spawnLoc = centerPoint.clone().add(0, 1.5, 0).add(dir.multiply(0.7));
                world.spawnParticle(particle, spawnLoc, 1, 0, 0, 0, 0);
            }
        });

        domeParticle.ifPresent(particle -> {
            double progress = (double) (tick % 40) / 40.0;
            for (int i = 0; i < domeLines; i++) {
                double lineAngle = (2 * Math.PI / domeLines) * i + rotationAngle / 2;
                double startX = centerPoint.getX() + radius * Math.cos(lineAngle);
                double startZ = centerPoint.getZ() + radius * Math.sin(lineAngle);
                Location startPoint = new Location(world, startX, centerPoint.getY(), startZ);
                Location topPoint = centerPoint.clone().add(0, radius, 0);
                Vector toTop = topPoint.toVector().subtract(startPoint.toVector());

                for (int j = 0; j < (int)(domeDensity * progress) ; j++) {
                    double t = (double) j / domeDensity;
                    Vector pos = startPoint.toVector().add(toTop.clone().multiply(t));
                    Vector toCenter = centerPoint.toVector().subtract(pos.clone());
                    double curveFactor = Math.sin(t * Math.PI);
                    pos.add(toCenter.normalize().multiply(-curveFactor * (radius/2)));
                    world.spawnParticle(particle, pos.getX(), pos.getY(), pos.getZ(), 1, 0, 0, 0, 0);
                }
            }
        });
    }

    private Optional<Particle> getParticleFromConfig(Map<?, ?> config, String key, long tick) {
        if (config.get(key) instanceof String) {
            String particleName = (String) config.get(key);
            try {
                return Optional.of(Particle.valueOf(particleName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                if (tick % 200 == 0) {
                    plugin.getLogger().warning("Tên particle không hợp lệ cho key '" + key + "': " + particleName);
                }
            }
        }
        return Optional.empty();
    }

    private boolean getBooleanFromConfig(Map<?, ?> config, String key, boolean defaultValue) {
        if (config.get(key) instanceof Boolean) {
            return (Boolean) config.get(key);
        }
        return defaultValue;
    }

    private String getStringFromConfig(Map<?, ?> config, String key, String defaultValue) {
        if (config.get(key) instanceof String) {
            return (String) config.get(key);
        }
        return defaultValue;
    }

    private int getIntFromConfig(Map<?, ?> config, String key, int defaultValue) {
        if (config.get(key) instanceof Number) {
            return ((Number) config.get(key)).intValue();
        }
        return defaultValue;
    }

    private double getDoubleFromConfig(Map<?, ?> config, String key, double defaultValue) {
        if (config.get(key) instanceof Number) {
            return ((Number) config.get(key)).doubleValue();
        }
        return defaultValue;
    }

    private void applyPotionEffect(Collection<LivingEntity> entities, Map<?, ?> config, Location formationCenter) {
        String targetType = getStringFromConfig(config, "target", "PLAYERS").toUpperCase();
        String potionName = getStringFromConfig(config, "potion_effect", "");
        if (potionName.isEmpty()) {
            plugin.getLogger().warning("Hiệu ứng POTION không có 'potion_effect'");
            return;
        }

        int amplifier = getIntFromConfig(config, "amplifier", 0);
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
                Set<UUID> affectedUuids = affectedEntitiesByFormation.get(formationCenter);
                if (affectedUuids != null) {
                    affectedUuids.add(entity.getUniqueId());
                }
            }
        }
    }

    private void applyCropGrowth(List<Block> blocks, Map<?, ?> config) {
        double multiplier = getDoubleFromConfig(config, "multiplier", 1.5);
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
        affectedEntitiesByFormation.remove(center);
        animationTickMap.remove(center);
        plugin.getFormationManager().deactivateFormation(center);
    }

    public void stopAllEffects() {
        new ArrayList<>(activeEffectTasks.keySet()).forEach(this::stopEffect);
        activeEffectTasks.clear();
        affectedEntitiesByFormation.clear();
        animationTickMap.clear();
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

    private void removePotionEffects(Formation formation, Location center) {
        Set<UUID> affectedUuids = affectedEntitiesByFormation.get(center);
        if (affectedUuids == null || affectedUuids.isEmpty()) return;
        List<PotionEffectType> formationPotionTypes = new ArrayList<>();
        for (Map<?, ?> effectMap : formation.getEffects()) {
            if ("POTION".equalsIgnoreCase(String.valueOf(effectMap.get("type")))) {
                String potionName = getStringFromConfig(effectMap, "potion_effect", "");
                if (!potionName.isEmpty()) {
                    PotionEffectType potionType = PotionEffectType.getByName(potionName.toUpperCase());
                    if (potionType != null) {
                        formationPotionTypes.add(potionType);
                    }
                }
            }
        }
        if (formationPotionTypes.isEmpty()) return;
        for (UUID uuid : affectedUuids) {
            LivingEntity entity = Bukkit.getEntity(uuid) instanceof LivingEntity ? (LivingEntity) Bukkit.getEntity(uuid) : null;
            if (entity != null && entity.isValid()) {
                for (PotionEffectType typeToRemove : formationPotionTypes) {
                    if (entity.hasPotionEffect(typeToRemove)) {
                        entity.removePotionEffect(typeToRemove);
                    }
                }
            }
        }
    }
}