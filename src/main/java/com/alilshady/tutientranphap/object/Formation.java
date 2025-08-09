package com.alilshady.tutientranphap.object;

import org.bukkit.Material;
import java.util.List;
import java.util.Map;

// Lớp này chỉ dùng để lưu trữ thông tin, không có logic phức tạp
public class Formation {
    private final String id;
    private final String displayName;
    private final Material activationItem;
    private final int durationSeconds;
    private final int radius;
    private final Map<Character, Material> patternKey;
    private final List<String> shape;
    private final Material centerBlock;
    private final List<Map<?, ?>> effects;

    public Formation(String id, String displayName, Material activationItem, int durationSeconds, int radius, Map<Character, Material> patternKey, List<String> shape, List<Map<?, ?>> effects) {
        this.id = id;
        this.displayName = displayName;
        this.activationItem = activationItem;
        this.durationSeconds = durationSeconds;
        this.radius = radius;
        this.patternKey = patternKey;
        this.shape = shape;
        this.effects = effects;
        this.centerBlock = patternKey.get('X'); // 'X' luôn là khối trung tâm
    }

    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getActivationItem() { return activationItem; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getRadius() { return radius; }
    public Map<Character, Material> getPatternKey() { return patternKey; }
    public List<String> getShape() { return shape; }
    public Material getCenterBlock() { return centerBlock; }
    public List<Map<?, ?>> getEffects() { return effects; }
}