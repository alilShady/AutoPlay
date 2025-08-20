package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.EssenceArrays;
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
import java.util.UUID; // Thêm import

public class CollectEffect implements FormationEffect {

    @Override
    public String getType() {
        return "COLLECT";
    }

    // SỬA Ở ĐÂY: Thêm UUID ownerId
    @Override
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        double speed = EffectUtils.getDoubleFromConfig(config, "value", 0.8);
        Vector centerVector = center.clone().add(0.5, 0.5, 0.5).toVector();

        Collection<Entity> allEntities = center.getWorld().getNearbyEntities(center, formation.getRadius(), formation.getRadius(), formation.getRadius());

        for (Entity entity : allEntities) {
            if (entity instanceof Item) {
                Item item = (Item) entity;
                if (item.getLocation().toVector().distanceSquared(centerVector) < 1.0) {
                    continue;
                }
                Vector direction = centerVector.clone().subtract(item.getLocation().toVector()).normalize().multiply(speed);
                item.setVelocity(direction);
            }
        }
    }
}