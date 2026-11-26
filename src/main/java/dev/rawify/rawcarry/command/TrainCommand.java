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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TrainCommand implements CommandExecutor, TabCompleter {

    private final RawCarry plugin;

    public TrainCommand(RawCarry plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rawcarry.use")) {
            sender.sendMessage(Component.text("You don't have permission to do that."));
            return true;
        }
        if (!(sender instanceof Player head)) {
            sender.sendMessage(Component.text("Only players can use this command."));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /train <player1> <player2> ..."));
            return true;
        }
        CarryManager carryManager = plugin.getCarryManager();
        List<Player> participants = new ArrayList<>();
        participants.add(head);
        for (String name : args) {
            String error = resolveError(carryManager, participants, name);
            if (error != null) {
                rollback(carryManager, participants);
                sender.sendMessage(Component.text(error));
                return true;
            }
        }
        for (Player participant : participants) {
            participant.sendActionBar(Component.text("Train started!"));
        }
        return true;
    }

    private String resolveError(CarryManager carryManager, List<Player> participants, String name) {
        Player passenger = Bukkit.getPlayerExact(name);
        Player carrier = participants.get(participants.size() - 1);
        CarryManager.CarryBlocker blocker = carryManager.canCarry(carrier, passenger);
        if (blocker != CarryManager.CarryBlocker.NONE) {
            return blocker.message(name);
        }
        if (!carryManager.carry(carrier, passenger)) {
            return CarryManager.CarryBlocker.BUSY.message(name);
        }
        participants.add(passenger);
        return null;
    }

    private void rollback(CarryManager carryManager, List<Player> participants) {
        for (int i = participants.size() - 2; i >= 0; i--) {
            Player carrier = participants.get(i);
            carryManager.drop(carrier);
            carryManager.clearCooldown(carrier);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return List.of();
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        Set<String> used = new HashSet<>();
        used.add(sender.getName().toLowerCase(Locale.ROOT));
        for (int i = 0; i < args.length - 1; i++) {
            used.add(args[i].toLowerCase(Locale.ROOT));
        }
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            String name = online.getName();
            if (!used.contains(name.toLowerCase(Locale.ROOT))
                    && name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                names.add(name);
            }
        }
        return names;
    }
}
