package com.alilshady.tutientranphap.managers;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class FormationManager {
    private final TuTienTranPhap plugin;
    private Map<Material, List<Formation>> formationsByCenterBlock;
    private final List<Location> activeFormationCenters;
    private Map<String, Formation> formationsById;

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
                if (relativeBlock.getType() != Material.AIR) {
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

    public void attemptToActivate(Player player, Block centerBlock, ItemStack activationItem) {
        if (activeFormationCenters.contains(centerBlock.getLocation())) {
            player.sendMessage(plugin.getConfigManager().getMessage("formation.activate.already-active"));
            return;
        }

        List<Formation> possibleFormations = formationsByCenterBlock.get(centerBlock.getType());
        if (possibleFormations == null) {
            return;
        }

        for (Formation formation : possibleFormations) {
            if (activationItem.getType() == formation.getActivationItem()) {
                if (isPatternMatch(centerBlock, formation)) {
                    activationItem.setAmount(activationItem.getAmount() - 1);
                    activeFormationCenters.add(centerBlock.getLocation());
                    plugin.getEffectHandler().startFormationEffects(formation, centerBlock.getLocation());

                    player.sendMessage(plugin.getConfigManager().getMessage("formation.activate.success", "%formation_name%", formation.getDisplayName()));
                    return;
                }
            }
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
    }
}