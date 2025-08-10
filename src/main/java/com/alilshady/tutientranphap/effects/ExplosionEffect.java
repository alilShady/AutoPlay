// src/main/java/com/alilshady/tutientranphap/effects/ExplosionEffect.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ExplosionEffect implements FormationEffect {

    @Override
    public String getType() {
        return "EXPLOSION";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        if (nearbyEntities == null) return;
        World world = center.getWorld();
        if (world == null) return;

        // Lấy sức mạnh từ config, các giá trị khác được mặc định
        float power = (float) EffectUtils.getDoubleFromConfig(config, "value", 2.0);
        String targetType = EffectUtils.getStringFromConfig(config, "target", "HOSTILE_MOBS").toUpperCase();

        for (LivingEntity entity : nearbyEntities) {
            boolean shouldExplode = false;
            switch (targetType) {
                case "PLAYERS":
                    if (entity instanceof Player) shouldExplode = true;
                    break;
                case "HOSTILE_MOBS":
                    if (entity instanceof Monster) shouldExplode = true;
                    break;
                case "ALL":
                    shouldExplode = true;
                    break;
            }

            if (shouldExplode) {
                // Tạo một vụ nổ tại vị trí của thực thể
                // Mặc định: Luôn tạo lửa (true), Không phá khối (false)
                world.createExplosion(entity.getLocation(), power, true, false);
            }
        }
    }
}