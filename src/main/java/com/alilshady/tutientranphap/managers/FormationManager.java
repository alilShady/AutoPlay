package com.alilshady.tutientranphap.managers;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FormationManager {
    private final TuTienTranPhap plugin;
    private List<Formation> loadedFormations;
    private List<Location> activeFormationCenters;

    public FormationManager(TuTienTranPhap plugin) {
        this.plugin = plugin;
        this.loadedFormations = new ArrayList<>();
        this.activeFormationCenters = new ArrayList<>();
    }

    public void loadFormations() {
        this.loadedFormations = plugin.getConfigManager().loadFormations();
    }

    public void attemptToActivate(Player player, Block centerBlock, ItemStack activationItem) {
        if (activeFormationCenters.contains(centerBlock.getLocation())) {
            player.sendMessage(ChatColor.RED + "Trận pháp tại đây đã được kích hoạt.");
            return;
        }

        for (Formation formation : loadedFormations) {
            // Kiểm tra khối trung tâm và vật phẩm kích hoạt
            if (centerBlock.getType() == formation.getCenterBlock() && activationItem.getType() == formation.getActivationItem()) {
                if (isPatternMatch(centerBlock, formation)) {
                    // Kích hoạt thành công
                    activationItem.setAmount(activationItem.getAmount() - 1); // Trừ vật phẩm
                    activeFormationCenters.add(centerBlock.getLocation());
                    plugin.getEffectHandler().startFormationEffects(formation, centerBlock.getLocation());
                    player.sendMessage(formation.getDisplayName() + ChatColor.GREEN + " đã được kích hoạt!");
                    return; // Dừng lại sau khi tìm thấy trận pháp phù hợp
                }
            }
        }
        player.sendMessage(ChatColor.RED + "Mô hình trận pháp không hợp lệ hoặc vật phẩm kích hoạt không đúng.");
    }

    private boolean isPatternMatch(Block centerBlock, Formation formation) {
        List<String> shape = formation.getShape();
        Map<Character, Material> key = formation.getPatternKey();
        int patternHeight = shape.size();
        int patternWidth = shape.get(0).length();

        int centerXOffset = patternWidth / 2;
        int centerZOffset = patternHeight / 2;

        for (int z = 0; z < patternHeight; z++) {
            String row = shape.get(z);
            for (int x = 0; x < patternWidth; x++) {
                char blockChar = row.charAt(x);
                if (blockChar == ' ' || blockChar == 'X') { // Bỏ qua không khí và khối trung tâm (đã kiểm tra)
                    continue;
                }

                Material expectedMaterial = key.get(blockChar);
                if (expectedMaterial == null) {
                    plugin.getLogger().warning("Invalid character '" + blockChar + "' in formation '" + formation.getId() + "'");
                    return false;
                }

                Block relativeBlock = centerBlock.getRelative(x - centerXOffset, 0, z - centerZOffset);
                if (relativeBlock.getType() != expectedMaterial) {
                    return false; // Mô hình không khớp
                }
            }
        }
        return true; // Tất cả các khối đều khớp
    }

    public void deactivateFormation(Location center) {
        activeFormationCenters.remove(center);
    }
}