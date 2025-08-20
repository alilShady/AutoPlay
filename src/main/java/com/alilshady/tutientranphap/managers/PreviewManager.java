package com.alilshady.tutientranphap.managers;

import com.alilshady.tutientranphap.EssenceArrays;
import com.alilshady.tutientranphap.object.Formation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PreviewManager {

    private final EssenceArrays plugin;
    private final NamespacedKey formationIdKey;
    private BukkitTask previewTask;
    private final Map<UUID, Map<Location, BlockData>> playerPreviews = new HashMap<>();
    private final Map<UUID, Integer> playerRotations = new HashMap<>();

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
        playerRotations.clear();
    }

    public int getRotation(Player player) {
        return playerRotations.getOrDefault(player.getUniqueId(), 0);
    }

    public void rotatePreview(Player player) {
        int currentRotation = getRotation(player);
        int nextRotation = (currentRotation + 90) % 360;
        playerRotations.put(player.getUniqueId(), nextRotation);
        player.sendMessage(plugin.getConfigManager().getMessage("formation.blueprint.rotated", "%degrees%", String.valueOf(nextRotation)));
        updatePreviewForPlayer(player);
    }

    private void updatePreviewForPlayer(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType() == Material.PAPER && itemInHand.hasItemMeta()) {
            ItemMeta meta = itemInHand.getItemMeta();
            if (meta != null) {
                PersistentDataContainer container = meta.getPersistentDataContainer();
                if (container.has(formationIdKey, PersistentDataType.STRING)) {
                    String formationId = container.get(formationIdKey, PersistentDataType.STRING);
                    Formation formation = plugin.getFormationManager().getFormationById(formationId);
                    // Giảm khoảng cách quét khối xuống một chút để trải nghiệm mượt hơn
                    Block targetBlock = player.getTargetBlockExact(5);

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

        Map<Location, BlockData> currentPreview = new HashMap<>();
        playerPreviews.put(player.getUniqueId(), currentPreview);

        List<String> rotatedShape = FormationManager.rotateShape(formation.getShape(), getRotation(player));
        Set<Material> replaceableBlocks = plugin.getConfigManager().getReplaceableBlocks();

        int patternHeight = rotatedShape.size();
        if (patternHeight == 0) return;
        int patternWidth = rotatedShape.get(0).length();
        int centerXOffset = patternWidth / 2;
        int centerZOffset = patternHeight / 2;

        for (int z = 0; z < patternHeight; z++) {
            String row = rotatedShape.get(z);
            for (int x = 0; x < patternWidth; x++) {
                char blockChar = row.charAt(x);
                if (blockChar == ' ') continue;

                Material material = formation.getPatternKey().get(blockChar);
                if (material != null) {
                    Block relativeBlock = center.getBlock().getRelative(x - centerXOffset, 0, z - centerZOffset);
                    currentPreview.put(relativeBlock.getLocation(), relativeBlock.getBlockData());

                    // --- THAY ĐỔI CHÍNH Ở ĐÂY ---
                    // Kiểm tra xem vị trí có thể xây dựng hay không
                    boolean canBuild = replaceableBlocks.contains(relativeBlock.getType());
                    // Nếu có thể xây, hiển thị khối thật. Nếu không, hiển thị kính đỏ.
                    Material previewMaterial = canBuild ? material : Material.RED_STAINED_GLASS;

                    player.sendBlockChange(relativeBlock.getLocation(), previewMaterial.createBlockData());
                }
            }
        }
    }

    public void clearPreview(Player player) {
        Map<Location, BlockData> preview = playerPreviews.remove(player.getUniqueId());
        if (preview != null) {
            for (Map.Entry<Location, BlockData> entry : preview.entrySet()) {
                player.sendBlockChange(entry.getKey(), entry.getValue());
            }
        }
    }
}