package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.EssenceArrays;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.util.Vector;

import javax.annotation.Nullable; // <-- THÊM IMPORT MỚI
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ClimateEffect implements FormationEffect {

    private final Random random = new Random();
    private static final double PHYSICAL_CHANCE = 0.05;

    @Override
    public String getType() {
        return "CLIMATE";
    }

    @Override
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyBlocks != null) {
            applyPhysicalEffects(nearbyEntities, nearbyBlocks, config);
        }
    }

    // SỬA Ở ĐÂY: Thêm tham số Vector windDirection
    public void applyVisuals(World world, Location center, double radius, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, @Nullable Vector windDirection) {
        String mode = EffectUtils.getStringFromConfig(config, "mode", "RAIN").toUpperCase();

        switch (mode) {
            case "RAIN":
                spawnParticlesInArea(world, center, radius, Particle.WATER_DROP, 150, 12);
                break;
            case "SNOW":
                spawnParticlesInArea(world, center, radius, Particle.SNOWFLAKE, 100, 12);
                break;
            case "ACID_RAIN":
                spawnParticlesInArea(world, center, radius, Particle.DRIPPING_OBSIDIAN_TEAR, 100, 12);
                break;
            case "DROUGHT":
                spawnParticlesInArea(world, center, radius, Particle.WHITE_ASH, 15, 8);
                break;
            case "THUNDER":
                spawnParticlesInArea(world, center, radius, Particle.WATER_DROP, 200, 15);
                if (random.nextDouble() < 0.01) {
                    strikeLightningEffectInArea(world, center, radius);
                }
                // SỬA Ở ĐÂY: Sử dụng hướng gió đã được truyền vào
                if (windDirection != null && random.nextDouble() < 0.02) {
                    double windForce = 0.6 + (random.nextDouble() * 0.4); // Tạo lực gió ngẫu nhiên
                    applyWindGust(nearbyEntities, windDirection.clone().multiply(windForce));
                    applyWindParticles(world, center, radius, windDirection);
                }
                break;
        }
    }

    private void spawnParticlesInArea(World world, Location center, double radius, Particle particle, int count, double height) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = Math.sqrt(random.nextDouble()) * radius;
            double x = center.getX() + distance * Math.cos(angle);
            double z = center.getZ() + distance * Math.sin(angle);

            double y;
            if (particle == Particle.WATER_DROP || particle == Particle.SNOWFLAKE || particle == Particle.DRIPPING_OBSIDIAN_TEAR) {
                y = center.getY() + height;
            } else {
                y = center.getY() + random.nextDouble() * height;
            }

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
            Block topBlock = world.getHighestBlockAt((int)x, (int)z);

            if (topBlock.getType().name().contains("LEAVES")) {
                Location particleLoc = topBlock.getLocation().add(0.5, -0.5, 0.5);
                // SỬA Ở ĐÂY: Thêm vận tốc cho hạt lá bay theo hướng gió
                world.spawnParticle(Particle.BLOCK_DUST, particleLoc, 0, windDirection.getX(), -0.2, windDirection.getZ(), 0.5, topBlock.getBlockData());
            }
        }
    }

    private void applyPhysicalEffects(Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, Map<?, ?> config) {
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
        }
        else if (block.getType() == Material.WATER) {
            block.setType(Material.AIR);
        }
        else if (block.getType().isFlammable() && block.getRelative(BlockFace.UP).getType().isAir() && random.nextDouble() < 0.01) {
            block.getRelative(BlockFace.UP).setType(Material.FIRE);
        }
    }

    private void applyWindGust(Collection<LivingEntity> nearbyEntities, Vector windDirection) {
        for (LivingEntity entity : nearbyEntities) {
            entity.setVelocity(entity.getVelocity().add(windDirection));
        }
    }
}