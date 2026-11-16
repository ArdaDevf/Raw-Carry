package dev.rawify.rawcarry.config;

import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigAccessor {

    private final FileConfiguration config;

    public ConfigAccessor(FileConfiguration config) {
        this.config = config;
    }

    public double getMaxDistance() {
        return config.getDouble("max-distance", 3.0);
    }

    public int getMaxStack() {
        return config.getInt("max-stack", 3);
    }

    public int getCooldownSeconds() {
        return config.getInt("cooldown-seconds", 2);
    }

    public boolean isFallDamageProtection() {
        return config.getBoolean("fall-damage-protection", true);
    }

    public boolean isWeightSystem() {
        return config.getBoolean("weight-system", true);
    }

    public int getWeightSlowdownLevel() {
        return config.getInt("weight-slowdown-level", 1);
    }

    public double getYeetPower() {
        return config.getDouble("yeet-power", 1.2);
    }

    public double getYeetVertical() {
        return config.getDouble("yeet-vertical", 0.45);
    }

    public boolean isEffects() {
        return config.getBoolean("effects", true);
    }
}
