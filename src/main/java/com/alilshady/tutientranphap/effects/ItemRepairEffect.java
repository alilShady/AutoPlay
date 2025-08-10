// src/main/java/com/alilshady/tutientranphap/effects/ItemRepairEffect.java
package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ItemRepairEffect implements FormationEffect {

    @Override
    public String getType() {
        return "REPAIR";
    }

    @Override
    public void apply(TuTienTranPhap plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks) {
        if (nearbyEntities == null) return;

        int amount = EffectUtils.getIntFromConfig(config, "value", 5);

        for (LivingEntity entity : nearbyEntities) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                repairItem(player.getInventory().getItemInMainHand(), amount);
                repairItem(player.getInventory().getItemInOffHand(), amount);
                for (ItemStack armor : player.getInventory().getArmorContents()) {
                    repairItem(armor, amount);
                }
            }
        }
    }

    private void repairItem(ItemStack item, int amount) {
        if (item != null && item.hasItemMeta() && item.getItemMeta() instanceof Damageable) {
            Damageable damageable = (Damageable) item.getItemMeta();
            int currentDamage = damageable.getDamage();
            if (currentDamage > 0) {
                damageable.setDamage(Math.max(0, currentDamage - amount));
                item.setItemMeta((ItemMeta) damageable);
            }
        }
    }
}