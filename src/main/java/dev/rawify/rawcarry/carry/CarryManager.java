package dev.rawify.rawcarry.carry;

import dev.rawify.rawcarry.RawCarry;
import dev.rawify.rawcarry.config.ConfigAccessor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CarryManager {

    private final Map<UUID, UUID> carrierToPassenger = new HashMap<>();
    private final Map<UUID, UUID> passengerToCarrier = new HashMap<>();
    private final Map<UUID, Long> lastCarryAt = new HashMap<>();
    private final Map<UUID, Integer> appliedSlowdownAmplifiers = new HashMap<>();
    private final Set<UUID> toggledPlayers = new HashSet<>();
    private final RawCarry plugin;
    private final ConfigAccessor config;

    public CarryManager(RawCarry plugin, ConfigAccessor config) {
        this.plugin = plugin;
        this.config = config;
    }

    public boolean toggle(Player player) {
        if (player == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        if (toggledPlayers.remove(playerId)) {
            return false;
        }
        toggledPlayers.add(playerId);
        return true;
    }

    public boolean isToggled(Player player) {
        return player != null && toggledPlayers.contains(player.getUniqueId());
    }

    public boolean carry(Player carrier, Player passenger) {
        if (carrier == null || passenger == null) {
            return false;
        }
        UUID carrierId = carrier.getUniqueId();
        UUID passengerId = passenger.getUniqueId();
        if (carrierId.equals(passengerId)) {
            return false;
        }
        if (isToggled(passenger)) {
            return false;
        }
        if (carrierToPassenger.containsKey(carrierId)) {
            return false;
        }
        if (passengerToCarrier.containsKey(passengerId)) {
            return false;
        }
        if (!carrier.addPassenger(passenger)) {
            return false;
        }
        carrierToPassenger.put(carrierId, passengerId);
        passengerToCarrier.put(passengerId, carrierId);
        lastCarryAt.put(carrierId, System.currentTimeMillis());
        try {
            refreshWeightEffects();
            spawnCarryCloud(carrier, passenger);
        } catch (RuntimeException exception) {
            carrierToPassenger.remove(carrierId);
            passengerToCarrier.remove(passengerId);
            lastCarryAt.remove(carrierId);
            carrier.removePassenger(passenger);
            return false;
        }
        return true;
    }

    public boolean drop(Player carrier) {
        if (carrier == null) {
            return false;
        }
        UUID carrierId = carrier.getUniqueId();
        UUID passengerId = carrierToPassenger.remove(carrierId);
        if (passengerId == null) {
            return false;
        }
        passengerToCarrier.remove(passengerId);
        Player passenger = Bukkit.getPlayer(passengerId);
        if (passenger != null) {
            carrier.removePassenger(passenger);
            if (carrier.getPassengers().contains(passenger)) {
                carrierToPassenger.put(carrierId, passengerId);
                passengerToCarrier.put(passengerId, carrierId);
                return false;
            }
            spawnDropCloud(passenger);
        }
        removeWeightSlowdown(carrier);
        refreshWeightEffects();
        return true;
    }

    public boolean yeet(Player carrier) {
        if (carrier == null) {
            return false;
        }
        UUID carrierId = carrier.getUniqueId();
        UUID passengerId = carrierToPassenger.remove(carrierId);
        if (passengerId == null) {
            return false;
        }
        passengerToCarrier.remove(passengerId);
        Player passenger = Bukkit.getPlayer(passengerId);
        if (passenger != null) {
            double power = config != null ? config.getYeetPower() : 1.2;
            double vertical = config != null ? config.getYeetVertical() : 0.45;
            Vector velocity = carrier.getEyeLocation().getDirection().multiply(power).add(new Vector(0, vertical, 0));
            carrier.removePassenger(passenger);
            if (carrier.getPassengers().contains(passenger)) {
                carrierToPassenger.put(carrierId, passengerId);
                passengerToCarrier.put(passengerId, carrierId);
                return false;
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (passenger.isOnline()) {
                    passenger.setVelocity(velocity);
                }
            }, 1L);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (passenger.isOnline()) {
                    passenger.setVelocity(velocity);
                }
            }, 3L);
            spawnDropCloud(passenger);
        }
        removeWeightSlowdown(carrier);
        refreshWeightEffects();
        return true;
    }

    public boolean releaseDismounting(Player carrier) {
        if (carrier == null) {
            return false;
        }
        UUID carrierId = carrier.getUniqueId();
        UUID passengerId = carrierToPassenger.remove(carrierId);
        if (passengerId == null) {
            return false;
        }
        passengerToCarrier.remove(passengerId);
        Player passenger = Bukkit.getPlayer(passengerId);
        if (passenger != null) {
            spawnDropCloud(passenger);
        }
        removeWeightSlowdown(carrier);
        refreshWeightEffects();
        return true;
    }

    public boolean isCarrying(Player carrier) {
        return carrier != null && carrierToPassenger.containsKey(carrier.getUniqueId());
    }

    public boolean isOnCooldown(Player carrier, int cooldownSeconds) {
        if (carrier == null || cooldownSeconds <= 0) {
            return false;
        }
        long lastCarry = lastCarryAt.getOrDefault(carrier.getUniqueId(), 0L);
        return System.currentTimeMillis() - lastCarry < cooldownSeconds * 1000L;
    }

    public void clearCooldown(Player player) {
        if (player != null) {
            lastCarryAt.remove(player.getUniqueId());
        }
    }

    public boolean isCarried(Player passenger) {
        return passenger != null && passengerToCarrier.containsKey(passenger.getUniqueId());
    }

    public Player getPassengerOf(Player carrier) {
        if (carrier == null) {
            return null;
        }
        UUID passengerId = carrierToPassenger.get(carrier.getUniqueId());
        if (passengerId == null) {
            return null;
        }
        return Bukkit.getPlayer(passengerId);
    }

    public Player getCarrierOf(Player passenger) {
        if (passenger == null) {
            return null;
        }
        UUID carrierId = passengerToCarrier.get(passenger.getUniqueId());
        if (carrierId == null) {
            return null;
        }
        return Bukkit.getPlayer(carrierId);
    }

    public int getTowerDepth(Player player) {
        if (player == null) {
            return 0;
        }
        Set<UUID> visited = new HashSet<>();
        visited.add(player.getUniqueId());
        int depth = 1;
        Player current = getCarrierOf(player);
        while (current != null && visited.add(current.getUniqueId())) {
            depth++;
            current = getCarrierOf(current);
        }
        return depth;
    }

    public boolean exceedsStackLimit(Player carrier, Player target, int maxStack) {
        return exceedsStackLimit(getTowerDepth(carrier), getTowerDepth(target), maxStack);
    }

    static boolean exceedsStackLimit(int carrierDepth, int targetDepth, int maxStack) {
        int towerDepth = carrierDepth + targetDepth;
        return towerDepth > maxStack + 1;
    }

    public boolean hasActiveCarries() {
        return !carrierToPassenger.isEmpty();
    }

    public static boolean shouldClearLeftoverSlowdown(boolean isCarrying, boolean isCarried) {
        return !isCarrying && !isCarried;
    }

    public void clearLeftoverSlowdown(Player player) {
        if (player == null) {
            return;
        }
        PotionEffect current = player.getPotionEffect(PotionEffectType.SLOWNESS);
        if (current == null || !hasCarrySlowdownSignature(current)) {
            return;
        }
        player.removePotionEffect(PotionEffectType.SLOWNESS);
    }

    public void dropAll() {
        for (UUID carrierId : new HashSet<>(carrierToPassenger.keySet())) {
            Player carrier = Bukkit.getPlayer(carrierId);
            if (carrier != null) {
                drop(carrier);
                continue;
            }
            UUID passengerId = carrierToPassenger.remove(carrierId);
            if (passengerId == null) {
                continue;
            }
            passengerToCarrier.remove(passengerId);
            Player passenger = Bukkit.getPlayer(passengerId);
            if (passenger != null) {
                passenger.leaveVehicle();
            }
        }
    }

    private void applyWeightSlowdown(Player carrier) {
        if (config == null || !config.isWeightSystem()) {
            appliedSlowdownAmplifiers.remove(carrier.getUniqueId());
            return;
        }
        int amplifier = Math.max(0, config.getWeightSlowdownLevel() + getTowerDepth(carrier) - 1);
        carrier.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, PotionEffect.INFINITE_DURATION, amplifier, true, false));
        appliedSlowdownAmplifiers.put(carrier.getUniqueId(), amplifier);
    }

    private void removeWeightSlowdown(Player carrier) {
        Integer amplifier = appliedSlowdownAmplifiers.remove(carrier.getUniqueId());
        if (amplifier == null) {
            return;
        }
        PotionEffect current = carrier.getPotionEffect(PotionEffectType.SLOWNESS);
        if (current == null || current.getAmplifier() != amplifier || !hasCarrySlowdownSignature(current)) {
            return;
        }
        carrier.removePotionEffect(PotionEffectType.SLOWNESS);
    }

    private void refreshWeightEffects() {
        for (UUID carrierId : new HashSet<>(carrierToPassenger.keySet())) {
            Player carrier = Bukkit.getPlayer(carrierId);
            if (carrier == null) {
                continue;
            }
            if (appliedSlowdownAmplifiers.containsKey(carrierId)) {
                carrier.removePotionEffect(PotionEffectType.SLOWNESS);
            }
            applyWeightSlowdown(carrier);
        }
    }

    private static boolean hasCarrySlowdownSignature(PotionEffect effect) {
        return effect.getDuration() == PotionEffect.INFINITE_DURATION
                && effect.isAmbient()
                && !effect.hasParticles();
    }

    private void spawnCarryCloud(Player carrier, Player passenger) {
        if (config == null || !config.isEffects()) {
            return;
        }
        Location midpoint = carrier.getLocation().toVector()
                .midpoint(passenger.getLocation().toVector())
                .toLocation(carrier.getWorld());
        carrier.getWorld().spawnParticle(Particle.CLOUD, midpoint, 12, 0.25, 0.25, 0.25, 0.02);
    }

    private void spawnDropCloud(Player passenger) {
        if (config == null || !config.isEffects()) {
            return;
        }
        passenger.getWorld().spawnParticle(Particle.CLOUD, passenger.getLocation(), 12, 0.25, 0.25, 0.25, 0.02);
    }
}
