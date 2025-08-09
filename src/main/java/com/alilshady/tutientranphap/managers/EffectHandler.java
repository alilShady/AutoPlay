package com.alilshady.tutientranphap.managers;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
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

        for (Map<?, ?> effectMap : formation.getEffects()) {
            String typeStr = String.valueOf(effectMap.get("type")).toUpperCase();

            // Logic cho các hiệu ứng chạy theo tick (tối ưu)
            if (currentTick % plugin.getConfigManager().getEffectCheckInterval() == 0) {
                switch (typeStr) {
                    case "POTION":
                        applyPotionEffect(world.getNearbyLivingEntities(center, formation.getRadius()), effectMap, center);
                        break;
                    case "CROP_GROWTH":
                        applyCropGrowth(getBlocksInRadius(center, (int) Math.ceil(formation.getRadius())), effectMap);
                        break;
                    case "AREA_DAMAGE":
                        applyAreaDamage(world.getNearbyLivingEntities(center, formation.getRadius()), effectMap);
                        break;
                    case "MOB_REPULSION":
                        applyMobRepulsion(world.getNearbyLivingEntities(center, formation.getRadius()), center, effectMap);
                        break;
                    case "FURNACE_BOOST":
                        applyFurnaceBoost(getBlocksInRadius(center, (int) Math.ceil(formation.getRadius())), effectMap);
                        break;
                    case "ITEM_REPAIR":
                        applyItemRepair(world.getNearbyLivingEntities(center, formation.getRadius()), effectMap);
                        break;
                }
            }

            // Hiệu ứng hạt luôn chạy mỗi tick để mượt mà
            if (typeStr.equals("PARTICLE_SHIELD")) {
                applyCinematicParticleShield(formation, center, effectMap, currentTick);
            }
        }
    }

    // --- Các hàm tiện ích để đọc config an toàn ---
    private String getStringFromConfig(Map<?, ?> config, String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue).toString();
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

    private Optional<Particle> getParticleFromConfig(Map<?, ?> config, String key) {
        String particleName = getStringFromConfig(config, key, "");
        if (!particleName.isEmpty()) {
            try {
                return Optional.of(Particle.valueOf(particleName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Log lỗi một cách an toàn, tránh spam console
                if (animationTickMap.values().stream().anyMatch(tick -> tick % 200 == 0)) {
                    plugin.getLogger().warning("Tên particle không hợp lệ cho key '" + key + "': " + particleName);
                }
            }
        }
        return Optional.empty();
    }

    // --- Các hàm logic cho từng hiệu ứng ---
    private void applyAreaDamage(Collection<LivingEntity> entities, Map<?, ?> config) {
        String targetType = getStringFromConfig(config, "target", "HOSTILE_MOBS").toUpperCase();
        double damage = getDoubleFromConfig(config, "damage", 1.0);
        for (LivingEntity entity : entities) {
            if (entity instanceof Player && ((Player) entity).getGameMode() != GameMode.SURVIVAL) continue;
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
            if (shouldDamage) entity.damage(damage);
        }
    }

    private void applyMobRepulsion(Collection<LivingEntity> entities, Location center, Map<?, ?> config) {
        double force = getDoubleFromConfig(config, "force", 0.8);
        Vector centerVector = center.toVector();
        for (LivingEntity entity : entities) {
            if (entity instanceof Monster) {
                Vector entityVector = entity.getLocation().toVector();
                Vector velocity = entityVector.subtract(centerVector).normalize().multiply(force).setY(0.4);
                entity.setVelocity(velocity);
            }
        }
    }

    private void applyFurnaceBoost(List<Block> blocks, Map<?, ?> config) {
        int multiplier = getIntFromConfig(config, "multiplier", 2);
        short boostAmount = (short) (multiplier * plugin.getConfigManager().getEffectCheckInterval());
        for (Block block : blocks) {
            if (block.getState() instanceof Furnace) {
                Furnace furnace = (Furnace) block.getState();
                if (furnace.getBurnTime() > 0 && furnace.getCookTime() > 0) {
                    furnace.setCookTime((short) (furnace.getCookTime() + boostAmount));
                    furnace.update();
                }
            }
        }
    }

    private void applyItemRepair(Collection<LivingEntity> entities, Map<?, ?> config) {
        String target = getStringFromConfig(config, "target", "PLAYERS").toUpperCase();
        if (!target.equals("PLAYERS") && !target.equals("ALL")) return;
        int amount = getIntFromConfig(config, "amount", 5);
        for (LivingEntity entity : entities) {
            if (entity instanceof Player) { // Chỉ sửa đồ cho Player
                Player player = (Player) entity;
                for (ItemStack item : player.getInventory().getContents()) repairItem(item, amount);
                for (ItemStack item : player.getInventory().getArmorContents()) repairItem(item, amount);
                repairItem(player.getInventory().getItemInOffHand(), amount);
            }
        }
    }

    private void repairItem(ItemStack item, int amount) {
        if (item != null && item.getItemMeta() instanceof Damageable) {
            Damageable meta = (Damageable) item.getItemMeta();
            if (meta.hasDamage()) {
                meta.setDamage(Math.max(0, meta.getDamage() - amount));
                item.setItemMeta(meta);
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
        double rotationAngle = Math.toRadians(tick * rotationSpeed);

        getParticleFromConfig(config, "main_particle").ifPresent(particle -> {
            for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 32) {
                world.spawnParticle(particle, centerPoint.getX() + radius * Math.cos(angle + rotationAngle), centerPoint.getY(), centerPoint.getZ() + radius * Math.sin(angle + rotationAngle), 1, 0, 0, 0, 0);
            }
        });

        getParticleFromConfig(config, "pillar_particle").ifPresent(particle -> {
            for (int i = 0; i < pillarCount; i++) {
                double angle = (2 * Math.PI / pillarCount) * i + rotationAngle;
                world.spawnParticle(particle, centerPoint.getX() + radius * Math.cos(angle), centerPoint.getY() + (tick % 40) / 10.0, centerPoint.getZ() + radius * Math.sin(angle), 1, 0, 0, 0, 0);
            }
        });
    }

    private void applyPotionEffect(Collection<LivingEntity> entities, Map<?, ?> config, Location formationCenter) {
        String targetType = getStringFromConfig(config, "target", "PLAYERS").toUpperCase();
        String potionName = getStringFromConfig(config, "potion_effect", "");
        if (potionName.isEmpty()) return;
        PotionEffectType potionType = PotionEffectType.getByName(potionName.toUpperCase());
        if (potionType == null) return;
        int amplifier = getIntFromConfig(config, "amplifier", 0);
        int durationTicks = plugin.getConfigManager().getEffectCheckInterval() + 40;
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
                affectedEntitiesByFormation.computeIfAbsent(formationCenter, k -> new HashSet<>()).add(entity.getUniqueId());
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

    // --- Các hàm quản lý vòng đời của hiệu ứng ---
    public void stopEffect(Location center) {
        Optional.ofNullable(activeEffectTasks.remove(center)).ifPresent(BukkitTask::cancel);
        affectedEntitiesByFormation.remove(center);
        animationTickMap.remove(center);
        plugin.getFormationManager().deactivateFormation(center);
    }

    public void stopAllEffects() {
        new ArrayList<>(activeEffectTasks.keySet()).forEach(this::stopEffect);
    }

    private List<Block> getBlocksInRadius(Location center, int radius) {
        List<Block> blocks = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) return blocks;

        // SỬA LỖI BUILD TẠI ĐÂY
        int cX = center.getBlockX();
        int cY = center.getBlockY();
        int cZ = center.getBlockZ();

        double radiusSquared = (double) radius * radius;
        for (int x = cX - radius; x <= cX + radius; x++) {
            for (int z = cZ - radius; z <= cZ + radius; z++) {
                // Tối ưu hóa bằng cách không tạo Location mới trong vòng lặp
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

        List<PotionEffectType> formationPotionTypes = new ArrayList<>();
        for (Map<?, ?> effectMap : formation.getEffects()) {
            if ("POTION".equalsIgnoreCase(getStringFromConfig(effectMap, "type", ""))) {
                String potionName = getStringFromConfig(effectMap, "potion_effect", "");
                Optional.ofNullable(PotionEffectType.getByName(potionName.toUpperCase()))
                        .ifPresent(formationPotionTypes::add);
            }
        }

        if (formationPotionTypes.isEmpty()) return;

        for (UUID uuid : affectedUuids) {
            Optional.ofNullable(Bukkit.getEntity(uuid))
                    .filter(LivingEntity.class::isInstance)
                    .map(LivingEntity.class::cast)
                    .ifPresent(entity -> formationPotionTypes.forEach(entity::removePotionEffect));
        }
    }
}