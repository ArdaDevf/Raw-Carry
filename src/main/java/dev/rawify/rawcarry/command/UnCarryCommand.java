package dev.rawify.rawcarry.command;

import dev.rawify.rawcarry.RawCarry;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class UnCarryCommand implements CommandExecutor {

    private final RawCarry plugin;

    public UnCarryCommand(RawCarry plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rawcarry.use")) {
            sender.sendMessage(Component.text("You don't have permission to do that."));
            return true;
        }
        if (!(sender instanceof Player carrier)) {
            sender.sendMessage(Component.text("Only players can use this command."));
            return true;
        }
        Player passenger = plugin.getCarryManager().getPassengerOf(carrier);
        if (passenger == null) {
            sender.sendMessage(Component.text("You aren't carrying anyone."));
            return true;
        }
        if (!plugin.getCarryManager().drop(carrier)) {
            sender.sendMessage(Component.text("You aren't carrying anyone."));
            return true;
        }
        carrier.sendActionBar(Component.text("Dropped " + passenger.getName()));
        passenger.sendActionBar(Component.text("Put down by " + carrier.getName()));
        return true;
    }
}
