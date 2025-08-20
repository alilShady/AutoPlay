package com.alilshady.tutientranphap.effects;

import com.alilshady.tutientranphap.EssenceArrays;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItemRepairEffect implements FormationEffect {

    @Override
    public String getType() {
        return "REPAIR";
    }

    @Override
    public void apply(EssenceArrays plugin, Formation formation, Location center, Map<?, ?> config, Collection<LivingEntity> nearbyEntities, List<Block> nearbyBlocks, UUID ownerId) {
        if (nearbyEntities == null) return;

        int amount = EffectUtils.getIntFromConfig(config, "value", 5);
        String targetType = EffectUtils.getStringFromConfig(config, "target", "UNDAMAGEABLE").toUpperCase();
        Player owner = (ownerId != null) ? Bukkit.getPlayer(ownerId) : null;

        for (LivingEntity entity : nearbyEntities) {
            boolean shouldRepair = false;
            switch (targetType) {
                case "OWNER":
                    if (owner != null && entity.getUniqueId().equals(owner.getUniqueId())) {
                        shouldRepair = true;
                    }
                    break;
                case "ALL":
                    shouldRepair = true;
                    break;
                case "MOBS":
                    if (entity instanceof Monster) {
                        shouldRepair = true;
                    }
                    break;
                case "DAMAGEABLE":
                    if (entity instanceof Monster || (entity instanceof Player && owner != null && !plugin.getTeamManager().isAlly(owner, (Player) entity))) {
                        shouldRepair = true;
                    }
                    break;
                case "UNDAMAGEABLE":
                    if (entity instanceof Animals || (entity instanceof Player && owner != null && plugin.getTeamManager().isAlly(owner, (Player) entity))) {
                        shouldRepair = true;
                    }
                    break;
            }

            if (shouldRepair && entity instanceof Player) {
                Player player = (Player) entity;
                repairItemInSlot(player.getInventory().getItemInMainHand(), amount);
                repairItemInSlot(player.getInventory().getItemInOffHand(), amount);
                for (ItemStack armor : player.getInventory().getArmorContents()) {
                    repairItemInSlot(armor, amount);
                }
            }
        }
    }

    private void repairItemInSlot(ItemStack item, int amount) {
        if (item != null && item.getItemMeta() instanceof Damageable) {
            Damageable damageable = (Damageable) item.getItemMeta();
            int currentDamage = damageable.getDamage();
            if (currentDamage > 0) {
                damageable.setDamage(Math.max(0, currentDamage - amount));
                item.setItemMeta((ItemMeta) damageable);
            }
        }
    }
}