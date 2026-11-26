package dev.rawify.rawcarry.command;

import dev.rawify.rawcarry.RawCarry;
import dev.rawify.rawcarry.carry.CarryManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CarryCommand implements CommandExecutor, TabCompleter {

    private final RawCarry plugin;

    public CarryCommand(RawCarry plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rawcarry.use")) {
            sender.sendMessage(Component.text("You don't have permission to do that."));
            return true;
        }
        if (!(sender instanceof Player carrier)) {
            sender.sendMessage(Component.text("Only players can carry other players."));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /carry <player> | /carry toggle"));
            return true;
        }
        CarryManager carryManager = plugin.getCarryManager();
        if (args[0].equalsIgnoreCase("toggle")) {
            boolean pickUpBlocked = carryManager.toggle(carrier);
            carrier.sendMessage(Component.text(pickUpBlocked ? "You will no longer be picked up." : "You can be picked up again."));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("That player is not online."));
            return true;
        }
        if (target.equals(carrier)) {
            sender.sendMessage(Component.text("You can't carry yourself."));
            return true;
        }
        if (carryManager.isCarrying(carrier)) {
            sender.sendMessage(Component.text("You are already carrying someone."));
            return true;
        }
        if (carryManager.isCarried(target)) {
            sender.sendMessage(Component.text("That player is already being carried."));
            return true;
        }
        if (carryManager.isOnCooldown(carrier, plugin.getConfigAccessor().getCooldownSeconds())) {
            sender.sendMessage(Component.text("You're doing that too quickly."));
            return true;
        }
        double maxDistance = plugin.getConfigAccessor().getMaxDistance();
        if (CarryManager.exceedsMaxDistance(carrier, target, maxDistance)) {
            sender.sendMessage(Component.text("That player is too far away."));
            return true;
        }
        if (carryManager.exceedsStackLimit(carrier, target, plugin.getConfigAccessor().getMaxStack())) {
            sender.sendMessage(Component.text("That tower would be too tall."));
            return true;
        }
        if (carryManager.isToggled(target)) {
            sender.sendMessage(Component.text(target.getName() + " doesn't want to be carried."));
            return true;
        }
        if (!carryManager.carry(carrier, target)) {
            sender.sendMessage(Component.text("You can't carry that player right now."));
            return true;
        }
        carrier.sendActionBar(Component.text("Carrying " + target.getName()));
        target.sendActionBar(Component.text("Carried by " + carrier.getName()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();
        if ("toggle".startsWith(prefix)) {
            names.add("toggle");
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                names.add(online.getName());
            }
        }
        return names;
    }
}
