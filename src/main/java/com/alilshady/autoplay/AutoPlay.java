package com.alilshady.autoplay;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class AutoPlayPlugin extends JavaPlugin implements CommandExecutor {

    // Chúng ta dùng một HashMap để lưu trạng thái của mỗi người chơi.
    // Key là UUID của người chơi, Value là tác vụ (task) đang chạy cho họ.
    private final HashMap<UUID, BukkitTask> autoWalkTasks = new HashMap<>();

    @Override
    public void onEnable() {
        // Đăng ký lệnh 'autowalk' khi plugin được bật
        this.getCommand("autowalk").setExecutor(this);
        getLogger().info("Plugin AutoPlay đã được bật!");
    }

    @Override
    public void onDisable() {
        // Khi plugin bị tắt, dừng tất cả các tác vụ đang chạy để tránh lỗi
        for (BukkitTask task : autoWalkTasks.values()) {
            task.cancel();
        }
        autoWalkTasks.clear();
        getLogger().info("Plugin AutoPlay đã được tắt.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Chỉ cho phép người chơi (Player) sử dụng lệnh này, không phải Console
        if (!(sender instanceof Player)) {
            sender.sendMessage("Lệnh này chỉ có thể được sử dụng bởi người chơi.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        // Kiểm tra xem người chơi này có đang trong trạng thái tự động đi không
        if (autoWalkTasks.containsKey(playerUUID)) {
            // Nếu có, dừng tác vụ lại
            autoWalkTasks.get(playerUUID).cancel();
            autoWalkTasks.remove(playerUUID);
            player.sendMessage(ChatColor.RED + "Đã tắt chế độ tự động đi.");
        } else {
            // Nếu không, bắt đầu một tác vụ mới
            player.sendMessage(ChatColor.GREEN + "Đã bật chế độ tự động đi. Gõ lại /autowalk để dừng.");

            // Tạo một tác vụ lặp lại (BukkitRunnable)
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    // Kiểm tra nếu người chơi còn online và không bị chết
                    if (!player.isOnline() || player.isDead()) {
                        cancel(); // Tự hủy tác vụ nếu người chơi thoát
                        return;
                    }

                    // Lấy hướng mà người chơi đang nhìn (dưới dạng vector)
                    Vector direction = player.getLocation().getDirection();

                    // Chúng ta chỉ muốn di chuyển trên mặt phẳng X-Z, không bay lên/xuống
                    direction.setY(0).normalize().multiply(0.25); // 0.25 là tốc độ, có thể điều chỉnh

                    // Đặt vận tốc cho người chơi để đẩy họ về phía trước
                    player.setVelocity(direction);
                }
            }.runTaskTimer(this, 0L, 1L); // Chạy ngay lập tức (0L) và lặp lại mỗi 1 tick (khoảng 1/20 giây)

            // Lưu tác vụ này vào HashMap
            autoWalkTasks.put(playerUUID, task);
        }
        return true;
    }
}