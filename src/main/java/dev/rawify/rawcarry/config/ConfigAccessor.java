package dev.rawify.rawcarry.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.function.Consumer;
import java.util.logging.Logger;

public final class ConfigAccessor {

    private static final double DEFAULT_MAX_DISTANCE = 3.0;
    private static final int DEFAULT_MAX_STACK = 3;
    private static final int DEFAULT_COOLDOWN_SECONDS = 2;
    private static final int DEFAULT_WEIGHT_SLOWDOWN_LEVEL = 1;
    private static final double DEFAULT_YEET_POWER = 1.2;
    private static final double DEFAULT_YEET_VERTICAL = 0.45;

    private final double maxDistance;
    private final int maxStack;
    private final int cooldownSeconds;
    private final boolean fallDamageProtection;
    private final boolean weightSystem;
    private final int weightSlowdownLevel;
    private final double yeetPower;
    private final double yeetVertical;
    private final boolean effects;

    public ConfigAccessor(FileConfiguration config) {
        this(config, Logger.getLogger("RawCarry")::warning);
    }

    public ConfigAccessor(FileConfiguration config, Consumer<String> warningSink) {
        this.maxDistance = readDouble(config, "max-distance", DEFAULT_MAX_DISTANCE, warningSink);
        this.maxStack = readInt(config, "max-stack", DEFAULT_MAX_STACK, warningSink);
        this.cooldownSeconds = readInt(config, "cooldown-seconds", DEFAULT_COOLDOWN_SECONDS, warningSink);
        this.fallDamageProtection = config.getBoolean("fall-damage-protection", true);
        this.weightSystem = config.getBoolean("weight-system", true);
        this.weightSlowdownLevel = readInt(config, "weight-slowdown-level", DEFAULT_WEIGHT_SLOWDOWN_LEVEL, warningSink);
        this.yeetPower = readDouble(config, "yeet-power", DEFAULT_YEET_POWER, warningSink);
        this.yeetVertical = readDouble(config, "yeet-vertical", DEFAULT_YEET_VERTICAL, warningSink);
        this.effects = config.getBoolean("effects", true);
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public int getMaxStack() {
        return maxStack;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public boolean isFallDamageProtection() {
        return fallDamageProtection;
    }

    public boolean isWeightSystem() {
        return weightSystem;
    }

    public int getWeightSlowdownLevel() {
        return weightSlowdownLevel;
    }

    public double getYeetPower() {
        return yeetPower;
    }

    public double getYeetVertical() {
        return yeetVertical;
    }

    public boolean isEffects() {
        return effects;
    }

    private static double readDouble(FileConfiguration config, String key, double fallback, Consumer<String> warningSink) {
        double value = config.getDouble(key, fallback);
        if (isInvalid(value)) {
            warn(warningSink, key, value, fallback);
            return fallback;
        }
        return value;
    }

    private static int readInt(FileConfiguration config, String key, int fallback, Consumer<String> warningSink) {
        int value = config.getInt(key, fallback);
        if (isInvalid(value)) {
            warn(warningSink, key, value, fallback);
            return fallback;
        }
        return value;
    }

    static boolean isInvalid(double value) {
        return !(value >= 0.0) || !Double.isFinite(value);
    }

    static boolean isInvalid(int value) {
        return value < 0;
    }

    private static void warn(Consumer<String> warningSink, String key, Number value, Number fallback) {
        String message = "Invalid value " + value + " for '" + key + "' in config.yml, falling back to " + fallback
                + ". Valid values are non-negative numbers.";
        if (warningSink != null) {
            warningSink.accept(message);
        } else {
            Logger.getLogger("RawCarry").warning(message);
        }
    }
}
