// src/main/java/com/alilshady/tutientranphap/effects/FurnaceBoostEffect.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FurnaceBoostEffect implements FormationEffect {

    @Override
    public String getType() {
        return "SMELT";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        if (nearbyBlocks == null) return;

        float multiplier = (float) EffectUtils.getDoubleFromConfig(config, "value", 2.0);

        for (Block block : nearbyBlocks) {
            if (block.getState() instanceof Furnace) {
                Furnace furnace = (Furnace) block.getState();
                if (furnace.getBurnTime() > 0 && furnace.getCookTimeTotal() > 0) {
                    // Tăng tiến độ nung
                    furnace.setCookTime((short) Math.min(furnace.getCookTimeTotal() - 1, furnace.getCookTime() + (int)(multiplier - 1)));
                    furnace.update();
                }
            }
        }
    }
}