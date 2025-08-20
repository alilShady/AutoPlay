package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.EssenceArrays;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID; // Thêm import

public class BreedEffect implements FormationEffect {

    private final Map<Location, Long> cooldowns = new HashMap<>();

    @Override
    public String getType() {
        return "BREED";
    }

    // SỬA Ở ĐÂY: Thêm UUID ownerId
    @Override
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyEntities == null) return;

        long cooldownTicks = EffectUtils.getIntFromConfig(config, "value", 200);
        long currentTime = center.getWorld().getFullTime();
        long lastBreedTime = cooldowns.getOrDefault(center, 0L);

        if (currentTime - lastBreedTime < cooldownTicks) {
            return;
        }

        for (LivingEntity entity : nearbyEntities) {
            if (entity instanceof Animals) {
                Animals animal = (Animals) entity;
                if (animal.isAdult() && animal.canBreed()) {
                    animal.setLoveModeTicks(600);
                    cooldowns.put(center, currentTime);
                    return;
                }
            }
        }
    }
}