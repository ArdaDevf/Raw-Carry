package dev.rawify.rawcarry.listener;

import dev.rawify.rawcarry.RawCarry;
import dev.rawify.rawcarry.carry.CarryManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CarrySafetyListener implements Listener {

    private static final long FALL_PROTECTION_TICKS = 40L;
    private static final long FALL_PROTECTION_MILLIS = FALL_PROTECTION_TICKS * 50L;

    private final RawCarry plugin;
    private final Map<UUID, Long> fallProtectionExpiry = new HashMap<>();

    public CarrySafetyListener(RawCarry plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player passenger)
                || !(event.getDismounted() instanceof Player carrier)) {
            return;
        }
        if (plugin.getConfigAccessor().isFallDamageProtection()) {
            fallProtectionExpiry.put(passenger.getUniqueId(), System.currentTimeMillis() + FALL_PROTECTION_MILLIS);
        }
        CarryManager carryManager = plugin.getCarryManager();
        if (!carryManager.isCarried(passenger)) {
            return;
        }
        Player recordedCarrier = carryManager.getCarrierOf(passenger);
        if (recordedCarrier != null && recordedCarrier.getUniqueId().equals(carrier.getUniqueId())) {
            carryManager.releaseDismounting(recordedCarrier);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        CarryManager carryManager = plugin.getCarryManager();
        if (carryManager.hasActiveCarries() || carryManager.isCarrying(joining) || carryManager.isCarried(joining)) {
            return;
        }
        carryManager.clearLeftoverSlowdown(joining);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Long expiry = fallProtectionExpiry.remove(player.getUniqueId());
        if (expiry == null) {
            return;
        }
        if (System.currentTimeMillis() >= expiry) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player quitting = event.getPlayer();
        releaseCarryLinks(quitting);
        fallProtectionExpiry.remove(quitting.getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dying = event.getEntity();
        releaseCarryLinks(dying);
        fallProtectionExpiry.remove(dying.getUniqueId());
    }

    private void releaseCarryLinks(Player player) {
        CarryManager carryManager = plugin.getCarryManager();
        carryManager.clearCooldown(player);
        Player carrier = carryManager.getCarrierOf(player);
        if (carrier != null) {
            carryManager.drop(carrier);
        }
        if (carryManager.isCarrying(player)) {
            carryManager.drop(player);
        }
    }
}
