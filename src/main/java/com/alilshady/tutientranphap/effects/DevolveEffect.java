// src/main/java/com/alilshady/tutientranphap/effects/DevolveEffect.java
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

public class DevolveEffect implements FormationEffect {

    @Override
    public String getType() {
        return "DEVOLVE";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {

        // Đọc 'value' từ config, mặc định là 1 nếu không có
        int devolveStrength = EffectUtils.getIntFromConfig(config, "value", 1);
        if (devolveStrength <= 0) return; // Bỏ qua nếu value không hợp lệ

        // --- Tác động lên Thực thể ---
        if (nearbyEntities != null) {
            for (LivingEntity entity : nearbyEntities) {
                // Biến động vật trưởng thành thành con non
                if (entity instanceof Animals) {
                    Animals animal = (Animals) entity;
                    if (animal.isAdult()) {
                        animal.setBaby();
                    }
                }

                // Giảm kích thước của Slime và Magma Cube
                if (entity instanceof Slime) {
                    Slime slime = (Slime) entity;
                    int newSize = Math.max(1, slime.getSize() - devolveStrength);
                    if (slime.getSize() > newSize) {
                        slime.setSize(newSize);
                    }
                }
            }
        }

        // --- Tác động lên Cây trồng ---
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