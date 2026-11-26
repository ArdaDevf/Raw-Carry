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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public final class CarryInteractionListener implements Listener {

    private static final long DROP_DELAY_TICKS = 10L;
    private static final long LATE_CLICK_TICKS = 2L;

    private final RawCarry plugin;
    private final InteractionTimers timers = new InteractionTimers();

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
        if (!(event.getRightClicked() instanceof Player target)) {
            return;
        }
        CarryManager carryManager = plugin.getCarryManager();
        CarryManager.CarryBlocker blocker = carryManager.canCarry(carrier, target);
        if (blocker != CarryManager.CarryBlocker.NONE) {
            carrier.sendActionBar(Component.text(blocker.message(target.getName())));
            return;
        }
        if (!carryManager.carry(carrier, target)) {
            carrier.sendActionBar(Component.text(CarryManager.CarryBlocker.BUSY.message(target.getName())));
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
        InteractionTimers.Cancelable lateClick = timers.removeLateClick(carrierId);
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
                timers.removePendingDrop(carrierId);
                plugin.getCarryManager().drop(carrier);
            }
        }.runTaskLater(plugin, DROP_DELAY_TICKS);
        timers.putPendingDrop(carrierId, task::cancel);
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
        if (carrier.isSneaking() || timers.hasPendingDrop(carrierId)) {
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
                timers.removeLateClick(carrierId);
            }
        }.runTaskLater(plugin, LATE_CLICK_TICKS);
        timers.putLateClick(carrierId, task::cancel);
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
                && !timers.hasPendingDrop(carrierId)
                && !timers.hasLateClick(carrierId)) {
            return;
        }
        event.setCancelled(true);
        cancelPendingDrop(carrierId);
        cancelLateClick(carrierId);
        if (plugin.getCarryManager().yeet(carrier)) {
            carrier.sendActionBar(Component.text("Yeeted!"));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        timers.cancelAll(event.getPlayer().getUniqueId());
    }

    private void cancelPendingDrop(UUID carrierId) {
        InteractionTimers.Cancelable task = timers.removePendingDrop(carrierId);
        if (task != null) {
            task.cancel();
        }
    }

    private void cancelLateClick(UUID carrierId) {
        InteractionTimers.Cancelable task = timers.removeLateClick(carrierId);
        if (task != null) {
            task.cancel();
        }
    }
}
