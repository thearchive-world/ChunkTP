package world.thearchive.chunktp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/**
 * Modern Paper plugin for chunk teleportation
 * Uses Brigadier commands, Adventure Components, and async APIs
 */
public class ChunkTP extends JavaPlugin {

    private MiniMessage miniMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.miniMessage = MiniMessage.miniMessage();
        getLogger().info("ChunkTP v" + getPluginMeta().getVersion() + " enabled!");

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            registerCommands(event.registrar().getDispatcher());
        });
    }

    @Override
    public void onDisable() {
        getLogger().info("ChunkTP disabled!");
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerCommand(dispatcher, "chunktp");
        registerCommand(dispatcher, "ctp");
    }

    private void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        dispatcher.register(
            Commands.literal(name)
                .requires(source -> source.getSender().hasPermission("chunktp.use"))
                .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                    .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                        .executes(this::executeCommand)
                    )
                )
        );
    }

    private int executeCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getExecutor() instanceof Player player)) {
            sendMessage(source.getSender(), "players-only");
            return 1;
        }

        int chunkX = IntegerArgumentType.getInteger(context, "chunkX");
        int chunkZ = IntegerArgumentType.getInteger(context, "chunkZ");

        // Check if coordinates are within hard-coded bounds (ALWAYS - prevents server crashes)
        final int MAX_CHUNK_DISTANCE = 1874999;
        if (chunkX < -MAX_CHUNK_DISTANCE || chunkX > MAX_CHUNK_DISTANCE || chunkZ < -MAX_CHUNK_DISTANCE || chunkZ > MAX_CHUNK_DISTANCE) {
            sendMessage(player, "coordinates-out-of-bounds",
                    Map.of("min", String.valueOf(-MAX_CHUNK_DISTANCE), "max", String.valueOf(MAX_CHUNK_DISTANCE)));
            return 1;
        }

        // Perform async teleportation
        teleportToChunkAsync(player, chunkX, chunkZ);

        return 1; // Command successful
    }

    private void teleportToChunkAsync(Player player, int chunkX, int chunkZ) {
        // Convert chunk coordinates to world coordinates (center of chunk)
        double worldX = chunkX * 16.0 + 8.0;
        double worldZ = chunkZ * 16.0 + 8.0;
        Location targetLocation = new Location(player.getWorld(), worldX, 0, worldZ);

        // Get highest block Y and teleport
        double worldY = targetLocation.getWorld().getHighestBlockYAt((int) worldX, (int) worldZ) + 1;
        Location finalLocation = new Location(player.getWorld(), worldX, worldY, worldZ);

        player.teleportAsync(finalLocation).thenAccept(success -> {
            String worldXStr = String.valueOf((int) worldX);
            String worldZStr = String.valueOf((int) worldZ);
            String chunkXStr = String.valueOf(chunkX);
            String chunkZStr = String.valueOf(chunkZ);

            if (success) {
                sendMessage(player, "teleport-success",
                        Map.of("chunkX", chunkXStr, "chunkZ", chunkZStr,
                                "worldX", worldXStr, "worldZ", worldZStr));
            } else {
                sendMessage(player, "teleport-failed",
                        Map.of("chunkX", chunkXStr, "chunkZ", chunkZStr,
                                "worldX", worldXStr, "worldZ", worldZStr));
            }
        });
    }

    private Component getMessage(String key, Map<String, String> placeholders) {
        String message = getConfig().getString("messages." + key);

        // Replace placeholders
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        return miniMessage.deserialize(message);
    }

    private Component getMessage(String key) {
        return getMessage(key, null);
    }

    private void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(getMessage(key, placeholders));
    }

    private void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, null);
    }
}
