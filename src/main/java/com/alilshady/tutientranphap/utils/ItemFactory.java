package com.alilshady.tutientranphap.utils;

import com.alilshady.tutientranphap.EssenceArrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemFactory {

    public static final NamespacedKey ACTIVATION_ITEM_KEY = new NamespacedKey(EssenceArrays.getInstance(), "activation_id");

    public static ItemStack createFromConfig(ConfigurationSection section) {
        if (section == null) return null;

        Material material = Material.matchMaterial(section.getString("material", ""));
        if (material == null) return null;

        int amount = section.getInt("amount", 1);
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            boolean isUnique = false; // Biến để theo dõi xem item có phải là độc nhất không

            if (section.contains("name")) {
                Component name = MiniMessage.miniMessage().deserialize(section.getString("name"))
                        .decoration(TextDecoration.ITALIC, false);
                meta.displayName(name);
                isUnique = true; // Có tên tùy chỉnh -> là item độc nhất
            }

            if (section.contains("lore")) {
                List<Component> lore = new ArrayList<>();
                for (String line : section.getStringList("lore")) {
                    lore.add(MiniMessage.miniMessage().deserialize(line)
                            .decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(lore);
                isUnique = true; // Có lore tùy chỉnh -> là item độc nhất
            }

            // --- LOGIC GÁN "DẤU VÂN TAY" ĐÃ ĐƯỢC CẬP NHẬT ---
            // Nếu vật phẩm được xác định là độc nhất (có name hoặc lore), chúng ta sẽ gán tag ẩn.
            if (isUnique) {
                String uniqueId = section.getParent().getName() + ":" + material.name();
                meta.getPersistentDataContainer().set(ACTIVATION_ITEM_KEY, PersistentDataType.STRING, uniqueId);
            }
            // --- KẾT THÚC CẬP NHẬT ---

            item.setItemMeta(meta);

            if (section.contains("enchantments")) {
                for (String enchString : section.getStringList("enchantments")) {
                    String[] parts = enchString.split(":");
                    if (parts.length == 2) {
                        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(parts[0].toLowerCase()));
                        if (enchantment != null) {
                            try {
                                int level = Integer.parseInt(parts[1]);
                                item.addUnsafeEnchantment(enchantment, level);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        }
        return item;
    }
}