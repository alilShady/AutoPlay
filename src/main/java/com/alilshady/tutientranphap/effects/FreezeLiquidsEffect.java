package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID; // Thêm import

public class FreezeLiquidsEffect implements FormationEffect {

    @Override
    public String getType() {
        return "FREEZE";
    }

    // SỬA Ở ĐÂY: Thêm UUID ownerId
    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyBlocks == null) return;

        for (Block block : nearbyBlocks) {
            if (block.isLiquid()) {
                if (block.getType() == Material.WATER) {
                    block.setType(Material.FROSTED_ICE);
                } else if (block.getType() == Material.LAVA) {
                    block.setType(Material.OBSIDIAN);
                }
            }
        }
    }
}