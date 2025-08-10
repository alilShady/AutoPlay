// src/main/java/com/alilshady/tutientranphap/effects/StasisEffect.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class StasisEffect implements FormationEffect {

    @Override
    public String getType() {
        return "STASIS";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        if (nearbyEntities == null) return;

        int slowAmplifier = EffectUtils.getIntFromConfig(config, "value", 2); // Mặc định làm chậm cấp 2
        int durationTicks = plugin.getConfigManager().getEffectCheckInterval() + 40; // Đảm bảo hiệu ứng không bị ngắt quãng

        PotionEffect slowEffect = new PotionEffect(PotionEffectType.SLOW, durationTicks, slowAmplifier - 1, true, false);

        for (LivingEntity entity : nearbyEntities) {
            // Hiệu ứng này chỉ tác động lên quái vật thù địch
            if (entity instanceof Monster) {
                entity.addPotionEffect(slowEffect);
            }
        }
    }
}