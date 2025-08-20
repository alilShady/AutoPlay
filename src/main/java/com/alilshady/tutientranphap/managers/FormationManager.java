package com.alilshady.tutientranphap.managers;

import com.alilshady.tutientranphap.EssenceArrays;
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
import java.util.stream.Collectors;

public class FormationManager {
    private final EssenceArrays plugin;
    private Map<Material, List<Formation>> formationsByCenterBlock;
    private final List<Location> activeFormationCenters;
    private Map<String, Formation> formationsById;
    private final Map<Location, Formation> activeFormations = new HashMap<>();
    private final Map<Location, UUID> formationOwners = new HashMap<>();

    public static final Set<Material> REPLACEABLE_BLOCKS = new HashSet<>(Arrays.asList(
            Material.AIR, Material.GRASS, Material.TALL_GRASS, Material.FERN,
            Material.LARGE_FERN, Material.DEAD_BUSH, Material.VINE, Material.POPPY,
            Material.DANDELION, Material.BLUE_ORCHID, Material.ALLIUM, Material.AZURE_BLUET,
            Material.RED_TULIP, Material.ORANGE_TULIP, Material.WHITE_TULIP, Material.PINK_TULIP,
            Material.OXEYE_DAISY, Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY,
            Material.WITHER_ROSE, Material.SUNFLOWER, Material.LILAC, Material.ROSE_BUSH,
            Material.PEONY, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.SNOW,
            Material.GRASS_BLOCK, Material.DIRT, Material.PODZOL, Material.COARSE_DIRT,
            Material.MYCELIUM, Material.DIRT_PATH
    ));

    public FormationManager(EssenceArrays plugin) {
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

    public boolean buildFormation(Formation formation, Location startLocation, Player player, int rotation) {
        // Lấy hình dạng đã xoay
        List<String> shape = rotateShape(formation.getShape(), rotation);

        if (shape.isEmpty() || shape.get(0).isEmpty()) return false;

        int patternHeight = shape.size();
        int patternWidth = shape.get(0).length();
        int centerXOffset = patternWidth / 2;
        int centerZOffset = patternHeight / 2;

        Map<Material, Integer> requiredMaterials = new HashMap<>();

        // Vòng lặp 1: Kiểm tra không gian VÀ đếm nguyên liệu
        for (int z = 0; z < patternHeight; z++) {
            String row = shape.get(z);
            for (int x = 0; x < patternWidth; x++) {
                char blockChar = row.charAt(x);
                if (blockChar == ' ') continue;

                Block relativeBlock = startLocation.getBlock().getRelative(x - centerXOffset, 0, z - centerZOffset);
                if (!REPLACEABLE_BLOCKS.contains(relativeBlock.getType())) {
                    if (plugin.getConfigManager().isDebugLoggingEnabled()) {
                        plugin.getLogger().warning("[DEBUG][BUILD] Build failed. Block " + relativeBlock.getType() + " at " + relativeBlock.getLocation() + " is not replaceable.");
                    }
                    player.sendMessage(plugin.getConfigManager().getMessage("formation.blueprint.build-fail-space"));
                    return false;
                }

                Material material = formation.getPatternKey().get(blockChar);
                if (material != null) {
                    requiredMaterials.put(material, requiredMaterials.getOrDefault(material, 0) + 1);
                }
            }
        }

        // Vòng lặp 2: Kiểm tra và trừ nguyên liệu nếu được bật trong config
        if (plugin.getConfigManager().isBlueprintRequiresMaterials()) {
            Map<Material, Integer> missingMaterials = new HashMap<>();

            for (Map.Entry<Material, Integer> entry : requiredMaterials.entrySet()) {
                if (!player.getInventory().containsAtLeast(new ItemStack(entry.getKey()), entry.getValue())) {
                    int amountInInventory = 0;
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item != null && item.getType() == entry.getKey()) {
                            amountInInventory += item.getAmount();
                        }
                    }
                    missingMaterials.put(entry.getKey(), entry.getValue() - amountInInventory);
                }
            }

            if (!missingMaterials.isEmpty()) {
                String missingMaterialsString = missingMaterials.entrySet().stream()
                        .map(entry -> entry.getValue() + "x " + entry.getKey().name().replace("_", " ").toLowerCase())
                        .collect(Collectors.joining(", "));
                player.sendMessage(plugin.getConfigManager().getMessage("formation.blueprint.build-fail-materials", "%materials%", missingMaterialsString));
                return false;
            }

            for (Map.Entry<Material, Integer> entry : requiredMaterials.entrySet()) {
                player.getInventory().removeItem(new ItemStack(entry.getKey(), entry.getValue()));
            }
        }

        // Vòng lặp 3: Thực hiện xây dựng
        for (int z = 0; z < patternHeight; z++) {
            String row = shape.get(z);
            for (int x = 0; x < patternWidth; x++) {
                char blockChar = row.charAt(x);
                if (blockChar == ' ') continue;
                Material material = formation.getPatternKey().get(blockChar);
                if (material != null) {
                    Block relativeBlock = startLocation.getBlock().getRelative(x - centerXOffset, 0, z - centerZOffset);
                    relativeBlock.setType(material);
                }
            }
        }

        player.sendMessage(plugin.getConfigManager().getMessage("formation.blueprint.build-success", "%formation_name%", formation.getDisplayName()));
        return true;
    }

    /**
     * Xoay cấu trúc của trận pháp theo góc 90, 180, 270 độ.
     * @param shape Cấu trúc gốc.
     * @param degrees Góc xoay (phải là bội số của 90).
     * @return Cấu trúc mới đã được xoay.
     */
    public static List<String> rotateShape(List<String> shape, int degrees) {
        if (shape.isEmpty() || degrees % 90 != 0) return shape;

        int rotations = (degrees / 90) % 4;
        if (rotations == 0) return shape;

        List<String> currentShape = new ArrayList<>(shape);
        for (int r = 0; r < rotations; r++) {
            int height = currentShape.size();
            int width = currentShape.get(0).length();
            char[][] newGrid = new char[width][height];

            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    newGrid[j][height - 1 - i] = currentShape.get(i).charAt(j);
                }
            }

            List<String> rotated = new ArrayList<>();
            for (int i = 0; i < width; i++) {
                rotated.add(new String(newGrid[i]));
            }
            currentShape = rotated;
        }
        return currentShape;
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