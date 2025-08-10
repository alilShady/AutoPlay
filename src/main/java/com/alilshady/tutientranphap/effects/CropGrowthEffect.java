// src/main/java/com/alilshady/tutientranphap/effects/CropGrowthEffect.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CropGrowthEffect implements FormationEffect {

    private final Random random = new Random();

    @Override
    public String getType() {
        return "GROWTH";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        if (nearbyBlocks == null) return;

        double multiplier = EffectUtils.getDoubleFromConfig(config, "value", 1.5);

        for (Block block : nearbyBlocks) {
            if (block.getBlockData() instanceof Ageable) {
                Ageable ageable = (Ageable) block.getBlockData();
                if (ageable.getAge() < ageable.getMaximumAge()) {
                    // Tăng cơ hội cây trồng lớn lên dựa trên hệ số
                    if (random.nextDouble() < (0.1 * multiplier)) {
                        ageable.setAge(ageable.getAge() + 1);
                        block.setBlockData(ageable);
                    }
                }
            }
        }
    }
}