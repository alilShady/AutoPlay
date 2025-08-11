package com.alilshady.tutientranphap.managers;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import com.alilshady.tutientranphap.utils.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class FormationManager {
    private final TuTienTranPhap plugin;
    private Map<Material, List<Formation>> formationsByCenterBlock;
    private final List<Location> activeFormationCenters;
    private Map<String, Formation> formationsById;
    private final Map<Location, Formation> activeFormations = new HashMap<>();
    private final Map<Location, UUID> formationOwners = new HashMap<>();

    public FormationManager(TuTienTranPhap plugin) {
        this.plugin = plugin;
        this.formationsByCenterBlock = new HashMap<>();
        this.activeFormationCenters = new ArrayList<>();
        this.formationsById = new HashMap<>();
    }

    public void loadFormations() {
        plugin.getConfigManager().loadFormationsAsync().thenAcceptAsync(loadedFormations -> {
            Map<Material, List<Formation>> newCenterBlockMap = new HashMap<>();
            Map<String, Formation> newIdMap = new HashMap<>();

            for (Formation f : loadedFormations) {
                newIdMap.put(f.getId(), f);
                if (f.getCenterBlock() != null) {
                    newCenterBlockMap.computeIfAbsent(f.getCenterBlock(), k -> new ArrayList<>()).add(f);
                }
            }
            this.formationsById = newIdMap;
            this.formationsByCenterBlock = newCenterBlockMap;

            if (plugin.getConfigManager().isDebugLoggingEnabled()) {
                plugin.getLogger().info("Successfully loaded " + loadedFormations.size() + " formations.");
            }
        }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
    }

    public Formation getFormationById(String id) {
        return formationsById.get(id);
    }

    public Set<String> getAllFormationIds() {
        if (formationsById == null) {
            return new HashSet<>();
        }
        return formationsById.keySet();
    }

    public boolean buildFormation(Formation formation, Location startLocation, Player player) {
        List<String> shape = formation.getShape();
        Map<Character, Material> key = formation.getPatternKey();
        if (shape.isEmpty() || shape.get(0).isEmpty()) return false;

        int patternHeight = shape.size();
        int patternWidth = shape.get(0).length();
        int centerXOffset = patternWidth / 2;
        int centerZOffset = patternHeight / 2;

        for (int z = 0; z < patternHeight; z++) {
            String row = shape.get(z);
            for (int x = 0; x < patternWidth; x++) {
                char blockChar = row.charAt(x);
                if (blockChar == ' ') continue;

                Block relativeBlock = startLocation.getBlock().getRelative(x - centerXOffset, 0, z - centerZOffset);
                if (relativeBlock.getType() != Material.AIR && !relativeBlock.isPassable()) {
                    if (plugin.getConfigManager().isDebugLoggingEnabled()) {
                        plugin.getLogger().warning("[DEBUG][BUILD] Build failed. Block " + relativeBlock.getType() + " at " + relativeBlock.getLocation() + " is not AIR.");
                    }
                    player.sendMessage(plugin.getConfigManager().getMessage("formation.blueprint.build-fail-space"));
                    return false;
                }
            }
        }

        for (int z = 0; z < patternHeight; z++) {
            String row = shape.get(z);
            for (int x = 0; x < patternWidth; x++) {
                char blockChar = row.charAt(x);
                if (blockChar == ' ') continue;

                Material material = key.get(blockChar);
                if (material != null) {
                    Block relativeBlock = startLocation.getBlock().getRelative(x - centerXOffset, 0, z - centerZOffset);
                    relativeBlock.setType(material);
                }
            }
        }

        player.sendMessage(plugin.getConfigManager().getMessage("formation.blueprint.build-success", "%formation_name%", formation.getDisplayName()));
        return true;
    }

    public void attemptToActivate(Player player, Block centerBlock, ItemStack activationItemInHand) {
        if (activeFormationCenters.contains(centerBlock.getLocation())) {
            player.sendMessage(plugin.getConfigManager().getMessage("formation.activate.already-active"));
            return;
        }

        List<Formation> possibleFormations = formationsByCenterBlock.get(centerBlock.getType());
        if (possibleFormations == null) {
            return;
        }

        for (Formation formation : possibleFormations) {
            if (isSpecialItemMatch(activationItemInHand, formation.getActivationItem())) {
                if (isPatternMatch(centerBlock, formation)) {
                    activationItemInHand.setAmount(activationItemInHand.getAmount() - 1);
                    activeFormationCenters.add(centerBlock.getLocation());
                    activeFormations.put(centerBlock.getLocation(), formation);
                    formationOwners.put(centerBlock.getLocation(), player.getUniqueId());
                    plugin.getEffectHandler().startFormationEffects(formation, centerBlock.getLocation(), player.getUniqueId());
                    player.sendMessage(plugin.getConfigManager().getMessage("formation.activate.success", "%formation_name%", formation.getDisplayName()));
                    return;
                }
            }
        }
    }

    private boolean isSpecialItemMatch(ItemStack itemInHand, ItemStack requiredItem) {
        if (itemInHand == null) return false;

        ItemMeta requiredMeta = requiredItem.getItemMeta();
        // Kiểm tra xem vật phẩm yêu cầu có được đánh dấu là 'unique' không
        String requiredTag = (requiredMeta != null) ? requiredMeta.getPersistentDataContainer().get(ItemFactory.ACTIVATION_ITEM_KEY, PersistentDataType.STRING) : null;

        if (requiredTag != null) {
            // Trường hợp 1: Vật phẩm YÊU CẦU có "dấu vân tay"
            if (!itemInHand.hasItemMeta()) return false;
            String handTag = itemInHand.getItemMeta().getPersistentDataContainer().get(ItemFactory.ACTIVATION_ITEM_KEY, PersistentDataType.STRING);

            // Phải có tag và tag phải giống nhau. Sau đó mới so sánh các thuộc tính khác.
            return requiredTag.equals(handTag) && itemInHand.isSimilar(requiredItem);
        } else {
            // Trường hợp 2: Vật phẩm không yêu cầu "dấu vân tay"
            // So sánh thông thường, nhưng đảm bảo vật phẩm trên tay cũng không có tag.
            // Điều này ngăn việc dùng vật phẩm "xịn" để kích hoạt trận pháp "thường".
            if (itemInHand.hasItemMeta()) {
                String handTag = itemInHand.getItemMeta().getPersistentDataContainer().get(ItemFactory.ACTIVATION_ITEM_KEY, PersistentDataType.STRING);
                if(handTag != null) return false; // Vật phẩm trên tay có tag -> không khớp
            }
            return itemInHand.isSimilar(requiredItem);
        }
    }

    private boolean isPatternMatch(Block centerBlock, Formation formation) {
        List<String> shape = formation.getShape();
        Map<Character, Material> key = formation.getPatternKey();
        if (shape.isEmpty() || shape.get(0).isEmpty()) return false;

        int patternHeight = shape.size();
        int patternWidth = shape.get(0).length();
        int centerXOffset = patternWidth / 2;
        int centerZOffset = patternHeight / 2;

        for (int z = 0; z < patternHeight; z++) {
            String row = shape.get(z);
            for (int x = 0; x < patternWidth; x++) {
                char blockChar = row.charAt(x);
                if (blockChar == ' ') continue;

                Material expectedMaterial = key.get(blockChar);
                if (expectedMaterial == null) {
                    if (plugin.getConfigManager().isDebugLoggingEnabled()) {
                        plugin.getLogger().warning("Invalid character '" + blockChar + "' in formation '" + formation.getId() + "'");
                    }
                    return false;
                }
                if(x == centerXOffset && z == centerZOffset) continue;

                Block relativeBlock = centerBlock.getRelative(x - centerXOffset, 0, z - centerZOffset);
                if (relativeBlock.getType() != expectedMaterial) {
                    return false;
                }
            }
        }
        return true;
    }

    public void deactivateFormation(Location center) {
        activeFormationCenters.remove(center);
        activeFormations.remove(center);
        formationOwners.remove(center);
    }

    public UUID getOwner(Location center) {
        return formationOwners.get(center);
    }

    public boolean isLocationInActiveFormation(Location location, String effectType) {
        for (Map.Entry<Location, Formation> entry : activeFormations.entrySet()) {
            Formation formation = entry.getValue();
            Location center = entry.getKey();

            boolean hasEffect = formation.getEffects().stream()
                    .anyMatch(effectMap -> effectType.equalsIgnoreCase(String.valueOf(effectMap.get("type"))));

            if (hasEffect) {
                if (center.getWorld().equals(location.getWorld())) {
                    if (center.distanceSquared(location) <= Math.pow(formation.getRadius(), 2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}