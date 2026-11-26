package dev.rawify.rawcarry.listener;

import dev.rawify.rawcarry.RawCarry;
import dev.rawify.rawcarry.carry.CarryManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CarryInteractionListener implements Listener {

    private static final long DROP_DELAY_TICKS = 10L;
    private static final long LATE_CLICK_TICKS = 2L;

    private final RawCarry plugin;
    private final Map<UUID, BukkitTask> pendingDrops = new HashMap<>();
    private final Map<UUID, BukkitTask> lateClicks = new HashMap<>();

    public CarryInteractionListener(RawCarry plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player carrier = event.getPlayer();
        if (!carrier.isSneaking() || !carrier.hasPermission("rawcarry.use")) {
            return;
        }
        if (!(event.getRightClicked() instanceof Player target) || target.equals(carrier)) {
            return;
        }
        CarryManager carryManager = plugin.getCarryManager();
        if (carryManager.isCarrying(carrier)) {
            carrier.sendActionBar(Component.text("You are already carrying someone."));
            return;
        }
        if (carryManager.isCarried(target)) {
            carrier.sendActionBar(Component.text("That player is already being carried."));
            return;
        }
        if (carryManager.isOnCooldown(carrier, plugin.getConfigAccessor().getCooldownSeconds())) {
            carrier.sendActionBar(Component.text("You're doing that too quickly."));
            return;
        }
        double maxDistance = plugin.getConfigAccessor().getMaxDistance();
        if (CarryManager.exceedsMaxDistance(carrier, target, maxDistance)) {
            carrier.sendActionBar(Component.text("That player is too far away."));
            return;
        }
        if (carryManager.exceedsStackLimit(carrier, target, plugin.getConfigAccessor().getMaxStack())) {
            carrier.sendActionBar(Component.text("That tower would be too tall."));
            return;
        }
        if (carryManager.isToggled(target)) {
            carrier.sendActionBar(Component.text(target.getName() + " doesn't want to be carried."));
            return;
        }
        if (!carryManager.carry(carrier, target)) {
            carrier.sendActionBar(Component.text("You can't carry that player right now."));
            return;
        }
        event.setCancelled(true);
        carrier.sendActionBar(Component.text("Carrying " + target.getName()));
        target.sendActionBar(Component.text("Carried by " + carrier.getName()));
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }
        Player carrier = event.getPlayer();
        if (!carrier.hasPermission("rawcarry.use") || !plugin.getCarryManager().isCarrying(carrier)) {
            return;
        }
        UUID carrierId = carrier.getUniqueId();
        BukkitTask lateClick = lateClicks.remove(carrierId);
        if (lateClick != null) {
            lateClick.cancel();
            if (plugin.getCarryManager().yeet(carrier)) {
                carrier.sendActionBar(Component.text("Yeeted!"));
            }
            return;
        }
        cancelPendingDrop(carrierId);
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                pendingDrops.remove(carrierId);
                plugin.getCarryManager().drop(carrier);
            }
        }.runTaskLater(plugin, DROP_DELAY_TICKS);
        pendingDrops.put(carrierId, task);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Player carrier = event.getPlayer();
        if (!carrier.hasPermission("rawcarry.use") || !plugin.getCarryManager().isCarrying(carrier)) {
            return;
        }
        UUID carrierId = carrier.getUniqueId();
        if (carrier.isSneaking() || pendingDrops.containsKey(carrierId)) {
            cancelPendingDrop(carrierId);
            event.setCancelled(true);
            if (plugin.getCarryManager().yeet(carrier)) {
                carrier.sendActionBar(Component.text("Yeeted!"));
            }
            return;
        }
        cancelLateClick(carrierId);
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                lateClicks.remove(carrierId);
            }
        }.runTaskLater(plugin, LATE_CLICK_TICKS);
        lateClicks.put(carrierId, task);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player carrier) || !carrier.hasPermission("rawcarry.use")) {
            return;
        }
        if (!plugin.getCarryManager().isCarrying(carrier)) {
            return;
        }
        UUID carrierId = carrier.getUniqueId();
        if (!carrier.isSneaking()
                && !pendingDrops.containsKey(carrierId)
                && !lateClicks.containsKey(carrierId)) {
            return;
        }
        event.setCancelled(true);
        cancelPendingDrop(carrierId);
        cancelLateClick(carrierId);
        if (plugin.getCarryManager().yeet(carrier)) {
            carrier.sendActionBar(Component.text("Yeeted!"));
        }
    }

    private void cancelPendingDrop(UUID carrierId) {
        BukkitTask task = pendingDrops.remove(carrierId);
        if (task != null) {
            task.cancel();
        }
    }

    private void cancelLateClick(UUID carrierId) {
        BukkitTask task = lateClicks.remove(carrierId);
        if (task != null) {
            task.cancel();
        }
    }
}
