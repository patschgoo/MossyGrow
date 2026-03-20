package dev.patschgo.mossycobble;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

public final class MossyCobbleGrowPlugin extends JavaPlugin {

    private final BlockListener blockListener = new BlockListener() {
        @Override
        public void onBlockPlace(BlockPlaceEvent event) {
            handleBlockPlace(event);
        }
    };

    private boolean nearWaterEnabled;
    private long nearWaterDelayTicks;
    private int nearWaterRadius;

    private boolean spreadingEnabled;

    private boolean nearMossyEnabled;
    private long nearMossyDelayTicks;
    private int nearMossyRadius;

    @Override
    public void onEnable() {
        ensureConfigDefaults();
        loadSettings();
        getServer().getPluginManager().registerEvent(
                Event.Type.BLOCK_PLACE,
                blockListener,
                Event.Priority.Normal,
                this
        );
        getServer().getLogger().info("[MossyGrow] Enabled.");
    }

    @Override
    public void onDisable() {
        getServer().getLogger().info("[MossyGrow] Disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("mossy")) {
            return false;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("mossy.reload")) {
                sender.sendMessage("You do not have permission to use this command.");
                return true;
            }

            getConfiguration().load();
            loadSettings();
            sender.sendMessage("MossyGrow configuration reloaded.");
            return true;
        }

        if (args.length == 1 && (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off"))) {
            if (!sender.hasPermission("mossy.admin")) {
                sender.sendMessage("You do not have permission to use this command.");
                return true;
            }

            boolean newState = args[0].equalsIgnoreCase("on");
            spreadingEnabled = newState;

            Configuration config = getConfiguration();
            config.setProperty("conversion.enabled", Boolean.valueOf(newState));
            config.save();

            sender.sendMessage("MossyGrow spreading is now " + (newState ? "ON" : "OFF") + ".");
            return true;
        }

        sender.sendMessage("Usage: /" + label + " <reload|on|off>");
        return true;
    }

    private void loadSettings() {
        Configuration config = getConfiguration();

        spreadingEnabled = config.getBoolean("conversion.enabled", true);

        nearWaterEnabled = config.getBoolean("conversion.near-water.enabled", true);
        nearWaterDelayTicks = secondsToTicks(config.getInt("conversion.near-water.delay-seconds", 300));
        nearWaterRadius = Math.max(0, config.getInt("conversion.near-water.radius", 1));

        nearMossyEnabled = config.getBoolean("conversion.near-mossy-cobblestone.enabled", true);
        nearMossyDelayTicks = secondsToTicks(config.getInt("conversion.near-mossy-cobblestone.delay-seconds", 180));
        nearMossyRadius = Math.max(0, config.getInt("conversion.near-mossy-cobblestone.radius", 1));
    }

    private void ensureConfigDefaults() {
        Configuration config = getConfiguration();
        config.load();

        setIfMissing(config, "conversion.enabled", true);

        setIfMissing(config, "conversion.near-water.enabled", true);
        setIfMissing(config, "conversion.near-water.delay-seconds", 300);
        setIfMissing(config, "conversion.near-water.radius", 1);

        setIfMissing(config, "conversion.near-mossy-cobblestone.enabled", true);
        setIfMissing(config, "conversion.near-mossy-cobblestone.delay-seconds", 180);
        setIfMissing(config, "conversion.near-mossy-cobblestone.radius", 1);

        config.save();
    }

    private void setIfMissing(Configuration config, String path, Object value) {
        if (config.getProperty(path) == null) {
            config.setProperty(path, value);
        }
    }

    private long secondsToTicks(long seconds) {
        long clamped = Math.max(0, seconds);
        return clamped * 20L;
    }

    public void handleBlockPlace(BlockPlaceEvent event) {
        if (!spreadingEnabled) {
            return;
        }

        Block block = event.getBlockPlaced();
        if (block.getType() != Material.COBBLESTONE) {
            return;
        }

        if (nearWaterEnabled && nearWaterDelayTicks > 0) {
            scheduleAttempt(block.getWorld(), block.getX(), block.getY(), block.getZ(), nearWaterDelayTicks, TriggerType.NEAR_WATER);
        }

        if (nearMossyEnabled && nearMossyDelayTicks > 0) {
            scheduleAttempt(block.getWorld(), block.getX(), block.getY(), block.getZ(), nearMossyDelayTicks, TriggerType.NEAR_MOSSY);
        }
    }

    private void scheduleAttempt(World world, int x, int y, int z, long delayTicks, TriggerType triggerType) {
        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() != Material.COBBLESTONE) {
                return;
            }

            boolean shouldConvert;
            if (triggerType == TriggerType.NEAR_WATER) {
                shouldConvert = isNearWater(block, nearWaterRadius);
            } else {
                shouldConvert = isNearMossyCobblestone(block, nearMossyRadius);
            }

            if (shouldConvert) {
                block.setType(Material.MOSSY_COBBLESTONE);
            }
            }
        }, delayTicks);
    }

    private boolean isNearWater(Block origin, int radius) {
        World world = origin.getWorld();
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    Block nearby = world.getBlockAt(ox + dx, oy + dy, oz + dz);
                    Material type = nearby.getType();
                    if (type == Material.WATER || type == Material.STATIONARY_WATER) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isNearMossyCobblestone(Block origin, int radius) {
        return isNear(origin, radius, Material.MOSSY_COBBLESTONE);
    }

    private boolean isNear(Block origin, int radius, Material target) {
        World world = origin.getWorld();
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    Block nearby = world.getBlockAt(ox + dx, oy + dy, oz + dz);
                    if (nearby.getType() == target) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private enum TriggerType {
        NEAR_WATER,
        NEAR_MOSSY
    }
}
