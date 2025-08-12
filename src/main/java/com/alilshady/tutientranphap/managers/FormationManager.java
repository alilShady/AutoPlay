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

    // --- SỬA Ở ĐÂY: Bổ sung thêm các khối đất và cỏ ---
    private static final Set<Material> REPLACEABLE_BLOCKS = new HashSet<>(Arrays.asList(
            Material.GRASS,
            Material.TALL_GRASS,
            Material.FERN,
            Material.LARGE_FERN,
            Material.DEAD_BUSH,
            Material.VINE,
            Material.POPPY,
            Material.DANDELION,
            Material.BLUE_ORCHID,
            Material.ALLIUM,
            Material.AZURE_BLUET,
            Material.RED_TULIP,
            Material.ORANGE_TULIP,
            Material.WHITE_TULIP,
            Material.PINK_TULIP,
            Material.OXEYE_DAISY,
            Material.CORNFLOWER,
            Material.LILY_OF_THE_VALLEY,
            Material.WITHER_ROSE,
            Material.SUNFLOWER,
            Material.LILAC,
            Material.ROSE_BUSH,
            Material.PEONY,
            Material.BROWN_MUSHROOM,
            Material.RED_MUSHROOM,
            Material.SNOW,
            Material.GRASS_BLOCK, // Thêm khối cỏ
            Material.DIRT,       // Thêm đất
            Material.PODZOL,
            Material.COARSE_DIRT,
            Material.MYCELIUM,
            Material.DIRT_PATH   // Thêm đường đất
    ));

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
        if (formationsById == null) return new HashSet<>();
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
                Material blockType = relativeBlock.getType();

                // Sửa ở đây: Dùng danh sách mới REPLACEABLE_BLOCKS
                boolean isReplaceable = (blockType == Material.AIR || REPLACEABLE_BLOCKS.contains(blockType));

                if (!isReplaceable) {
                    if (plugin.getConfigManager().isDebugLoggingEnabled()) {
                        plugin.getLogger().warning("[DEBUG][BUILD] Build failed. Block " + blockType + " at " + relativeBlock.getLocation() + " is not replaceable.");
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

        // --- SỬA LỖI Ở ĐÂY: Truyền tên trận pháp đã được định dạng đúng ---
        player.sendMessage(plugin.getConfigManager().getMessage("formation.blueprint.build-success", "%formation_name%", formation.getDisplayName()));
        return true;
    }

    // ... (Các hàm còn lại giữ nguyên không đổi) ...
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
        String requiredTag = (requiredMeta != null) ? requiredMeta.getPersistentDataContainer().get(ItemFactory.ACTIVATION_ITEM_KEY, PersistentDataType.STRING) : null;

        if (requiredTag != null) {
            if (!itemInHand.hasItemMeta()) return false;
            String handTag = itemInHand.getItemMeta().getPersistentDataContainer().get(ItemFactory.ACTIVATION_ITEM_KEY, PersistentDataType.STRING);
            return requiredTag.equals(handTag) && itemInHand.isSimilar(requiredItem);
        } else {
            if (itemInHand.hasItemMeta()) {
                String handTag = itemInHand.getItemMeta().getPersistentDataContainer().get(ItemFactory.ACTIVATION_ITEM_KEY, PersistentDataType.STRING);
                if(handTag != null) return false;
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
                if (Objects.equals(center.getWorld(), location.getWorld())) {
                    if (center.distanceSquared(location) <= Math.pow(formation.getRadius(), 2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}