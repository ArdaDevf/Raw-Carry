package dev.rawify.rawcarry;

import dev.rawify.rawcarry.carry.CarryManager;
import dev.rawify.rawcarry.command.CarryCommand;
import dev.rawify.rawcarry.command.TrainCommand;
import dev.rawify.rawcarry.command.UnCarryCommand;
import dev.rawify.rawcarry.config.ConfigAccessor;
import dev.rawify.rawcarry.listener.CarryInteractionListener;
import dev.rawify.rawcarry.listener.CarrySafetyListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class RawCarry extends JavaPlugin {

    private static final long DISTANCE_CHECK_TICKS = 20L;

    private CarryManager carryManager;
    private ConfigAccessor configAccessor;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configAccessor = new ConfigAccessor(getConfig());
        carryManager = new CarryManager(this, configAccessor);
        getServer().getScheduler().runTaskTimer(this, carryManager::enforceMaxDistance,
                DISTANCE_CHECK_TICKS, DISTANCE_CHECK_TICKS);
        getServer().getPluginManager().registerEvents(new CarryInteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new CarrySafetyListener(this), this);
        PluginCommand carry = Objects.requireNonNull(getCommand("carry"), "command carry missing from plugin.yml");
        CarryCommand carryCommand = new CarryCommand(this);
        carry.setExecutor(carryCommand);
        carry.setTabCompleter(carryCommand);
        PluginCommand uncarry = Objects.requireNonNull(getCommand("uncarry"), "command uncarry missing from plugin.yml");
        uncarry.setExecutor(new UnCarryCommand(this));
        PluginCommand train = Objects.requireNonNull(getCommand("train"), "command train missing from plugin.yml");
        TrainCommand trainCommand = new TrainCommand(this);
        train.setExecutor(trainCommand);
        train.setTabCompleter(trainCommand);
        getLogger().info("RawCarry enabled");
    }

    @Override
    public void onDisable() {
        if (carryManager != null) {
            carryManager.dropAll();
        }
        getLogger().info("RawCarry disabled");
    }

    public CarryManager getCarryManager() {
        return carryManager;
    }

    public ConfigAccessor getConfigAccessor() {
        return configAccessor;
    }
}
