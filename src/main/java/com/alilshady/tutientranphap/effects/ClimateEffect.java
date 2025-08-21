// src/main/java/com/alilshady/tutientranphap/effects/ClimateEffect.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.EssenceArrays;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.ArrayList;

public class ClimateEffect implements FormationEffect {

    private final Random random = new Random();
    private static final double PHYSICAL_CHANCE = 0.05;

    public static class Tornado {
        private Location location;
        private Vector velocity;
        private int ticksLived;
        private final int maxLifespan;
        private final Random random = new Random();
        private final double speed;

        public Tornado(Location startLocation, double speed, int maxLifespan) {
            this.location = startLocation.clone();
            this.speed = speed;
            this.velocity = new Vector(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5).normalize().multiply(speed);
            this.ticksLived = 0;
            this.maxLifespan = maxLifespan;
        }

        public void update() {
            Vector wander = new Vector(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5).normalize().multiply(0.1);
            velocity.add(wander).normalize().multiply(this.speed);
            location.add(velocity);
            ticksLived++;
        }

        public Location getLocation() {
            return location;
        }

        public boolean isDead(Location formationCenter, double maxDistance) {
            return ticksLived > maxLifespan || location.distanceSquared(formationCenter) > maxDistance * maxDistance;
        }
    }

    @Override
    public String getType() {
        return "CLIMATE";
    }

