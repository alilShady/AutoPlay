package com.alilshady.autoplay;

// Import lớp xử lý lệnh mới của chúng ta
import com.alilshady.autoplay.commands.AutoWalkCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoPlay extends JavaPlugin { // Đổi tên class cho ngắn gọn và không cần implements CommandExecutor nữa

    private AutoWalkCommand autoWalkCommand;

    @Override
    public void onEnable() {
        // 1. Khởi tạo lớp xử lý lệnh
        this.autoWalkCommand = new AutoWalkCommand(this);

        // 2. Đăng ký lệnh 'autowalk' và trỏ nó tới lớp xử lý AutoWalkCommand
        this.getCommand("autowalk").setExecutor(autoWalkCommand);

        getLogger().info("Plugin AutoPlay đã được bật!");
        getLogger().info("Các lệnh đã được đăng ký.");
    }

    @Override
    public void onDisable() {
        // 3. Gọi phương thức dừng task từ lớp AutoWalkCommand
        if (autoWalkCommand != null) {
            autoWalkCommand.stopAllTasks();
        }
        getLogger().info("Plugin AutoPlay đã được tắt và các tác vụ đã được dừng.");
    }
}