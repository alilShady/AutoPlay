package com.alilshady.tutientranphap.object;

import org.bukkit.Material;
import java.util.List;
import java.util.Map;

public class Formation {
    private final String id;
    private final String displayName;
    private final Material activationItem;
    private final String duration; // Đã đổi từ int durationSeconds
    private final int radius;
    private final Map<Character, Material> patternKey;
    private final List<String> shape;
    private final Material centerBlock;
    private final List<Map<?, ?>> effects;
    private final Map<String, Object> particleConfig;

    public Formation(String id, String displayName, Material activationItem, String duration, int radius, Map<Character, Material> patternKey, List<String> shape, List<Map<?, ?>> effects, Map<String, Object> particleConfig) {
        this.id = id;
        this.displayName = displayName;
        this.activationItem = activationItem;
        this.duration = duration;
        this.radius = radius;
        this.patternKey = patternKey;
        this.shape = shape;
        this.effects = effects;
        this.centerBlock = patternKey.get('X');
        this.particleConfig = particleConfig;
    }

    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getActivationItem() { return activationItem; }
    public String getDuration() { return duration; } // Đã đổi tên từ getDurationSeconds
    public int getRadius() { return radius; }
    public Map<Character, Material> getPatternKey() { return patternKey; }
    public List<String> getShape() { return shape; }
    public Material getCenterBlock() { return centerBlock; }
    public List<Map<?, ?>> getEffects() { return effects; }
    public Map<String, Object> getParticleConfig() { return particleConfig; }
}