    @Override
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (random.nextDouble() <= PHYSICAL_CHANCE) {
            applyPhysicalEffects(plugin, ownerId, nearbyEntities, nearbyBlocks, config, center, formation.getRadius());
        }
    }

    public void applyVisuals(World world, Location center, double radius, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, @Nullable Vector windDirection, @Nullable List<Tornado> tornadoes) {
        String mode = EffectUtils.getStringFromConfig(config, "mode", "RAIN").toUpperCase();

        switch (mode) {
            case "RAIN":
                spawnParticlesInArea(world, center, radius, Particle.WATER_DROP, 150, 12.0);
                break;
            case "SNOW":
                spawnParticlesInArea(world, center, radius, Particle.SNOWFLAKE, 100, 12.0);
                break;
            case "ACID_RAIN":
                spawnParticlesInArea(world, center, radius, Particle.DRIPPING_OBSIDIAN_TEAR, 100, 12.0);
                break;
            case "DROUGHT":
                spawnParticlesInArea(world, center, radius, Particle.WHITE_ASH, 15, 8.0);
                break;
            case "THUNDER":
                spawnParticlesInArea(world, center, radius, Particle.WATER_DROP, 200, 15.0);
                if (random.nextDouble() < 0.01) {
                    strikeLightningEffectInArea(world, center, radius);
                }
                if (windDirection != null && random.nextDouble() < 0.02) {
                    double windForce = 0.1 + (random.nextDouble() * 0.15);
                    applyWindGust(nearbyEntities, windDirection.clone().multiply(windForce));
                    applyWindParticles(world, center, radius, windDirection);
                }
                break;
            case "TORNADO":
                if (tornadoes != null) {
                    manageMovingTornadoes(world, center, radius, config, tornadoes);
                }
                break;
        }
    }

    private void manageMovingTornadoes(World world, Location formationCenter, double maxDistance, Map<?, ?> config, List<Tornado> tornadoes) {
        Object tornadoConfigObj = config.get("tornado_config");
        if (!(tornadoConfigObj instanceof Map)) return;
        Map<?, ?> tornadoConfig = (Map<?, ?>) tornadoConfigObj;

        int maxCount = EffectUtils.getIntFromConfig(tornadoConfig, "count", 1);
        double spawnChance = EffectUtils.getDoubleFromConfig(tornadoConfig, "spawn_chance", 0.01);

        if (tornadoes.size() < maxCount && random.nextDouble() < spawnChance) {
            int lifespan = (int) EffectUtils.parseDurationToTicks(EffectUtils.getStringFromConfig(tornadoConfig, "lifespan", "10s"));
            double speed = EffectUtils.getDoubleFromConfig(tornadoConfig, "speed", 0.4);
            tornadoes.add(new Tornado(formationCenter, speed, lifespan));
        }

        Iterator<Tornado> iterator = tornadoes.iterator();
        while(iterator.hasNext()) {
            Tornado tornado = iterator.next();
            tornado.update();

            if (tornado.isDead(formationCenter, maxDistance)) {
                iterator.remove();
            } else {
                drawTornadoParticles(world, tornado.getLocation(), tornadoConfig);
            }
        }
    }

    // PHIÊN BẢN CŨ ĐỂ TƯƠNG THÍCH
    public void applyVisuals(World world, Location center, double radius, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, @Nullable Vector windDirection) {
        applyVisuals(world, center, radius, config, nearbyEntities, windDirection, null);
    }

    private void spawnParticlesInArea(World world, Location center, double radius, Particle particle, int count, double height) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = Math.sqrt(random.nextDouble()) * radius;
            double x = center.getX() + distance * Math.cos(angle);
            double z = center.getZ() + distance * Math.sin(angle);
            double y = (particle == Particle.WATER_DROP || particle == Particle.SNOWFLAKE || particle == Particle.DRIPPING_OBSIDIAN_TEAR)
                    ? center.getY() + height
                    : center.getY() + random.nextDouble() * height;
            world.spawnParticle(particle, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    private void strikeLightningEffectInArea(World world, Location center, double radius) {
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = Math.sqrt(random.nextDouble()) * radius;
        double x = center.getX() + distance * Math.cos(angle);
        double z = center.getZ() + distance * Math.sin(angle);
        Location strikeLoc = new Location(world, x, center.getY(), z);
        strikeLoc.setY(world.getHighestBlockYAt(strikeLoc));
        world.strikeLightningEffect(strikeLoc);
    }

    private void applyWindParticles(World world, Location center, double radius, Vector windDirection) {
        for (int i = 0; i < 150; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = Math.sqrt(random.nextDouble()) * radius;
            double x = center.getX() + distance * Math.cos(angle);
            double z = center.getZ() + distance * Math.sin(angle);
            Block topBlock = world.getHighestBlockAt((int) x, (int) z);

            if (topBlock.getType().name().contains("LEAVES")) {
                Location particleLoc = topBlock.getLocation().add(0.5, -0.5, 0.5);
                world.spawnParticle(Particle.BLOCK_DUST, particleLoc, 0, windDirection.getX(), -0.2, windDirection.getZ(), 0.5, topBlock.getBlockData());
            }
        }
    }

    private void drawTornadoParticles(World world, Location center, Map<?, ?> config) {
        double height = EffectUtils.getDoubleFromConfig(config, "height", 25.0);
        double maxRadius = EffectUtils.getDoubleFromConfig(config, "radius", 12.0);
        int particles = 700;
        int strands = 5;
        double revolutions = 3.0;
        Particle mainParticle = Particle.SWEEP_ATTACK;
        Particle secondaryParticle = Particle.SMOKE_NORMAL;
        Particle debrisParticle = Particle.BLOCK_DUST;
        long tick = world.getFullTime();

        for (int i = 0; i < particles; i++) {
            double progress = random.nextDouble();
            double y = progress * height;
            double currentRadius = maxRadius * progress;
            currentRadius *= (1 + (random.nextDouble() - 0.5) * 0.2);
            double angle = revolutions * 2 * Math.PI * progress;
            double rotation = tick * 0.15;
            for (int s = 0; s < strands; s++) {
                double strandOffset = (2 * Math.PI / strands) * s;
                double finalAngle = angle + rotation + strandOffset;
                double x = center.getX() + currentRadius * Math.cos(finalAngle);
                double z = center.getZ() + currentRadius * Math.sin(finalAngle);
                world.spawnParticle(mainParticle, x, center.getY() + y, z, 1, 0, 0, 0, 0);
                if(i % 5 == 0) {
                    world.spawnParticle(secondaryParticle, x, center.getY() + y, z, 1, 0, 0, 0, 0.01);
                }
            }
        }

        int debrisCount = 60;
        for (int i = 0; i < debrisCount; i++) {
            double debrisY = random.nextDouble() * height * 0.4;
            double debrisProgress = debrisY / (height * 0.4);
            double debrisRadius = maxRadius * debrisProgress;
            double debrisAngle = (tick * 0.5 + (i * 4.0)) % (2 * Math.PI);
            double debrisX = center.getX() + debrisRadius * Math.cos(debrisAngle);
            double debrisZ = center.getZ() + debrisRadius * Math.sin(debrisAngle);
            BlockData blockData = world.getBlockAt(center.clone().add(
                    (random.nextDouble() - 0.5) * 5, -1, (random.nextDouble() - 0.5) * 5
            )).getBlockData();
            world.spawnParticle(debrisParticle, debrisX, center.getY() + debrisY, debrisZ, 1, 0.2, 0.3, 0.2, 0.2, blockData);
        }
    }

    private void applyPhysicalEffects(EssenceArrays plugin, UUID ownerId, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, Map<?, ?> config, Location center, double radius) {
        String mode = EffectUtils.getStringFromConfig(config, "mode", "RAIN").toUpperCase();

        for (Block block : nearbyBlocks) {
            if (random.nextDouble() > PHYSICAL_CHANCE) continue;

            switch (mode) {
                case "RAIN":
                case "THUNDER":
                    handleRainEffects(block, mode);
                    break;
                case "SNOW":
                    handleSnowEffects(block);
                    break;
                case "DROUGHT":
                    handleDroughtEffects(block);
                    break;
            }
        }

        if (mode.equals("ACID_RAIN")) {
            if (random.nextDouble() <= PHYSICAL_CHANCE) {
                handleAcidRainEffects(nearbyEntities);
            }
        } else if (mode.equals("TORNADO")) {
            handleTornadoEffects(plugin, ownerId, nearbyEntities, config, center, radius);
        }
    }

    private void handleTornadoEffects(EssenceArrays plugin, UUID ownerId, Collection<LivingEntity> nearbyEntities, Map<?, ?> config, Location center, double radius) {
        double pullForce = 0.8;
        double liftForce = 0.6;
        String targetType = EffectUtils.getStringFromConfig(config, "target", "ALL").toUpperCase();
        Vector centerVector = center.toVector();

        for (LivingEntity entity : nearbyEntities) {
            if (EffectUtils.shouldApplyToEntity(plugin, entity, targetType, ownerId)) {
                Vector entityVector = entity.getLocation().toVector();
                double distance = entityVector.distance(centerVector);

                if (distance < radius) {
                    Vector toCenter = centerVector.clone().subtract(entityVector).normalize().multiply(pullForce);
                    Vector lift = new Vector(0, liftForce, 0);
                    entity.setVelocity(entity.getVelocity().add(toCenter).add(lift));
                }
            }
        }
    }

    private void handleRainEffects(Block block, String mode) {
        if (block.getType() == Material.FIRE) block.setType(Material.AIR);
        else if (block.getBlockData() instanceof Lightable) {
            Lightable lightable = (Lightable) block.getBlockData();
            if (lightable.isLit()) {
                lightable.setLit(false);
                block.setBlockData(lightable);
            }
        } else if (block.getType() == Material.CAULDRON) {
            Levelled cauldronData = (Levelled) block.getBlockData();
            if (cauldronData.getLevel() < cauldronData.getMaximumLevel()) {
                cauldronData.setLevel(cauldronData.getLevel() + 1);
                block.setBlockData(cauldronData);
            }
        }
        if (mode.equals("THUNDER") && random.nextDouble() < 0.001) {
            block.getWorld().strikeLightning(block.getLocation());
        }
    }

    private void handleSnowEffects(Block block) {
        Block blockAbove = block.getRelative(BlockFace.UP);
        boolean isSnowableSurface = !block.getType().isAir() && (block.getType().isSolid() || block.getType().name().contains("LEAVES"));

        if (isSnowableSurface && blockAbove.getType().isAir()) {
            blockAbove.setType(Material.SNOW);
        }
    }

    private void handleAcidRainEffects(Collection<LivingEntity> nearbyEntities) {
        for (LivingEntity entity : nearbyEntities) {
            if (entity.getWorld().getHighestBlockYAt(entity.getLocation()) <= entity.getLocation().getY()) {
                entity.damage(0.5);
                if (entity instanceof Player) {
                    for (ItemStack armor : ((Player) entity).getInventory().getArmorContents()) {
                        if (armor != null && armor.getItemMeta() instanceof Damageable) {
                            Damageable meta = (Damageable) armor.getItemMeta();
                            meta.setDamage(meta.getDamage() + 1);
                            armor.setItemMeta(meta);
                        }
                    }
                }
            }
        }
    }

    private void handleDroughtEffects(Block block) {
        if (block.getType() == Material.FARMLAND) {
            block.setType(Material.DIRT);
        } else if (block.getType() == Material.WATER) {
            block.setType(Material.AIR);
        } else if (block.getType().isFlammable() && block.getRelative(BlockFace.UP).getType().isAir() && random.nextDouble() < 0.01) {
            block.getRelative(BlockFace.UP).setType(Material.FIRE);
        }
    }

    private void applyWindGust(Collection<LivingEntity> nearbyEntities, Vector windDirection) {
        for (LivingEntity entity : nearbyEntities) {
            entity.setVelocity(entity.getVelocity().add(windDirection));
        }
    }
}