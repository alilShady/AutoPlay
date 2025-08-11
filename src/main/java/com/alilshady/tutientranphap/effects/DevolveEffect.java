package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Animals;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID; // Thêm import

public class DevolveEffect implements FormationEffect {

    @Override
    public String getType() {
        return "DEVOLVE";
    }

    // SỬA Ở ĐÂY: Thêm UUID ownerId
    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        int devolveStrength = EffectUtils.getIntFromConfig(config, "value", 1);
        if (devolveStrength <= 0) return;

        if (nearbyEntities != null) {
            for (LivingEntity entity : nearbyEntities) {
                if (entity instanceof Animals) {
                    Animals animal = (Animals) entity;
                    if (animal.isAdult()) {
                        animal.setBaby();
                    }
                }
                if (entity instanceof Slime) {
                    Slime slime = (Slime) entity;
                    int newSize = Math.max(1, slime.getSize() - devolveStrength);
                    if (slime.getSize() > newSize) {
                        slime.setSize(newSize);
                    }
                }
            }
        }

        if (nearbyBlocks != null) {
            for (Block block : nearbyBlocks) {
                if (block.getBlockData() instanceof Ageable) {
                    Ageable ageable = (Ageable) block.getBlockData();
                    int newAge = Math.max(0, ageable.getAge() - devolveStrength);
                    if (ageable.getAge() > newAge) {
                        ageable.setAge(newAge);
                        block.setBlockData(ageable);
                    }
                }
            }
        }
    }
}