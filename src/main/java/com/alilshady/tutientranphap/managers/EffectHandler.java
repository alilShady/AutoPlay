package com.alilshady.tutientranphap.managers;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EffectHandler {

    private final TuTienTranPhap plugin;
    private final Map<Location, BukkitTask> activeEffectTasks = new ConcurrentHashMap<>();
    private final Map<Location, Set<UUID>> affectedEntitiesByFormation = new ConcurrentHashMap<>();
    private final Map<Location, Long> animationTickMap = new ConcurrentHashMap<>();

    private static final List<PotionEffectType> DEBUFFS_TO_CLEANSE = Arrays.asList(
            PotionEffectType.SLOW, PotionEffectType.SLOW_DIGGING, PotionEffectType.WEAKNESS,
            PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.CONFUSION,
            PotionEffectType.BLINDNESS, PotionEffectType.HUNGER, PotionEffectType.LEVITATION
    );

    public EffectHandler(TuTienTranPhap plugin) {
        this.plugin = plugin;
    }

    public void startFormationEffects(Formation formation, Location center) {
        if (activeEffectTasks.containsKey(center)) {
            stopEffect(center);
        }
        final World world = center.getWorld();
        if (world == null) return;

        affectedEntitiesByFormation.put(center, new HashSet<>());
        animationTickMap.put(center, 0L);

        final long maxDurationTicks = parseDurationToTicks(formation.getDuration());

        BukkitTask task = new BukkitRunnable() {
            private long ticksLived = 0;
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

        Map<String, Object> particleConfig = formation.getParticleConfig();
        if (particleConfig != null && !particleConfig.isEmpty()) {
            applyCinematicParticleShield(formation, center, particleConfig, currentTick);
        }

        if (currentTick % plugin.getConfigManager().getEffectCheckInterval() != 0) {
            return;
        }

        Collection<LivingEntity> nearbyEntities = null;
        List<Block> nearbyBlocks = null;
        final boolean isDebug = plugin.getConfigManager().isDebugLoggingEnabled();

        for (Map<?, ?> effectMap : formation.getEffects()) {
            String typeStr = String.valueOf(effectMap.get("type")).toUpperCase();

            if (nearbyEntities == null && Arrays.asList("POTION", "DAMAGE", "LIGHTNING", "REPEL", "REPAIR", "CLEANSE", "ROOT", "HEAL").contains(typeStr)) {
                nearbyEntities = world.getNearbyLivingEntities(center, formation.getRadius());
            }
            if (nearbyBlocks == null && Arrays.asList("GROWTH", "SMELT", "HARVEST", "FREEZE").contains(typeStr)) {
                nearbyBlocks = getBlocksInRadius(center, (int) Math.ceil(formation.getRadius()));
            }

            if(isDebug) plugin.getLogger().info("[DEBUG][EFFECT] Running effect: " + typeStr);

            switch (typeStr) {
                case "POTION": applyPotionEffect(nearbyEntities, effectMap, center); break;
                case "DAMAGE": applyAreaDamage(nearbyEntities, effectMap); break;
                case "LIGHTNING": applyLightningStrike(nearbyEntities, effectMap); break;
                case "REPEL": applyMobRepulsion(nearbyEntities, effectMap, center); break;
                case "SMELT": applyFurnaceBoost(nearbyBlocks, effectMap); break;
                case "REPAIR": applyItemRepair(nearbyEntities, effectMap); break;
                case "GROWTH": applyCropGrowth(nearbyBlocks, effectMap); break;
                case "CLEANSE": applyCleanse(nearbyEntities, effectMap); break;
                case "ROOT": applyRoot(nearbyEntities, effectMap); break;
                case "HARVEST": applyAutoHarvest(nearbyBlocks, effectMap, world); break;
                case "HEAL": applyHeal(nearbyEntities, effectMap); break;
                case "XP": applyXpOrbGeneration(effectMap, center); break;
                case "FREEZE": applyFreezeLiquids(nearbyBlocks, effectMap); break;
                default:
                    if (isDebug && currentTick % 200 == 0) {
                        plugin.getLogger().warning("Loại hiệu ứng không xác định '" + typeStr + "' trong trận pháp: " + formation.getId());
                    }
                    break;
            }
        }
    }

    private void applyHeal(Collection<LivingEntity> entities, Map<?, ?> config) {
        String targetType = getStringFromConfig(config, "target", "PLAYERS").toUpperCase();
        double healAmount = getDoubleFromConfig(config, "value", 1.0);
        for (LivingEntity entity : entities) {
            boolean shouldHeal = false;
            switch (targetType) {
                case "PLAYERS": if (entity instanceof Player) shouldHeal = true; break;
                case "ALL": shouldHeal = true; break;
            }
            if (shouldHeal) {
                AttributeInstance maxHealthAttribute = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                double maxHealth = (maxHealthAttribute != null) ? maxHealthAttribute.getValue() : 20.0;
                double newHealth = Math.min(entity.getHealth() + healAmount, maxHealth);
                entity.setHealth(newHealth);
            }
        }
    }

    private void applyFreezeLiquids(List<Block> blocks, Map<?, ?> config) {
        for (Block block : blocks) {
            if (block.isLiquid()) {
                if (block.getType() == Material.WATER) block.setType(Material.FROSTED_ICE);
                else if (block.getType() == Material.LAVA) block.setType(Material.OBSIDIAN);
            }
        }
    }

    private void applyXpOrbGeneration(Map<?, ?> config, Location center) {
        int amount = getIntFromConfig(config, "value", 1);
        Location orbLocation = center.clone().add(0.5, 1.5, 0.5);
        ExperienceOrb orb = (ExperienceOrb) center.getWorld().spawnEntity(orbLocation, EntityType.EXPERIENCE_ORB);
        orb.setExperience(amount);
    }

    private void applyAutoHarvest(List<Block> blocks, Map<?, ?> config, World world) {
        boolean replant = getBooleanFromConfig(config, "replant", true);
        for (Block block : blocks) {
            if (block.getBlockData() instanceof Ageable) {
                Ageable ageable = (Ageable) block.getBlockData();
                if (ageable.getAge() == ageable.getMaximumAge()) {
                    Collection<ItemStack> drops = block.getDrops();
                    Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
                    for (ItemStack item : drops) world.dropItemNaturally(dropLocation, item);
                    world.playSound(dropLocation, Sound.BLOCK_CROP_BREAK, 1.0f, 1.0f);
                    if (replant) {
                        ageable.setAge(0);
                        block.setBlockData(ageable);
                    } else {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }

    private void applyCleanse(Collection<LivingEntity> entities, Map<?, ?> config) {
        String targetType = getStringFromConfig(config, "target", "PLAYERS").toUpperCase();
        for (LivingEntity entity : entities) {
            boolean shouldCleanse = (targetType.equals("PLAYERS") && entity instanceof Player) || targetType.equals("ALL");
            if (shouldCleanse) DEBUFFS_TO_CLEANSE.forEach(entity::removePotionEffect);
        }
    }

    private void applyRoot(Collection<LivingEntity> entities, Map<?, ?> config) {
        int durationTicks = (int) parseDurationToTicks(getStringFromConfig(config, "duration", "40t"));
        int amplifier = getIntFromConfig(config, "value", 7);
        PotionEffect rootEffect = new PotionEffect(PotionEffectType.SLOW, durationTicks, amplifier, true, false);
        String targetType = getStringFromConfig(config, "target", "HOSTILE_MOBS").toUpperCase();
        for (LivingEntity entity : entities) {
            boolean shouldRoot = (targetType.equals("HOSTILE_MOBS") && entity instanceof Monster) || (targetType.equals("ALL") && !(entity instanceof Player));
            if (shouldRoot) entity.addPotionEffect(rootEffect);
        }
    }

    private void applyMobRepulsion(Collection<LivingEntity> entities, Map<?, ?> config, Location center) {
        double force = getDoubleFromConfig(config, "value", 0.8);
        Vector centerVector = center.toVector();
        for (LivingEntity entity : entities) {
            if (entity instanceof Monster) {
                Vector awayVector = entity.getLocation().toVector().subtract(centerVector).normalize().multiply(force);
                awayVector.setY(0.1);
                entity.setVelocity(awayVector);
            }
        }
    }

    private void applyFurnaceBoost(List<Block> blocks, Map<?, ?> config) {
        float multiplier = (float) getDoubleFromConfig(config, "value", 2.0);
        for (Block block : blocks) {
            if (block.getState() instanceof Furnace) {
                Furnace furnace = (Furnace) block.getState();
                if (furnace.getBurnTime() > 0 && furnace.getCookTimeTotal() > 0) {
                    furnace.setCookTime((short) Math.min(furnace.getCookTimeTotal() - 1, furnace.getCookTime() + (int)(multiplier - 1)));
                    furnace.update();
                }
            }
        }
    }

    private void applyItemRepair(Collection<LivingEntity> entities, Map<?, ?> config) {
        int amount = getIntFromConfig(config, "value", 5);
        for (LivingEntity entity : entities) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                repairItem(player.getInventory().getItemInMainHand(), amount);
                repairItem(player.getInventory().getItemInOffHand(), amount);
                for (ItemStack armor : player.getInventory().getArmorContents()) repairItem(armor, amount);
            }
        }
    }

    private void repairItem(ItemStack item, int amount) {
        if (item != null && item.hasItemMeta() && item.getItemMeta() instanceof Damageable) {
            Damageable damageable = (Damageable) item.getItemMeta();
            int currentDamage = damageable.getDamage();
            if (currentDamage > 0) {
                damageable.setDamage(Math.max(0, currentDamage - amount));
                item.setItemMeta((ItemMeta) damageable);
            }
        }
    }

    private void applyAreaDamage(Collection<LivingEntity> entities, Map<?, ?> config) {
        double damage = getDoubleFromConfig(config, "value", 1.0);
        String targetType = getStringFromConfig(config, "target", "HOSTILE_MOBS").toUpperCase();
        for (LivingEntity entity : entities) {
            if (entity instanceof Player && ((Player) entity).getGameMode() != GameMode.SURVIVAL) continue;
            boolean shouldDamage = false;
            switch (targetType) {
                case "PLAYERS": if (entity instanceof Player) shouldDamage = true; break;
                case "HOSTILE_MOBS": if (entity instanceof Monster) shouldDamage = true; break;
                case "ALL": shouldDamage = true; break;
            }
            if (shouldDamage) entity.damage(damage);
        }
    }

    private void applyLightningStrike(Collection<LivingEntity> entities, Map<?, ?> config) {
        boolean visualOnly = getBooleanFromConfig(config, "visual_only", false);
        String targetType = getStringFromConfig(config, "target", "HOSTILE_MOBS").toUpperCase();
        for (LivingEntity entity : entities) {
            if (entity instanceof Player && ((Player) entity).getGameMode() != GameMode.SURVIVAL) continue;
            boolean shouldStrike = false;
            switch (targetType) {
                case "PLAYERS": if (entity instanceof Player) shouldStrike = true; break;
                case "HOSTILE_MOBS": if (entity instanceof Monster) shouldStrike = true; break;
                case "ALL": shouldStrike = true; break;
            }
            if (shouldStrike) {
                if (visualOnly) entity.getWorld().strikeLightningEffect(entity.getLocation());
                else entity.getWorld().strikeLightning(entity.getLocation());
            }
        }
    }

    private void applyPotionEffect(Collection<LivingEntity> entities, Map<?, ?> config, Location formationCenter) {
        String potionName = getStringFromConfig(config, "potion_effect", "");
        if (potionName.isEmpty()) return;
        PotionEffectType potionType = PotionEffectType.getByName(potionName.toUpperCase());
        if (potionType == null) return;

        int amplifier = getIntFromConfig(config, "value", 0);
        int durationTicks = plugin.getConfigManager().getEffectCheckInterval() + 40;
        PotionEffect effect = new PotionEffect(potionType, durationTicks, amplifier, true, false);

        String targetType = getStringFromConfig(config, "target", "PLAYERS").toUpperCase();
        for (LivingEntity entity : entities) {
            boolean shouldApply = false;
            switch (targetType) {
                case "PLAYERS": if (entity instanceof Player) shouldApply = true; break;
                case "HOSTILE_MOBS": if (entity instanceof Monster) shouldApply = true; break;
                case "ALL": shouldApply = true; break;
            }
            if (shouldApply) {
                entity.addPotionEffect(effect);
                affectedEntitiesByFormation.computeIfAbsent(formationCenter, k -> new HashSet<>()).add(entity.getUniqueId());
            }
        }
    }

    private void applyCropGrowth(List<Block> blocks, Map<?, ?> config) {
        double multiplier = getDoubleFromConfig(config, "value", 1.5);
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

    private void applyCinematicParticleShield(Formation formation, Location center, Map<?, ?> config, long tick) {
        World world = center.getWorld();
        if (world == null) return;
        double radius = formation.getRadius();
        Location centerPoint = center.clone().add(0.5, 0.1, 0.5);
        double rotationSpeed = getDoubleFromConfig(config, "rotation_speed", 1.5);
        int pillarCount = getIntFromConfig(config, "pillar_count", 5);
        int domeLines = getIntFromConfig(config, "dome_lines", 8);
        Optional<Particle> mainParticle = getParticleFromConfig(config, "main_particle", tick);
        Optional<Particle> pillarParticle = getParticleFromConfig(config, "pillar_particle", tick);
        Optional<Particle> orbParticle = getParticleFromConfig(config, "orb_particle", tick);
        Optional<Particle> domeParticle = getParticleFromConfig(config, "dome_particle", tick);
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
            int domeDensity = getIntFromConfig(config, "dome_density", 15);
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

    private long parseDurationToTicks(String durationStr) {
        if (durationStr == null || durationStr.isEmpty()) return 0;
        try {
            Pattern pattern = Pattern.compile("(\\d+)([tsmh]?)");
            Matcher matcher = pattern.matcher(durationStr.toLowerCase());
            if (matcher.matches()) {
                long value = Long.parseLong(matcher.group(1));
                String unit = matcher.group(2);
                switch (unit) {
                    case "s": return value * 20;
                    case "m": return value * 20 * 60;
                    case "h": return value * 20 * 60 * 60;
                    case "t": default: return value;
                }
            }
            return Long.parseLong(durationStr);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Không thể phân tích định dạng thời gian: '" + durationStr + "'. Mặc định về 0.");
            return 0;
        }
    }

    private Optional<Particle> getParticleFromConfig(Map<?, ?> config, String key, long tick) {
        Object value = config.get(key);
        if (value instanceof String) {
            String particleName = (String) value;
            try {
                return Optional.of(Particle.valueOf(particleName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                if (tick % 200 == 0) plugin.getLogger().warning("Tên particle không hợp lệ '" + particleName + "' cho key '" + key + "'");
            }
        }
        return Optional.empty();
    }

    private boolean getBooleanFromConfig(Map<?, ?> config, String key, boolean defaultValue) {
        Object value = config.get(key);
        return (value instanceof Boolean) ? (Boolean) value : defaultValue;
    }

    private String getStringFromConfig(Map<?, ?> config, String key, String defaultValue) {
        Object value = config.get(key);
        return (value instanceof String) ? (String) value : defaultValue;
    }

    private int getIntFromConfig(Map<?, ?> config, String key, int defaultValue) {
        Object value = config.get(key);
        return (value instanceof Number) ? ((Number) value).intValue() : defaultValue;
    }

    private double getDoubleFromConfig(Map<?, ?> config, String key, double defaultValue) {
        Object value = config.get(key);
        return (value instanceof Number) ? ((Number) value).doubleValue() : defaultValue;
    }

    public void stopEffect(Location center) {
        BukkitTask task = activeEffectTasks.remove(center);
        if (task != null) task.cancel();
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
                    if (potionType != null) formationPotionTypes.add(potionType);
                }
            }
        }
        if (formationPotionTypes.isEmpty()) return;
        for (UUID uuid : affectedUuids) {
            LivingEntity entity = (LivingEntity) Bukkit.getEntity(uuid);
            if (entity != null && entity.isValid()) {
                for (PotionEffectType typeToRemove : formationPotionTypes) {
                    entity.removePotionEffect(typeToRemove);
                }
            }
        }
    }
}