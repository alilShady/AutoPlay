// src/main/java/com/alilshady/tutientranphap/effects/FormationEffect.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface FormationEffect {

    /**
     * Lấy loại hiệu ứng (ví dụ: "POTION", "DAMAGE").
     * @return Tên loại hiệu ứng.
     */
    String getType();

    /**
     * Áp dụng logic của hiệu ứng.
     * @param plugin         Instance của plugin chính.
     * @param formation      Trận pháp đang được kích hoạt.
     * @param center         Vị trí trung tâm của trận pháp.
     * @param config         Cấu hình riêng của hiệu ứng này từ formations.yml.
     * @param nearbyEntities Danh sách các thực thể sống trong bán kính.
     * @param nearbyBlocks   Danh sách các khối trong bán kính.
     */
    void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks);
}