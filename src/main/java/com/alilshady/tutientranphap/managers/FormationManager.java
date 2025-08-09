package com.alilshady.tutientranphap.managers;

import com.alilshady.tutientranphap.TuTienTranPhap;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Bukkit; // SỬA LỖI TẠI ĐÂY
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormationManager {
    private final TuTienTranPhap plugin;
    // Tối ưu: sử dụng Map để truy cập nhanh hơn
    private Map<Material, List<Formation>> formationsByCenterBlock;
    private final List<Location> activeFormationCenters;

    public FormationManager(TuTienTranPhap plugin) {
        this.plugin = plugin;
        this.formationsByCenterBlock = new HashMap<>();
        this.activeFormationCenters = new ArrayList<>();
    }

    public void loadFormations() {
        // Tải bất đồng bộ và xử lý kết quả trên thread chính
        plugin.getConfigManager().loadFormationsAsync().thenAcceptAsync(loadedFormations -> {
            Map<Material, List<Formation>> newMap = new HashMap<>();
            for (Formation f : loadedFormations) {
                if (f.getCenterBlock() != null) {
                    newMap.computeIfAbsent(f.getCenterBlock(), k -> new ArrayList<>()).add(f);
                }
            }
            this.formationsByCenterBlock = newMap;

            if (plugin.getConfigManager().isDebugLoggingEnabled()) {
                plugin.getLogger().info("Successfully loaded " + loadedFormations.size() + " formations.");
            }
        }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
    }


    public void attemptToActivate(Player player, Block centerBlock, ItemStack activationItem) {
        if (activeFormationCenters.contains(centerBlock.getLocation())) {
            player.sendMessage(ChatColor.RED + "Trận pháp tại đây đã được kích hoạt.");
            return;
        }

        // Tối ưu: chỉ lấy danh sách các trận pháp có cùng khối trung tâm
        List<Formation> possibleFormations = formationsByCenterBlock.get(centerBlock.getType());
        if (possibleFormations == null) {
            // Không gửi tin nhắn nếu không có trận pháp nào, để tránh spam chat
            return;
        }

        for (Formation formation : possibleFormations) {
            // Kiểm tra vật phẩm kích hoạt
            if (activationItem.getType() == formation.getActivationItem()) {
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
    }

    private boolean isPatternMatch(Block centerBlock, Formation formation) {
        List<String> shape = formation.getShape();
        Map<Character, Material> key = formation.getPatternKey();

        if (shape.isEmpty() || shape.get(0).isEmpty()) {
            return false; // Tránh lỗi nếu shape không hợp lệ
        }

        int patternHeight = shape.size();
        int patternWidth = shape.get(0).length();

        int centerXOffset = patternWidth / 2;
        int centerZOffset = patternHeight / 2;

        for (int z = 0; z < patternHeight; z++) {
            String row = shape.get(z);
            for (int x = 0; x < patternWidth; x++) {
                char blockChar = row.charAt(x);
                // Bỏ qua không khí và khối trung tâm (đã kiểm tra ở bước trước)
                if (blockChar == ' ' || (x == centerXOffset && z == centerZOffset)) {
                    continue;
                }

                Material expectedMaterial = key.get(blockChar);
                if (expectedMaterial == null) {
                    if (plugin.getConfigManager().isDebugLoggingEnabled()) {
                        plugin.getLogger().warning("Invalid character '" + blockChar + "' in formation '" + formation.getId() + "'");
                    }
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