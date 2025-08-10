// src/main/java/com/alilshady/tutientranphap/effects/BreedEffect.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BreedEffect implements FormationEffect {

    // Sử dụng Map để quản lý cooldown cho từng trận pháp riêng biệt
    private final Map<Location, Long> cooldowns = new HashMap<>();

    @Override
    public String getType() {
        return "BREED";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        if (nearbyEntities == null) return;

        long cooldownTicks = EffectUtils.getIntFromConfig(config, "value", 200); // 10 giây mặc định
        long currentTime = center.getWorld().getFullTime();
        long lastBreedTime = cooldowns.getOrDefault(center, 0L);

        // Kiểm tra cooldown
        if (currentTime - lastBreedTime < cooldownTicks) {
            return;
        }

        for (LivingEntity entity : nearbyEntities) {
            if (entity instanceof Animals) {
                Animals animal = (Animals) entity;
                if (animal.isAdult() && animal.canBreed()) {
                    animal.setLoveModeTicks(600); // Kích hoạt chế độ "yêu"
                    cooldowns.put(center, currentTime); // Đặt lại cooldown
                    return; // Mỗi lần chỉ nhân giống một con để tránh lag
                }
            }
        }
    }
}