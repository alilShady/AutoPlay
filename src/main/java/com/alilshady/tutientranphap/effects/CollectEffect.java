// src/main/java/com/alilshady/tutientranphap/effects/CollectEffect.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CollectEffect implements FormationEffect {

    @Override
    public String getType() {
        return "COLLECT";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        // Hiệu ứng này không dùng nearbyEntities hay nearbyBlocks, nó lấy các thực thể riêng
        double speed = EffectUtils.getDoubleFromConfig(config, "value", 0.8);
        Vector centerVector = center.clone().add(0.5, 0.5, 0.5).toVector();

        // Lấy tất cả thực thể trong bán kính, không chỉ LivingEntity
        Collection<Entity> allEntities = center.getWorld().getNearbyEntities(center, formation.getRadius(), formation.getRadius(), formation.getRadius());

        for (Entity entity : allEntities) {
            if (entity instanceof Item) {
                Item item = (Item) entity;
                // Nếu vật phẩm đã ở rất gần trung tâm thì bỏ qua
                if (item.getLocation().toVector().distanceSquared(centerVector) < 1.0) {
                    continue;
                }
                Vector direction = centerVector.clone().subtract(item.getLocation().toVector()).normalize().multiply(speed);
                item.setVelocity(direction);
            }
        }
    }
}