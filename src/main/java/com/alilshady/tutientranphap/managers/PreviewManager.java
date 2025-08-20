package com.alilshady.tutientranphap.managers;

import com.alilshady.tutientranphap.EssenceArrays;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData; // <-- SỬA LỖI: Thêm import chính xác
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PreviewManager {

    private final EssenceArrays plugin;
    private final NamespacedKey formationIdKey;
    private BukkitTask previewTask;

    // SỬA LỖI: Thay đổi "Block.BlockData" thành "BlockData"
    private final Map<UUID, Map<Location, BlockData>> playerPreviews = new HashMap<>();

    public PreviewManager(EssenceArrays plugin) {
        this.plugin = plugin;
        this.formationIdKey = new NamespacedKey(plugin, "formation_id");
    }

    public void startPreviewTask() {
        if (previewTask != null && !previewTask.isCancelled()) {
            previewTask.cancel();
        }
        previewTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    updatePreviewForPlayer(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    public void stopPreviewTask() {
        if (previewTask != null) {
            previewTask.cancel();
            previewTask = null;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            clearPreview(player);
        }
        playerPreviews.clear();
    }

    private void updatePreviewForPlayer(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType() == Material.PAPER && itemInHand.hasItemMeta()) {
            ItemMeta meta = itemInHand.getItemMeta();
            if (meta != null) { // Thêm kiểm tra null an toàn
                PersistentDataContainer container = meta.getPersistentDataContainer();
                if (container.has(formationIdKey, PersistentDataType.STRING)) {
                    String formationId = container.get(formationIdKey, PersistentDataType.STRING);
                    Formation formation = plugin.getFormationManager().getFormationById(formationId);
                    Block targetBlock = player.getTargetBlockExact(10);

                    if (formation != null && targetBlock != null) {
                        showPreview(player, formation, targetBlock.getLocation());
                        return;
                    }
                }
            }
        }
        clearPreview(player);
    }

    public void showPreview(Player player, Formation formation, Location center) {
        clearPreview(player);

        // SỬA LỖI: Thay đổi "Block.BlockData" thành "BlockData"
        Map<Location, BlockData> currentPreview = new HashMap<>();
        playerPreviews.put(player.getUniqueId(), currentPreview);

        int patternHeight = formation.getShape().size();
        int patternWidth = formation.getShape().get(0).length();
        int centerXOffset = patternWidth / 2;
        int centerZOffset = patternHeight / 2;

        for (int z = 0; z < patternHeight; z++) {
            String row = formation.getShape().get(z);
            for (int x = 0; x < patternWidth; x++) {
                char blockChar = row.charAt(x);
                if (blockChar == ' ') continue;

                Material material = formation.getPatternKey().get(blockChar);
                if (material != null) {
                    Block relativeBlock = center.getBlock().getRelative(x - centerXOffset, 0, z - centerZOffset);

                    // SỬA LỖI: Dùng đúng phương thức getBlockData()
                    currentPreview.put(relativeBlock.getLocation(), relativeBlock.getBlockData());

                    boolean canBuild = FormationManager.REPLACEABLE_BLOCKS.contains(relativeBlock.getType());

                    Material previewMaterial = canBuild ? Material.GREEN_STAINED_GLASS : Material.RED_STAINED_GLASS;
                    player.sendBlockChange(relativeBlock.getLocation(), previewMaterial.createBlockData());
                }
            }
        }
    }

    public void clearPreview(Player player) {
        // SỬA LỖI: Thay đổi "Block.BlockData" thành "BlockData"
        Map<Location, BlockData> preview = playerPreviews.remove(player.getUniqueId());
        if (preview != null) {
            // SỬA LỖI: Thay đổi "Block.BlockData" thành "BlockData"
            for (Map.Entry<Location, BlockData> entry : preview.entrySet()) {
                player.sendBlockChange(entry.getKey(), entry.getValue());
            }
        }
    }
}