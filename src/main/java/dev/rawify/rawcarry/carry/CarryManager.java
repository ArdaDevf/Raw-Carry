package dev.rawify.rawcarry.carry;

import dev.rawify.rawcarry.RawCarry;
import dev.rawify.rawcarry.config.ConfigAccessor;
import net.kyori.adventure.text.Component;
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

    /**
     * Why a pickup is refused; {@link #NONE} means every precondition holds.
     * The enum is the single place that decides and words a refusal.
     */
    public enum CarryBlocker {
        NONE,
        NO_PERMISSION,
        TARGET_OFFLINE,
        SELF_CARRY,
        ALREADY_CARRYING,
        ALREADY_CARRIED,
        ON_COOLDOWN,
        TOO_FAR_AWAY,
        TOWER_TOO_TALL,
        TARGET_OPTED_OUT,
        BUSY;

        public String message(String targetName) {
            String name = targetName == null ? "That player" : targetName;
            return switch (this) {
                case NONE -> "";
                case NO_PERMISSION -> "You don't have permission to do that.";
                case TARGET_OFFLINE -> name + " is not online.";
                case SELF_CARRY -> "You can't carry yourself.";
                case ALREADY_CARRYING -> "You are already carrying someone.";
                case ALREADY_CARRIED -> name + " is already being carried.";
                case ON_COOLDOWN -> "You're doing that too quickly.";
                case TOO_FAR_AWAY -> name + " is too far away.";
                case TOWER_TOO_TALL -> "That tower would be too tall.";
                case TARGET_OPTED_OUT -> name + " doesn't want to be carried.";
                case BUSY -> "You can't carry that player right now.";
            };
        }
    }

    /**
     * The pickup preconditions as a value object, so the policy can be tested
     * without a running server. Field order is the order of the checks.
     */
    public record CarryFacts(
            boolean permitted,
            boolean targetOnline,
            boolean samePlayer,
            boolean carrierFree,
            boolean targetFree,
            boolean cooledDown,
            boolean inRange,
            boolean towerFits,
            boolean targetWilling) {
    }

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

    /**
     * Single guard for picking someone up: permission, target, self-carry, both busy states,
     * cooldown, range and tower height all resolve here. Every entry point and
     * {@link #carry(Player, Player)} itself ask this method, so the checks cannot drift apart.
     */
    public CarryBlocker canCarry(Player carrier, Player target) {
        if (carrier == null) {
            return CarryBlocker.BUSY;
        }
        if (target == null) {
            return CarryBlocker.TARGET_OFFLINE;
        }
        return canCarry(new CarryFacts(
                carrier.hasPermission("rawcarry.use"),
                true,
                carrier.getUniqueId().equals(target.getUniqueId()),
                !carrierToPassenger.containsKey(carrier.getUniqueId()),
                !passengerToCarrier.containsKey(target.getUniqueId()),
                cooledDown(carrier),
                inRange(carrier, target),
                stackFits(carrier, target),
                !isToggled(target)));
    }

    static CarryBlocker canCarry(CarryFacts facts) {
        if (!facts.permitted()) {
            return CarryBlocker.NO_PERMISSION;
        }
        if (!facts.targetOnline()) {
            return CarryBlocker.TARGET_OFFLINE;
        }
        if (facts.samePlayer()) {
            return CarryBlocker.SELF_CARRY;
        }
        if (!facts.carrierFree()) {
            return CarryBlocker.ALREADY_CARRYING;
        }
        if (!facts.targetFree()) {
            return CarryBlocker.ALREADY_CARRIED;
        }
        if (!facts.cooledDown()) {
            return CarryBlocker.ON_COOLDOWN;
        }
        if (!facts.inRange()) {
            return CarryBlocker.TOO_FAR_AWAY;
        }
        if (!facts.towerFits()) {
            return CarryBlocker.TOWER_TOO_TALL;
        }
        if (!facts.targetWilling()) {
            return CarryBlocker.TARGET_OPTED_OUT;
        }
        return CarryBlocker.NONE;
    }

    private boolean cooledDown(Player carrier) {
        return config == null || !isOnCooldown(carrier, config.getCooldownSeconds());
    }

    private boolean inRange(Player carrier, Player target) {
        return config == null || !exceedsMaxDistance(carrier, target, config.getMaxDistance());
    }

    private boolean stackFits(Player carrier, Player target) {
        return config == null || !exceedsStackLimit(carrier, target, config.getMaxStack());
    }

    public boolean carry(Player carrier, Player passenger) {
        if (canCarry(carrier, passenger) != CarryBlocker.NONE) {
            return false;
        }
        UUID carrierId = carrier.getUniqueId();
        UUID passengerId = passenger.getUniqueId();
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

    /**
     * Players in the chain below {@code carrier}, including indirect passengers.
     * This is the weight the carrier actually bears, so it drives the slowdown.
     */
    public int getCarriedCount(Player carrier) {
        if (carrier == null) {
            return 0;
        }
        Set<UUID> visited = new HashSet<>();
        visited.add(carrier.getUniqueId());
        int count = 0;
        Player current = getPassengerOf(carrier);
        while (current != null && visited.add(current.getUniqueId())) {
            count++;
            current = getPassengerOf(current);
        }
        return count;
    }

    /**
     * Slowness amplifier for a carrier: the configured base for the first carried player,
     * one level more for every additional player in the chain below.
     */
    static int weightAmplifier(int baseLevel, int carriedBelow) {
        return Math.max(0, baseLevel + Math.max(0, carriedBelow) - 1);
    }

    public boolean exceedsStackLimit(Player carrier, Player target, int maxStack) {
        return exceedsStackLimit(getTowerDepth(carrier), getTowerDepth(target), maxStack);
    }

    static boolean exceedsStackLimit(int carrierDepth, int targetDepth, int maxStack) {
        int towerDepth = carrierDepth + targetDepth;
        return towerDepth > maxStack + 1;
    }

    /**
     * A chain is out of range when the two players are in different worlds or farther apart
     * than {@code maxDistance}. Vanilla riding never separates them, so this is what keeps the
     * configured limit true while a carry is in progress.
     */
    static boolean exceedsMaxDistance(boolean sameWorld, double distance, double maxDistance) {
        return !sameWorld || distance > maxDistance;
    }

    /**
     * Range check for two players: different worlds count as out of range, and the distance is
     * only computed when both are in the same world.
     */
    public static boolean exceedsMaxDistance(Player carrier, Player target, double maxDistance) {
        if (carrier == null || target == null || !carrier.getWorld().equals(target.getWorld())) {
            return true;
        }
        return exceedsMaxDistance(true, carrier.getLocation().distance(target.getLocation()), maxDistance);
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

    /**
     * Drops every passenger whose carrier has moved out of the configured range.
     * Runs on a repeating task because riding alone never enforces a distance.
     */
    public void enforceMaxDistance() {
        if (config == null) {
            return;
        }
        double maxDistance = config.getMaxDistance();
        for (UUID carrierId : new HashSet<>(carrierToPassenger.keySet())) {
            Player carrier = Bukkit.getPlayer(carrierId);
            if (carrier == null) {
                continue;
            }
            Player passenger = getPassengerOf(carrier);
            if (passenger == null) {
                continue;
            }
            if (!exceedsMaxDistance(carrier, passenger, maxDistance)) {
                continue;
            }
            if (drop(carrier)) {
                carrier.sendActionBar(Component.text("Dropped " + passenger.getName() + " — too far away."));
            }
        }
    }

    private void applyWeightSlowdown(Player carrier) {
        if (config == null || !config.isWeightSystem()) {
            appliedSlowdownAmplifiers.remove(carrier.getUniqueId());
            return;
        }
        int amplifier = weightAmplifier(config.getWeightSlowdownLevel(), getCarriedCount(carrier));
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
