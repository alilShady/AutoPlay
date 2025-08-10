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

    // Map để lưu trữ các chiến lược hiệu ứng (Strategy Pattern)
    private final Map<String, FormationEffect> effectStrategies = new HashMap<>();

    // Danh sách các debuff sẽ bị xóa bởi hiệu ứng CLEANSE
    public static final List<PotionEffectType> DEBUFFS_TO_CLEANSE = Collections.unmodifiableList(Arrays.asList(
            PotionEffectType.SLOW, PotionEffectType.SLOW_DIGGING, PotionEffectType.WEAKNESS,
            PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.CONFUSION,
            PotionEffectType.BLINDNESS, PotionEffectType.HUNGER, PotionEffectType.LEVITATION
    ));

    public EffectHandler(TuTienTranPhap plugin) {
        this.plugin = plugin;
        registerEffectStrategies();
    }

    /**
     * Đăng ký tất cả các lớp hiệu ứng (chiến lược) vào map.
     * Khi cần thêm hiệu ứng mới, chỉ cần thêm vào stream này.
     */
    private void registerEffectStrategies() {
        Stream.of(
                new PotionEffectStrategy(),
                new DamageEffect(),
                new HealEffect(),
                new LightningStrikeEffect(),
                new MobRepulsionEffect(),
                new RootEffect(),
                new CleanseEffect(),
                new ItemRepairEffect(),
                new XpOrbGenerationEffect(),
                new FurnaceBoostEffect(),
                new CropGrowthEffect(),
                new HarvestEffect(),
                new FreezeLiquidsEffect(),
                new CollectEffect(),
                new BreedEffect(),
                new VortexEffect(),
                new StasisEffect(),
                new ExplosionEffect(),
                new IgniteEffect(),
                new ClimateEffect(),
                new BarrierEffect()
        ).forEach(strategy -> effectStrategies.put(strategy.getType(), strategy));
    }

    /**
     * Bắt đầu vòng lặp hiệu ứng cho một trận pháp tại một vị trí.
     */
    public void startFormationEffects(Formation formation, Location center) {
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
                // Kiểm tra điều kiện dừng: hết thời gian hoặc khối trung tâm bị phá
                if ((maxDurationTicks > 0 && ticksLived >= maxDurationTicks) || world.getBlockAt(center).getType() != formation.getCenterBlock()) {
                    stopAndCleanup();
                    return;
                }

                applyEffects(formation, center);
                animationTickMap.computeIfPresent(center, (k, v) -> v + 1);
                ticksLived++;
            }

            private void stopAndCleanup() {
                this.cancel();
                removePotionEffects(formation, center);
                stopEffect(center); // Dọn dẹp task và các map
                world.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
                world.spawnParticle(Particle.SMOKE_LARGE, center.clone().add(0.5, 1, 0.5), 50, 0.5, 0.5, 0.5, 0);
            }
        }.runTaskTimer(plugin, 0L, 1L); // Chạy mỗi tick

        activeEffectTasks.put(center, task);
        world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
    }

    /**
     * Áp dụng tất cả các hiệu ứng được cấu hình cho trận pháp.
     * Phương thức này giờ chỉ đóng vai trò điều phối.
     */
    private void applyEffects(Formation formation, Location center) {
        World world = center.getWorld();
        if (world == null) return;
        long currentTick = animationTickMap.getOrDefault(center, 0L);

        // 1. Áp dụng hiệu ứng hạt (nếu có)
        Map<String, Object> particleConfig = formation.getParticleConfig();
        if (particleConfig != null && !particleConfig.isEmpty()) {
            applyCinematicParticleShield(formation, center, particleConfig, currentTick);
        }

        // 2. Chỉ áp dụng hiệu ứng logic theo tần suất đã định
        if (currentTick % plugin.getConfigManager().getEffectCheckInterval() != 0) {
            return;
        }

        // 3. Lấy danh sách thực thể và khối một lần duy nhất để tối ưu
        Collection<LivingEntity> nearbyEntities = world.getNearbyLivingEntities(center, formation.getRadius());
        List<Block> nearbyBlocks = getBlocksInRadius(center, (int) Math.ceil(formation.getRadius()));

        final boolean isDebug = plugin.getConfigManager().isDebugLoggingEnabled();

        // 4. Vòng lặp và ủy thác cho các lớp Strategy
        for (Map<?, ?> effectMap : formation.getEffects()) {
            String typeStr = String.valueOf(effectMap.get("type")).toUpperCase();
            FormationEffect strategy = effectStrategies.get(typeStr);

            if (strategy != null) {
                if (isDebug) plugin.getLogger().info("[DEBUG][EFFECT] Running effect: " + typeStr);
                strategy.apply(plugin, formation, center, effectMap, nearbyEntities, nearbyBlocks);
            } else {
                if (isDebug && currentTick % 200 == 0) { // Log cảnh báo không thường xuyên
                    plugin.getLogger().warning("Loại hiệu ứng không xác định '" + typeStr + "' trong trận pháp: " + formation.getId());
                }
            }
        }
    }

    /**
     * Được gọi bởi PotionEffectStrategy để theo dõi các thực thể bị ảnh hưởng.
     */
    public void trackAffectedEntity(Location center, UUID entityId) {
        affectedEntitiesByFormation.computeIfAbsent(center, k -> new HashSet<>()).add(entityId);
    }

    /**
     * Dừng và dọn dẹp một hiệu ứng trận pháp tại một vị trí.
     */
    public void stopEffect(Location center) {
        BukkitTask task = activeEffectTasks.remove(center);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        affectedEntitiesByFormation.remove(center);
        animationTickMap.remove(center);
        // Thông báo cho FormationManager rằng trận pháp không còn hoạt động
        plugin.getFormationManager().deactivateFormation(center);
    }

    /**
     * Dừng tất cả các hiệu ứng trận pháp đang hoạt động trên server.
     */
    public void stopAllEffects() {
        // Tạo một bản sao của keySet để tránh ConcurrentModificationException
        new ArrayList<>(activeEffectTasks.keySet()).forEach(this::stopEffect);
        activeEffectTasks.clear();
        affectedEntitiesByFormation.clear();
        animationTickMap.clear();
    }

    /**
     * Lấy tất cả các khối trong một bán kính hình tròn.
     */
    private List<Block> getBlocksInRadius(Location center, int radius) {
        List<Block> blocks = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) return blocks;
        int cX = center.getBlockX(), cY = center.getBlockY(), cZ = center.getBlockZ();
        int radiusSquared = radius * radius;

        for (int x = cX - radius; x <= cX + radius; x++) {
            for (int z = cZ - radius; z <= cZ + radius; z++) {
                // Chỉ lấy các khối trên cùng một mặt phẳng Y
                if (center.distanceSquared(new Location(world, x, cY, z)) <= radiusSquared) {
                    blocks.add(world.getBlockAt(x, cY, z));
                }
            }
        }
        return blocks;
    }

    /**
     * Xóa các hiệu ứng thuốc do trận pháp gây ra khi nó hết hiệu lực.
     */
    private void removePotionEffects(Formation formation, Location center) {
        Set<UUID> affectedUuids = affectedEntitiesByFormation.get(center);
        if (affectedUuids == null || affectedUuids.isEmpty()) return;

        // Lấy danh sách các loại hiệu ứng thuốc từ cấu hình trận pháp
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

    /**
     * Vẽ các hiệu ứng hạt phức tạp (giữ nguyên).
     */
    private void applyCinematicParticleShield(Formation formation, Location center, Map<?, ?> config, long tick) {
        World world = center.getWorld();
        if (world == null) return;
        double radius = formation.getRadius();
        Location centerPoint = center.clone().add(0.5, 0.1, 0.5);
        double rotationSpeed = EffectUtils.getDoubleFromConfig(config, "rotation_speed", 1.5);
        int pillarCount = EffectUtils.getIntFromConfig(config, "pillar_count", 5);
        int domeLines = EffectUtils.getIntFromConfig(config, "dome_lines", 8);
        Optional<Particle> mainParticle = Optional.ofNullable(EffectUtils.getStringFromConfig(config, "ring", null)).map(s -> Particle.valueOf(s.toUpperCase()));
        Optional<Particle> pillarParticle = Optional.ofNullable(EffectUtils.getStringFromConfig(config, "pillar", null)).map(s -> Particle.valueOf(s.toUpperCase()));
        Optional<Particle> orbParticle = Optional.ofNullable(EffectUtils.getStringFromConfig(config, "orb", null)).map(s -> Particle.valueOf(s.toUpperCase()));
        Optional<Particle> domeParticle = Optional.ofNullable(EffectUtils.getStringFromConfig(config, "dome", null)).map(s -> Particle.valueOf(s.toUpperCase()));
        double rotationAngle = Math.toRadians(tick * rotationSpeed);
        mainParticle.ifPresent(p -> {
            for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 32) {
                double x = centerPoint.getX() + radius * Math.cos(angle + rotationAngle);
                double z = centerPoint.getZ() + radius * Math.sin(angle + rotationAngle);
                world.spawnParticle(p, x, centerPoint.getY(), z, 1, 0, 0, 0, 0);
            }
        });
        pillarParticle.ifPresent(p -> {
            for (int i = 0; i < pillarCount; i++) {
                double pillarAngle = (2 * Math.PI / pillarCount) * i + rotationAngle;
                double x = centerPoint.getX() + radius * Math.cos(pillarAngle);
                double z = centerPoint.getZ() + radius * Math.sin(pillarAngle);
                double yOffset = (tick % 40) / 10.0;
                world.spawnParticle(p, x, centerPoint.getY() + yOffset, z, 1, 0, 0, 0, 0);
            }
        });
        orbParticle.ifPresent(p -> {
            for (int i = 0; i < 5; i++) {
                Vector dir = new Vector(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).normalize();
                world.spawnParticle(p, centerPoint.clone().add(0, 1.5, 0).add(dir.multiply(0.7)), 1, 0, 0, 0, 0);
            }
        });
        domeParticle.ifPresent(p -> {
            double progress = (double) (tick % 40) / 40.0;
            int domeDensity = EffectUtils.getIntFromConfig(config, "dome_density", 15);
            for (int i = 0; i < domeLines; i++) {
                double lineAngle = (2 * Math.PI / domeLines) * i + rotationAngle / 2;
                Location startPoint = new Location(world, centerPoint.getX() + radius * Math.cos(lineAngle), centerPoint.getY(), centerPoint.getZ() + radius * Math.sin(lineAngle));
                Location topPoint = centerPoint.clone().add(0, radius, 0);
                Vector toTop = topPoint.toVector().subtract(startPoint.toVector());
                for (int j = 0; j < (int)(domeDensity * progress) ; j++) {
                    double t = (double) j / domeDensity;
                    Vector pos = startPoint.toVector().add(toTop.clone().multiply(t));
                    Vector toCenter = centerPoint.toVector().subtract(pos.clone());
                    pos.add(toCenter.normalize().multiply(-Math.sin(t * Math.PI) * (radius/2)));
                    world.spawnParticle(p, pos.getX(), pos.getY(), pos.getZ(), 1, 0, 0, 0, 0);
                }
            }
        });
    }
}