// src/main/java/com/alilshady/tutientranphap/effects/XpOrbGenerationEffect.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class XpOrbGenerationEffect implements FormationEffect {

    private final Random random = new Random();
    private static final int ORB_COUNT = 20; // Giá trị số lượng orb đã được cố định ở đây

    @Override
    public String getType() {
        return "XP";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        World world = center.getWorld();
        if (world == null) return;

        int totalExperience = EffectUtils.getIntFromConfig(config, "value", 1);
        double radius = formation.getRadius();

        // Tổng kinh nghiệm sẽ được chia đều cho số lượng orb cố định
        int experiencePerOrb = Math.max(1, totalExperience / ORB_COUNT);

        for (int i = 0; i < ORB_COUNT; i++) {
            // Tạo vị trí ngẫu nhiên trong bán kính hình tròn
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * radius;
            double xOffset = Math.cos(angle) * distance;
            double zOffset = Math.sin(angle) * distance;

            // Thêm một chút chênh lệch chiều cao để tự nhiên hơn
            double yOffset = 1.0 + random.nextDouble();

            Location orbLocation = center.clone().add(xOffset, yOffset, zOffset);

            ExperienceOrb orb = (ExperienceOrb) world.spawnEntity(orbLocation, EntityType.EXPERIENCE_ORB);
            orb.setExperience(experiencePerOrb);
        }
    }
}