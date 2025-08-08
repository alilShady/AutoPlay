package com.alilshady.autoplay.commands;

import com.alilshady.autoplay.AutoPlay;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Tag;


public class AutoWalkCommand implements CommandExecutor {

    private final AutoPlay plugin;
    private final HashMap<UUID, BukkitTask> autoWalkTasks = new HashMap<>();
    private final double targetSpeed = 0.23;
    private final double acceleration = 0.3;
    private final double jumpPower = 0.42;
    private final double swimUpPower = 0.15; // Lực bơi lên để không bị chìm

    public AutoWalkCommand(AutoPlay plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Lệnh này chỉ có thể được sử dụng bởi người chơi.");
            return true;
        }
        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();
        if (autoWalkTasks.containsKey(playerUUID)) {
            autoWalkTasks.get(playerUUID).cancel();
            autoWalkTasks.remove(playerUUID);
            player.sendMessage(ChatColor.RED + "AI đã được tắt.");
        } else {
            player.sendMessage(ChatColor.GREEN + "AI với kỹ năng sinh tồn đã được kích hoạt.");
            startSmartWalkTask(player);
        }
        return true;
    }

    private void startSmartWalkTask(Player player) {
        UUID playerUUID = player.getUniqueId();
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }

                // Nếu người chơi ở trong nước, kích hoạt logic bơi riêng biệt
                if (player.isInWater()) {
                    handleSwimming(player);
                    return; // Dừng lại, không thực hiện logic đi bộ thông thường
                }

                // --- Logic đi bộ trên cạn ---
                Location playerLoc = player.getLocation();
                Vector playerDirection = playerLoc.getDirection().setY(0).normalize();
                Vector currentVelocity = player.getVelocity();

                Vector bestDirection = findBestPath(playerLoc, playerDirection);
                visualizePath(player, bestDirection);

                Vector targetVelocity = bestDirection.multiply(targetSpeed);
                double newVx = currentVelocity.getX() + (targetVelocity.getX() - currentVelocity.getX()) * acceleration;
                double newVz = currentVelocity.getZ() + (targetVelocity.getZ() - currentVelocity.getZ()) * acceleration;
                Vector finalHorizontalVelocity = new Vector(newVx, 0, newVz);

                double finalVy = currentVelocity.getY();
                Block blockInFront = playerLoc.clone().add(playerDirection.clone().multiply(0.5)).getBlock();
                Block blockAbove = playerLoc.clone().add(playerDirection.clone().multiply(0.5)).add(0, 1, 0).getBlock();
                if (player.isOnGround() && isFullBlock(blockInFront) && !blockAbove.getType().isSolid()) {
                    finalVy = jumpPower;
                }

                Vector finalVelocity = finalHorizontalVelocity.setY(finalVy);
                player.setVelocity(finalVelocity);
            }

            @Override
            public synchronized void cancel() {
                super.cancel();
                autoWalkTasks.remove(playerUUID);
            }
        }.runTaskTimer(plugin, 0L, 1L);
        autoWalkTasks.put(playerUUID, task);
    }

    /**
     * Xử lý logic khi AI ở trong nước.
     */
    private void handleSwimming(Player player) {
        Location playerLoc = player.getLocation();
        Vector direction = playerLoc.getDirection().setY(0).normalize();

        // Tìm kiếm một khối đất liền xung quanh để trèo lên
        Block land = findNearestLand(playerLoc);

        Vector finalVelocity;

        if (land != null) {
            // Nếu tìm thấy đất liền, bơi về hướng đó và cố gắng trèo lên
            direction = land.getLocation().toVector().subtract(playerLoc.toVector()).normalize();
            finalVelocity = direction.multiply(targetSpeed);
            finalVelocity.setY(jumpPower); // Dùng lực nhảy để vọt lên bờ
            visualizePath(player, direction);
        } else {
            // Nếu xung quanh toàn là nước, chỉ bơi về phía trước và giữ cho đầu không bị chìm
            finalVelocity = direction.multiply(targetSpeed * 0.8); // Bơi chậm hơn đi bộ
            finalVelocity.setY(swimUpPower);
        }
        player.setVelocity(finalVelocity);
    }

    /**
     * Tìm khối đất liền gần nhất trong bán kính 3 block.
     */
    private Block findNearestLand(Location center) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                // Bỏ qua khối ở chính giữa (vị trí của người chơi)
                if (x == 0 && z == 0) continue;

                Block relativeBlock = center.clone().add(x, 0, z).getBlock();
                // Một khối được coi là "đất liền" nếu nó không phải là không khí và không phải là chất lỏng
                if (relativeBlock.getType() != Material.AIR && !relativeBlock.isLiquid()) {
                    // Kiểm tra xem có đủ không gian phía trên để trèo lên không
                    if(relativeBlock.getRelative(0,1,0).getType() == Material.AIR) {
                        return relativeBlock;
                    }
                }
            }
        }
        return null; // Không tìm thấy đất liền
    }

    private double calculateCost(Location playerLoc, Vector direction) {
        Location targetLoc = playerLoc.clone().add(direction.multiply(1.2));
        double cost = 0;

        Block floor = targetLoc.clone().add(0, -1, 0).getBlock();
        Block middle = targetLoc.getBlock();
        Block top = targetLoc.clone().add(0, 1, 0).getBlock();

        // Ưu tiên kiểm tra chất lỏng trước tiên
        if (middle.isLiquid() || floor.isLiquid()) {
            return 500; // Gán một chi phí rất cao và trả về ngay lập tức
        }

        if (middle.getType() == Material.LAVA || floor.getType() == Material.LAVA) cost += 1000;
        if (isFullBlock(middle)) cost += 100;
        if (top.getType().isSolid()) cost += 100;

        if (!floor.getType().isSolid() && !isSteppable(floor)) {
            Block oneBlockDown = targetLoc.clone().add(0, -2, 0).getBlock();
            Block twoBlocksDown = targetLoc.clone().add(0, -3, 0).getBlock();
            if (oneBlockDown.getType().isSolid() || isSteppable(oneBlockDown)) {
                cost -= 5;
            } else if (twoBlocksDown.getType().isSolid() || isSteppable(twoBlocksDown)) {
                cost += 10;
            } else {
                cost += 100;
            }
        }
        return cost;
    }

    private Vector findBestPath(Location playerLoc, Vector playerDirection) {
        HashMap<Vector, Double> costMap = new HashMap<>();
        Vector right = new Vector(playerDirection.getZ(), 0, -playerDirection.getX()).normalize();
        Vector left = right.clone().multiply(-1);

        Vector[] directionsToScan = {
                playerDirection, // Thẳng
                playerDirection.clone().add(left).normalize(),  // Chéo trái
                playerDirection.clone().add(right).normalize(), // Chéo phải
                left,  // Ngang trái
                right  // Ngang phải
        };

        for (Vector dir : directionsToScan) {
            costMap.put(dir, calculateCost(playerLoc, dir));
        }

        Vector bestDirection = playerDirection;
        double minCost = costMap.getOrDefault(playerDirection, 1000.0);
        for (Vector dir : costMap.keySet()) {
            if (costMap.get(dir) < minCost) {
                minCost = costMap.get(dir);
                bestDirection = dir;
            }
        }
        if (minCost >= 100) return new Vector(0,0,0);
        return bestDirection;
    }

    private void visualizePath(Player player, Vector direction) {
        if (direction.lengthSquared() == 0) return;
        Location start = player.getLocation();
        World world = player.getWorld();
        for (double i = 0.5; i < 3.0; i += 0.25) {
            Location point = start.clone().add(direction.clone().multiply(i));
            Location groundPoint = world.getHighestBlockAt(point).getLocation().add(0, 1.1, 0);
            player.spawnParticle(Particle.NOTE, groundPoint, 1, 0, 0, 0, 0);
        }
    }

    private boolean isFullBlock(Block block) {
        return block.getType().isOccluding();
    }

    private boolean isSteppable(Block block) {
        return Tag.SLABS.isTagged(block.getType()) || Tag.STAIRS.isTagged(block.getType());
    }

    public void stopAllTasks() {
        for (BukkitTask task : autoWalkTasks.values()) {
            task.cancel();
        }
        autoWalkTasks.clear();
    }
}