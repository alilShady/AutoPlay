// src/main/java/com/alilshady/tutientranphap/managers/EffectHandler.java
package com.alilshady.tutientranphap.managers;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.effects.*;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EffectHandler {

    private final TuTienTranPhap plugin;
    private final Map<Location, BukkitTask> activeEffectTasks = new ConcurrentHashMap<>();
    private final Map<Location, Set<UUID>> affectedEntitiesByFormation = new ConcurrentHashMap<>();
    private final Map<Location, Long> animationTickMap = new ConcurrentHashMap<>();

    private final Map<String, FormationEffect> effectStrategies = new HashMap<>();

    public static final List<PotionEffectType> DEBUFFS_TO_CLEANSE = Collections.unmodifiableList(Arrays.asList(
            PotionEffectType.SLOW, PotionEffectType.SLOW_DIGGING, PotionEffectType.WEAKNESS,
            PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.CONFUSION,
            PotionEffectType.BLINDNESS, PotionEffectType.HUNGER, PotionEffectType.LEVITATION
    ));

    public EffectHandler(TuTienTranPhap plugin) {
        this.plugin = plugin;
        registerEffectStrategies();
    }

    private void registerEffectStrategies() {
        Stream.of(
                new PotionEffectStrategy(), new DamageEffect(), new HealEffect(),
                new LightningStrikeEffect(), new MobRepulsionEffect(), new RootEffect(),
                new CleanseEffect(), new ItemRepairEffect(), new XpOrbGenerationEffect(),
                new FurnaceBoostEffect(), new CropGrowthEffect(), new HarvestEffect(),
                new FreezeLiquidsEffect(), new CollectEffect(), new BreedEffect(),
                new VortexEffect(), new StasisEffect(), new ExplosionEffect(),
                new IgniteEffect(), new ClimateEffect(), new BarrierEffect(),
                new DevolveEffect()
        ).forEach(strategy -> effectStrategies.put(strategy.getType(), strategy));
    }

    public void startFormationEffects(Formation formation, Location center, UUID ownerId) {
        if (activeEffectTasks.containsKey(center)) {
            stopEffect(center);
        }
        final World world = center.getWorld();
        if (world == null) return;

        affectedEntitiesByFormation.put(center, new HashSet<>());
        animationTickMap.put(center, 0L);

        final long maxDurationTicks = EffectUtils.parseDurationToTicks(formation.getDuration());

        BukkitTask task = new BukkitRunnable() {
            private long ticksLived = 0;

            @Override
            public void run() {
                if ((maxDurationTicks > 0 && ticksLived >= maxDurationTicks) || world.getBlockAt(center).getType() != formation.getCenterBlock()) {
                    stopAndCleanup();
                    return;
                }

                applyEffects(formation, center, ownerId);
                animationTickMap.computeIfPresent(center, (k, v) -> v + 1);
                ticksLived++;
            }

            private void stopAndCleanup() {
                this.cancel();
                removePotionEffects(formation, center);
                stopEffect(center);
                world.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
                world.spawnParticle(Particle.SMOKE_LARGE, center.clone().add(0.5, 1, 0.5), 50, 0.5, 0.5, 0.5, 0);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeEffectTasks.put(center, task);
        world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
    }

    private void applyEffects(Formation formation, Location center, UUID ownerId) {
        World world = center.getWorld();
        if (world == null) return;
        long currentTick = animationTickMap.getOrDefault(center, 0L);

        Map<String, Object> particleConfig = formation.getParticleConfig();
        if (particleConfig != null && !particleConfig.isEmpty()) {
            applyCinematicParticles(formation, center, particleConfig, currentTick);
        }

        Collection<LivingEntity> allNearbyLivingEntities = world.getNearbyLivingEntities(center, formation.getRadius());

        for (Map<?, ?> effectMap : formation.getEffects()) {
            String typeStr = String.valueOf(effectMap.get("type")).toUpperCase();
            FormationEffect strategy = effectStrategies.get(typeStr);

            if (strategy instanceof StasisEffect) {
                int slowAmplifier = EffectUtils.getIntFromConfig(effectMap, "value", 3);
                ((StasisEffect) strategy).applyToProjectiles(world, center, formation.getRadius(), slowAmplifier);
            }

            if (strategy instanceof BarrierEffect) {
                ((BarrierEffect) strategy).applyBarrierPush(plugin, formation, center, effectMap, allNearbyLivingEntities, ownerId);
            }
        }

        if (currentTick % plugin.getConfigManager().getEffectCheckInterval() != 0) {
            return;
        }

        List<Block> nearbyBlocks = getBlocksInRadius(center, (int) Math.ceil(formation.getRadius()));
        final boolean isDebug = plugin.getConfigManager().isDebugLoggingEnabled();

        for (Map<?, ?> effectMap : formation.getEffects()) {
            String typeStr = String.valueOf(effectMap.get("type")).toUpperCase();
            FormationEffect strategy = effectStrategies.get(typeStr);

            if (strategy != null) {
                if (isDebug) plugin.getLogger().info("[DEBUG][EFFECT] Running effect: " + typeStr);
                strategy.apply(plugin, formation, center, effectMap, allNearbyLivingEntities, nearbyBlocks, ownerId);
            } else {
                if (isDebug && currentTick % 200 == 0) {
                    plugin.getLogger().warning("Loại hiệu ứng không xác định '" + typeStr + "' trong trận pháp: " + formation.getId());
                }
            }
        }
    }

    public void trackAffectedEntity(Location center, UUID entityId) {
        affectedEntitiesByFormation.computeIfAbsent(center, k -> new HashSet<>()).add(entityId);
    }

    public void stopEffect(Location center) {
        BukkitTask task = activeEffectTasks.remove(center);
        if (task != null && !task.isCancelled()) {
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
        int cX = center.getBlockX(), cY = center.getBlockY(), cZ = center.getBlockZ();
        int radiusSquared = radius * radius;

        for (int x = cX - radius; x <= cX + radius; x++) {
            for (int z = cZ - radius; z <= cZ + radius; z++) {
                if (center.distanceSquared(new Location(world, x, cY, z)) <= radiusSquared) {
                    blocks.add(world.getBlockAt(x, cY, z));
                }
            }
        }
        return blocks;
    }

    private void removePotionEffects(Formation formation, Location center) {
        Set<UUID> affectedUuids = affectedEntitiesByFormation.get(center);
        if (affectedUuids == null || affectedUuids.isEmpty()) return;

        Set<PotionEffectType> formationPotionTypes = formation.getEffects().stream()
                .filter(map -> "POTION".equalsIgnoreCase(String.valueOf(map.get("type"))))
                .map(map -> EffectUtils.getStringFromConfig(map, "potion_effect", ""))
                .filter(name -> !name.isEmpty())
                .map(name -> PotionEffectType.getByName(name.toUpperCase()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (formationPotionTypes.isEmpty()) return;

        for (UUID uuid : affectedUuids) {
            LivingEntity entity = (LivingEntity) Bukkit.getEntity(uuid);
            if (entity != null && entity.isValid()) {
                formationPotionTypes.forEach(entity::removePotionEffect);
            }
        }
    }

    private void applyCinematicParticles(Formation formation, Location center, Map<String, Object> config, long tick) {
        World world = center.getWorld();
        if (world == null) return;
        Location centerPoint = center.clone().add(0.5, 0.1, 0.5);

        Object shapesObject = config.get("shapes");
        if (!(shapesObject instanceof List)) {
            return;
        }

        List<?> shapeConfigs = (List<?>) shapesObject;

        for (Object shapeConfigObj : shapeConfigs) {
            if (!(shapeConfigObj instanceof Map)) continue;
            Map<?, ?> shapeConfig = (Map<?, ?>) shapeConfigObj;

            String type = EffectUtils.getStringFromConfig(shapeConfig, "type", "").toUpperCase();
            Particle particle = Optional.ofNullable(EffectUtils.getStringFromConfig(shapeConfig, "particle", null))
                    .map(s -> {
                        try {
                            return Particle.valueOf(s.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    }).orElse(null);

            if (particle == null) continue;

            double radius = EffectUtils.getDoubleFromConfig(shapeConfig, "radius", formation.getRadius());
            double rotationSpeed = EffectUtils.getDoubleFromConfig(shapeConfig, "rotation_speed", 0.0);
            double rotationAngle = Math.toRadians(tick * rotationSpeed);

            switch (type) {
                case "RING":
                    drawRing(world, centerPoint, particle, radius, rotationAngle, shapeConfig);
                    break;
                case "PILLAR":
                    drawPillars(world, centerPoint, particle, radius, rotationAngle, shapeConfig);
                    break;
                case "SPHERE":
                    drawSphere(world, centerPoint, particle, shapeConfig);
                    break;
                case "DOME":
                    drawDome(world, centerPoint, particle, radius, rotationAngle, shapeConfig, tick);
                    break;
                case "HELIX":
                    drawHelix(world, centerPoint, particle, radius, rotationAngle, shapeConfig);
                    break;
                case "VORTEX":
                    drawVortex(world, centerPoint, particle, radius, rotationAngle, shapeConfig);
                    break;
                case "RAIN":
                    drawRain(world, centerPoint, particle, radius, shapeConfig);
                    break;
            }
        }
    }

    private void drawRing(World world, Location center, Particle particle, double radius, double rotationAngle, Map<?, ?> config) {
        int density = EffectUtils.getIntFromConfig(config, "density", 64);
        double yOffset = EffectUtils.getDoubleFromConfig(config, "y-offset", 0.1);
        for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / (density / 2.0)) {
            double x = center.getX() + radius * Math.cos(angle + rotationAngle);
            double z = center.getZ() + radius * Math.sin(angle + rotationAngle);
            world.spawnParticle(particle, x, center.getY() + yOffset, z, 1, 0, 0, 0, 0);
        }
    }

    private void drawPillars(World world, Location center, Particle particle, double radius, double rotationAngle, Map<?, ?> config) {
        int count = EffectUtils.getIntFromConfig(config, "count", 4);
        double height = EffectUtils.getDoubleFromConfig(config, "height", 4.0);
        int density = EffectUtils.getIntFromConfig(config, "density", 20);
        for (int i = 0; i < count; i++) {
            double pillarAngle = (2 * Math.PI / count) * i + rotationAngle;
            double x = center.getX() + radius * Math.cos(pillarAngle);
            double z = center.getZ() + radius * Math.sin(pillarAngle);
            for (double y = 0; y < height; y += height / density) {
                world.spawnParticle(particle, x, center.getY() + y, z, 1, 0, 0, 0, 0);
            }
        }
    }

    private void drawSphere(World world, Location center, Particle particle, Map<?, ?> config) {
        double radius = EffectUtils.getDoubleFromConfig(config, "radius", 1.5);
        int density = EffectUtils.getIntFromConfig(config, "density", 100);
        double yOffset = EffectUtils.getDoubleFromConfig(config, "y-offset", 1.5);
        Location sphereCenter = center.clone().add(0, yOffset, 0);

        for (int i = 0; i < density; i++) {
            Vector dir = new Vector(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).normalize().multiply(radius);
            world.spawnParticle(particle, sphereCenter.clone().add(dir), 1, 0, 0, 0, 0);
        }
    }

    private void drawDome(World world, Location center, Particle particle, double radius, double rotationAngle, Map<?, ?> config, long tick) {
        int lines = EffectUtils.getIntFromConfig(config, "lines", 8);
        int density = EffectUtils.getIntFromConfig(config, "density", 200);

        for (int i = 0; i < lines; i++) {
            double lineAngle = (2 * Math.PI / lines) * i + rotationAngle;
            Location startPoint = new Location(world, center.getX() + radius * Math.cos(lineAngle), center.getY(), center.getZ() + radius * Math.sin(lineAngle));
            Location topPoint = center.clone().add(0, radius, 0);
            Vector toTop = topPoint.toVector().subtract(startPoint.toVector());

            int particlesPerLine = density / lines;
            for (int j = 0; j < particlesPerLine; j++) {
                double t = (double) j / particlesPerLine;
                Vector pos = startPoint.toVector().add(toTop.clone().multiply(t));
                Vector toCenter = center.toVector().subtract(pos.clone());
                pos.add(toCenter.normalize().multiply(-Math.sin(t * Math.PI) * (radius / 2)));
                world.spawnParticle(particle, pos.getX(), pos.getY(), pos.getZ(), 1, 0, 0, 0, 0);
            }
        }
    }

    private void drawHelix(World world, Location center, Particle particle, double radius, double rotationAngle, Map<?, ?> config) {
        double height = EffectUtils.getDoubleFromConfig(config, "height", 5.0);
        int density = EffectUtils.getIntFromConfig(config, "density", 100);
        int strands = EffectUtils.getIntFromConfig(config, "strands", 2); // Số chuỗi xoắn
        double yOffset = EffectUtils.getDoubleFromConfig(config, "y-offset", 0.1);
        double revolutions = EffectUtils.getDoubleFromConfig(config, "revolutions", 2.0); // Số vòng xoắn

        for (int i = 0; i < density; i++) {
            double y = (height / density) * i;
            double angle = revolutions * 2 * Math.PI * (y / height); // Góc xoay dựa trên chiều cao

            for (int s = 0; s < strands; s++) {
                double strandOffset = (2 * Math.PI / strands) * s;
                double x = center.getX() + radius * Math.cos(angle + rotationAngle + strandOffset);
                double z = center.getZ() + radius * Math.sin(angle + rotationAngle + strandOffset);
                world.spawnParticle(particle, x, center.getY() + y + yOffset, z, 1, 0, 0, 0, 0);
            }
        }
    }

    private void drawVortex(World world, Location center, Particle particle, double radius, double rotationAngle, Map<?, ?> config) {
        double height = EffectUtils.getDoubleFromConfig(config, "height", 5.0);
        int density = EffectUtils.getIntFromConfig(config, "density", 150);
        double yOffset = EffectUtils.getDoubleFromConfig(config, "y-offset", 0.1);

        for (int i = 0; i < density; i++) {
            double progress = (double) i / density;
            double currentRadius = radius * (1 - progress); // Bán kính nhỏ dần về phía dưới
            double y = height * (1 - progress);
            double angle = progress * 4 * Math.PI; // Xoay nhiều vòng

            double x = center.getX() + currentRadius * Math.cos(angle + rotationAngle);
            double z = center.getZ() + currentRadius * Math.sin(angle + rotationAngle);
            world.spawnParticle(particle, x, center.getY() + y + yOffset, z, 1, 0, 0, 0, 0);
        }
    }

    private void drawRain(World world, Location center, Particle particle, double radius, Map<?, ?> config) {
        int density = EffectUtils.getIntFromConfig(config, "density", 10); // Mật độ mưa mỗi tick
        double height = EffectUtils.getDoubleFromConfig(config, "height", 10.0);
        Random random = new Random();

        for (int i = 0; i < density; i++) {
            double x = center.getX() + (random.nextDouble() - 0.5) * (radius * 2);
            double z = center.getZ() + (random.nextDouble() - 0.5) * (radius * 2);
            // Chỉ spawn bên trong vòng tròn
            if (center.distanceSquared(new Location(world, x, center.getY(), z)) <= radius * radius) {
                world.spawnParticle(particle, x, center.getY() + height, z, 1, 0, 0, 0, 0);
            }
        }
    }
}