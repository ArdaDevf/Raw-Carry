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
        CarryManager.CarryBlocker blocker = carryManager.canCarry(carrier, target);
        if (blocker != CarryManager.CarryBlocker.NONE) {
            sender.sendMessage(Component.text(blocker.message(args[0])));
            return true;
        }
        if (!carryManager.carry(carrier, target)) {
            sender.sendMessage(Component.text(CarryManager.CarryBlocker.BUSY.message(target.getName())));
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
