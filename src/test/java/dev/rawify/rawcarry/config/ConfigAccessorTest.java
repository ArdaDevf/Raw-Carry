package dev.rawify.rawcarry.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigAccessorTest {

    private static ConfigAccessor load(String yaml, List<String> warnings) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (org.bukkit.configuration.InvalidConfigurationException e) {
            throw new AssertionError("test fixture failed to parse", e);
        }
        return new ConfigAccessor(config, warnings::add);
    }

    @Test
    void defaultsWhenFileIsEmpty() {
        List<String> warnings = new ArrayList<>();
        ConfigAccessor accessor = load("", warnings);
        assertEquals(3.0, accessor.getMaxDistance());
        assertEquals(3, accessor.getMaxStack());
        assertEquals(2, accessor.getCooldownSeconds());
        assertEquals(1, accessor.getWeightSlowdownLevel());
        assertEquals(1.2, accessor.getYeetPower());
        assertEquals(0.45, accessor.getYeetVertical());
        assertTrue(accessor.isFallDamageProtection());
        assertTrue(accessor.isWeightSystem());
        assertTrue(accessor.isEffects());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void validValuesKeptExactly() {
        List<String> warnings = new ArrayList<>();
        ConfigAccessor accessor = load("""
                max-distance: 5.5
                max-stack: 5
                cooldown-seconds: 7
                weight-slowdown-level: 2
                yeet-power: 2.5
                yeet-vertical: 0.8
                fall-damage-protection: false
                weight-system: false
                effects: false
                """, warnings);
        assertEquals(5.5, accessor.getMaxDistance());
        assertEquals(5, accessor.getMaxStack());
        assertEquals(7, accessor.getCooldownSeconds());
        assertEquals(2, accessor.getWeightSlowdownLevel());
        assertEquals(2.5, accessor.getYeetPower());
        assertEquals(0.8, accessor.getYeetVertical());
        assertFalse(accessor.isFallDamageProtection());
        assertFalse(accessor.isWeightSystem());
        assertFalse(accessor.isEffects());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void negativeValuesFallBackWithOneWarningEach() {
        List<String> warnings = new ArrayList<>();
        ConfigAccessor accessor = load("""
                max-distance: -3.0
                max-stack: -2
                cooldown-seconds: -1
                weight-slowdown-level: -5
                yeet-power: -1.2
                yeet-vertical: -0.45
                """, warnings);
        assertEquals(3.0, accessor.getMaxDistance());
        assertEquals(3, accessor.getMaxStack());
        assertEquals(2, accessor.getCooldownSeconds());
        assertEquals(1, accessor.getWeightSlowdownLevel());
        assertEquals(1.2, accessor.getYeetPower());
        assertEquals(0.45, accessor.getYeetVertical());
        assertEquals(6, warnings.size());
        for (String warning : warnings) {
            assertTrue(warning.contains("falling back to"), warning);
        }
        assertTrue(warnings.stream().anyMatch(w -> w.contains("max-distance")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("max-stack")));
    }

    @Test
    void zeroCooldownStaysValid() {
        List<String> warnings = new ArrayList<>();
        ConfigAccessor accessor = load("cooldown-seconds: 0", warnings);
        assertEquals(0, accessor.getCooldownSeconds());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void eachWarningEmittedOncePerKey() {
        List<String> warnings = new ArrayList<>();
        ConfigAccessor accessor = load("max-distance: -1", warnings);
        accessor.getMaxDistance();
        accessor.getMaxDistance();
        assertEquals(1, warnings.stream().filter(w -> w.contains("max-distance")).count());
    }
}
