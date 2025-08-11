package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID; // Thêm import

public class ClimateEffect implements FormationEffect {

    private final Random random = new Random();
    private static final int VISUAL_DENSITY = 75;
    private static final double PHYSICAL_CHANCE = 0.05;

    @Override
    public String getType() {
        return "CLIMATE";
    }

    // SỬA Ở ĐÂY: Thêm UUID ownerId
    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyEntities != null) {
            applyVisualEffects(nearbyEntities, config);
        }
        if (nearbyBlocks != null) {
            applyPhysicalEffects(nearbyEntities, nearbyBlocks, config);
        }
    }

    private void applyVisualEffects(Collection<LivingEntity> nearbyEntities, Map<?, ?> config) {
        String mode = EffectUtils.getStringFromConfig(config, "mode", "RAIN").toUpperCase();

        for (LivingEntity entity : nearbyEntities) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                switch (mode) {
                    case "RAIN":
                        spawnParticlesAroundPlayer(player, Particle.WATER_DROP, VISUAL_DENSITY, 15);
                        break;
                    case "SNOW":
                        spawnParticlesAroundPlayer(player, Particle.SNOWFLAKE, VISUAL_DENSITY, 15);
                        break;
                    case "ACID_RAIN":
                        spawnParticlesAroundPlayer(player, Particle.DRIPPING_OBSIDIAN_TEAR, VISUAL_DENSITY, 15);
                        break;
                    case "DROUGHT":
                        spawnParticlesAroundPlayer(player, Particle.WHITE_ASH, 10, 15);
                        break;
                    case "THUNDER":
                        spawnParticlesAroundPlayer(player, Particle.WATER_DROP, VISUAL_DENSITY, 15);
                        if (random.nextDouble() < 0.01) strikeLightningEffectNearPlayer(player);
                        if (random.nextDouble() < 0.02) applyWindGust(nearbyEntities);
                        break;
                }
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
        if (!block.getType().isAir() && block.getType().isSolid() && blockAbove.getType().isAir()) {
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

    private void applyWindGust(Collection<LivingEntity> nearbyEntities) {
        Vector windDirection = new Vector(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5).normalize().multiply(0.8);
        for (LivingEntity entity : nearbyEntities) {
            entity.setVelocity(entity.getVelocity().add(windDirection));
        }
    }

    private void spawnParticlesAroundPlayer(Player player, Particle particle, int density, double radius) {
        Location playerLoc = player.getLocation();
        for (int i = 0; i < density; i++) {
            double xOffset = (random.nextDouble() - 0.5) * radius;
            double yOffset = random.nextDouble() * 5;
            double zOffset = (random.nextDouble() - 0.5) * radius;
            player.spawnParticle(particle, playerLoc.clone().add(xOffset, yOffset, zOffset), 1, 0, 0, 0, 0);
        }
    }

    private void strikeLightningEffectNearPlayer(Player player) {
        Location strikeLoc = player.getLocation().clone().add((random.nextDouble() - 0.5) * 20, 0, (random.nextDouble() - 0.5) * 20);
        strikeLoc.setY(player.getWorld().getHighestBlockYAt(strikeLoc));
        player.getWorld().strikeLightningEffect(strikeLoc);
    }
}