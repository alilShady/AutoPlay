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

public class XpOrbGenerationEffect implements FormationEffect {

    @Override
    public String getType() {
        return "XP";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        World world = center.getWorld();
        if (world == null) return;

        int amount = EffectUtils.getIntFromConfig(config, "value", 1);
        Location orbLocation = center.clone().add(0.5, 1.5, 0.5);

        ExperienceOrb orb = (ExperienceOrb) world.spawnEntity(orbLocation, EntityType.EXPERIENCE_ORB);
        orb.setExperience(amount);
    }
}