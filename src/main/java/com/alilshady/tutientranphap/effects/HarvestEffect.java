// src/main/java/com/alilshady/tutientranphap/effects/HarvestEffect.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class HarvestEffect implements FormationEffect {

    @Override
    public String getType() {
        return "HARVEST";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        if (nearbyBlocks == null) return;
        World world = center.getWorld();
        if (world == null) return;

        boolean replant = EffectUtils.getBooleanFromConfig(config, "replant", true);

        for (Block block : nearbyBlocks) {
            if (block.getBlockData() instanceof Ageable) {
                Ageable ageable = (Ageable) block.getBlockData();
                if (ageable.getAge() == ageable.getMaximumAge()) {
                    Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);

                    // Thu hoạch
                    Collection<ItemStack> drops = block.getDrops();
                    for (ItemStack item : drops) {
                        world.dropItemNaturally(dropLocation, item);
                    }
                    world.playSound(dropLocation, Sound.BLOCK_CROP_BREAK, 1.0f, 1.0f);

                    // Trồng lại hoặc xóa
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
}