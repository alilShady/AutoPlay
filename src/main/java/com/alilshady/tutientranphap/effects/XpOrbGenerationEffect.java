package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.EssenceArrays;
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
import java.util.UUID; // Thêm import

public class XpOrbGenerationEffect implements FormationEffect {

    private final Random random = new Random();
    private static final int ORB_COUNT = 20;

    @Override
    public String getType() {
        return "XP";
    }

    // SỬA Ở ĐÂY: Thêm UUID ownerId
    @Override
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        World world = center.getWorld();
        if (world == null) return;

        int totalExperience = EffectUtils.getIntFromConfig(config, "value", 1);
        double radius = formation.getRadius();

        int experiencePerOrb = Math.max(1, totalExperience / ORB_COUNT);

        for (int i = 0; i < ORB_COUNT; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * radius;
            double xOffset = Math.cos(angle) * distance;
            double zOffset = Math.sin(angle) * distance;
            double yOffset = 1.0 + random.nextDouble();
            Location orbLocation = center.clone().add(xOffset, yOffset, zOffset);

            ExperienceOrb orb = (ExperienceOrb) world.spawnEntity(orbLocation, EntityType.EXPERIENCE_ORB);
            orb.setExperience(experiencePerOrb);
        }
    }
}