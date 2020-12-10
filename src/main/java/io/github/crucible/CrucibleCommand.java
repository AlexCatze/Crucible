package io.github.crucible;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.cauldron.CauldronHooks;
import net.minecraftforge.common.DimensionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.spigotmc.RestartCommand;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CrucibleCommand extends Command {

    private static final DecimalFormat timeFormat = new DecimalFormat("########0.000");
    private static WeakReference<MinecraftServer> serveReference;

    protected CrucibleCommand(MinecraftServer server) {
        super("crucible");
        serveReference = new WeakReference<>(server);

        StringBuilder usage = new StringBuilder();
        usage.append("&7&m-------------------&7[&bCrucible&7]&m-------------------\n");
        usage.append("&b  >&e crucible tps &7-&a Show tps statistics.\n");
        usage.append("&b  >&e crucible restart &7-&a Restart the server.\n");
        usage.append("&b  >&e crucible info &7-&a Print some information about the server.\n");
        usage.append("&b  >&e crucible chunks &7-&a Print some information about loaded chunks.\n");
        usage.append("&b  >&e crucible heap &7-&a Dump the server heap.\n");
        usage.append("&b  >&e crucible plugins &7-&a Shows all your loaded plugins and mod plugins.\n");
        usage.append("&b  >&e crucible mods &7-&a Shows all your loaded mods.\n");
        setUsage(ChatColor.translateAlternateColorCodes('&', usage.toString()));
        setPermission("crucible");
    }

    public static String generateInfo() {
        StringBuilder info = new StringBuilder();
        info.append("This server is running &3Crucible&r [").append(CrucibleModContainer.instance.getVersion()).append("] (Thermos fork by CrucibleMC Team).\n");
        info.append("&9https://github.com/CrucibleMC/Crucible\n&r");
        info.append("Bukkit API implemented: 1.7.9-R0.3-SNAPSHOT\n");
        info.append("Plugins: ").append(Bukkit.getPluginManager().getPlugins().length).append("\n&r");
        info.append("Mods: ").append(Loader.instance().getActiveModList().size())
                .append(" &r| Loaded: ").append(Loader.instance().getModList().size()).append("\n&r");
        return ChatColor.translateAlternateColorCodes('&' ,info.toString());
    }

    private static String getPluginList() {
        StringBuilder pluginList = new StringBuilder();
        Plugin[] plugins = Bukkit.getPluginManager().getPlugins();

        for (Plugin plugin : plugins) {
            if (pluginList.length() > 0) {
                pluginList.append(ChatColor.WHITE);
                pluginList.append(", ");
            }

            if (Crucible.isModPlugin(plugin))
                pluginList.append(ChatColor.AQUA);
            else
                pluginList.append(plugin.isEnabled() ? ChatColor.GREEN : ChatColor.RED);

            pluginList.append(plugin.getDescription().getName()).append("@").append(plugin.getDescription().getVersion());
        }

        return "(" + plugins.length + "): " + pluginList.toString();
    }

    private static String getModList() {
        StringBuilder modList = new StringBuilder();

        List<ModContainer> mods = Loader.instance().getModList();

        for (ModContainer mod : mods) {
            if (modList.length() > 0) {
                modList.append(ChatColor.WHITE);
                modList.append(", ");
            }

            modList.append(ChatColor.GREEN).append(mod.getName()).append("@").append(mod.getVersion());
        }

        return "(" + mods.size() + "): " + modList.toString();
    }

    private static String getTps() {
        StringBuilder tps = new StringBuilder();
        tps.append("\n&8[&e&l\u26a1&r&8] &7TPS from last 1m, 5m, 15m: &r");

        double[] recentTps = MinecraftServer.getServer().recentTps;

        for (double t : recentTps) {
            tps.append(parseTps(t));
        }

        double meanTickTime = mean(getServer().tickTimeArray) * 1.0E-6D;
        tps.append("\n&r&8[&e&l\u26a1&r&8]&7 Mean tick time: &l").append(timeFormat.format(meanTickTime)).append("&r&7ms&r\n");

        for (Integer dimId : DimensionManager.getIDs()) {
            double worldTickTime = mean(getServer().worldTickTimes.get(dimId)) * 1.0E-6D;
            double worldTPS = Math.min(1000.0 / worldTickTime, CrucibleConfigs.configs.crucible_tickHandler_serverTickRate);
            WorldProvider worldProvider = DimensionManager.getProvider(dimId);
            String name = worldProvider.getDimensionName();
            if (name.equals("Overworld")) {
                name = worldProvider.worldObj.getSaveHandler().getWorldDirectoryName();
            }
            tps.append("&8(&2&l")
                    .append(dimId)
                    .append(" &r&7&o\u279c &r&b")
                    .append(name).append("&r&8) &7Time: &l")
                    .append(timeFormat.format(worldTickTime))
                    .append("&r&7ms TPS: ")
                    .append(parseTps(worldTPS))
                    .append("&r\n");
        }

        return ChatColor.translateAlternateColorCodes('&', tps.toString());
    }

    private static String parseTps(double tps) {
        StringBuilder parsedTps = new StringBuilder();
        if (tps <= 10)
            parsedTps.append("&c&l");
        else if (tps <= 15)
            parsedTps.append("&e&l");
        else
            parsedTps.append("&a&l");

        parsedTps.append(String.format("%.2f", Math.min(Math.round(tps * 100.0) / 100.0, CrucibleConfigs.configs.crucible_tickHandler_serverTickRate))).append("&r ");
        return parsedTps.toString();
    }

    /*
     * Based on ForgeCommand
     */
    private static long mean(long[] values) {
        long sum = 0l;
        for (long v : values) {
            sum += v;
        }

        return sum / values.length;
    }

    /*
     * Based on ForgeCommand
     */
    private static MinecraftServer getServer() {
        return serveReference.get();
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!testPermission(sender))
            return true;
        if (args.length == 0) {
            sender.sendMessage(ChatColor.BLUE + "[Crucible] " + ChatColor.GRAY + "Please specify action");
            sender.sendMessage(usageMessage);
            return true;
        }
        if (args[0].equalsIgnoreCase("tps")) {
            if (!testPermission(sender, "crucible.tps"))
                return true;
            sender.sendMessage(getTps());
        } else if (args[0].equalsIgnoreCase("restart")) {
            if (!testPermission(sender, "crucible.restart"))
                return true;
            RestartCommand.restart(true);
        } else if (args[0].equalsIgnoreCase("info")) {
            if (!testPermission(sender, "crucible.info"))
                return true;
            sender.sendMessage(generateInfo());
        } else if (args[0].equalsIgnoreCase("chunks")) {
            if (!testPermission(sender, "crucible.chunks"))
                return true;
            processChunks(sender, args);
        } else if (args[0].equalsIgnoreCase("heap")) {
            if (!testPermission(sender, "crucible.heap"))
                return true;
            processHeap(sender, args);
        } else if (args[0].equalsIgnoreCase("mods")) {
            if (!testPermission(sender, "crucible.mods"))
                return true;
            sender.sendMessage("Mods " + getModList());
        } else if (args[0].equalsIgnoreCase("plugins")) {
            if (!testPermission(sender, "crucible.plugins"))
                return true;
            sender.sendMessage("Plugins " + getPluginList());
        } else if (args[0].equalsIgnoreCase("")) {
            if (!testPermission(sender, "crucible."))
                return true;
        } else {
            sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
            sender.sendMessage(usageMessage);
        }
        return true;
    }

    public boolean testPermission(CommandSender target, String permission) {
        if (testPermissionSilent(target, permission)) {
            return true;
        }
        target.sendMessage(ChatColor.BLUE + "[Crucible] " + ChatColor.DARK_RED + "I'm sorry, but you do not have permission to perform this command. Please contact the server administrators if you believe that this is an error.");
        return false;
    }

    public boolean testPermissionSilent(CommandSender target, String permission) {
        if (!super.testPermissionSilent(target)) {
            return false;
        }
        for (String p : permission.split(";"))
            if (target.hasPermission(p))
                return true;
        return false;
    }

    private void processHeap(CommandSender sender, String[] args) {
        File file = new File(new File(new File("."), "dumps"), "heap-dump-" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()) + "-server.hprof");
        sender.sendMessage("Writing heap dump to: " + file);
        CauldronHooks.dumpHeap(file, true);
        sender.sendMessage("Heap dump complete.");
    }

    private void processChunks(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "Dimension stats: ");
        for (net.minecraft.world.WorldServer world : MinecraftServer.getServer().worlds) {
            sender.sendMessage(ChatColor.GOLD + "Dimension: " + ChatColor.GRAY + world.provider.dimensionId +
                    ChatColor.GOLD + " Loaded Chunks: " + ChatColor.GRAY + world.theChunkProviderServer.loadedChunkHashMap_KC.size() +
                    ChatColor.GOLD + " Active Chunks: " + ChatColor.GRAY + world.activeChunkSet.size() +
                    ChatColor.GOLD + " Entities: " + ChatColor.GRAY + world.loadedEntityList.size() +
                    ChatColor.GOLD + " Tile Entities: " + ChatColor.GRAY + world.loadedTileEntityList.size()
            );
            sender.sendMessage(ChatColor.GOLD + " Entities Last Tick: " + ChatColor.GRAY + world.entitiesTicked +
                    ChatColor.GOLD + " Tiles Last Tick: " + ChatColor.GRAY + world.tilesTicked +
                    ChatColor.GOLD + " Removed Entities: " + ChatColor.GRAY + world.unloadedEntityList.size() +
                    ChatColor.GOLD + " Removed Tile Entities: " + ChatColor.GRAY + world.field_147483_b.size()
            );
        }

        if ((args.length < 2) || !"dump".equalsIgnoreCase(args[1])) {
            return;
        }
        boolean dumpAll = ((args.length > 2) && "all".equalsIgnoreCase(args[2]));

        File file = new File(new File(new File("."), "chunk-dumps"), "chunk-info-" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()) + "-server.txt");
        sender.sendMessage("Writing chunk info to: " + file);
        CauldronHooks.writeChunks(file, dumpAll);
        sender.sendMessage("Chunk info complete");
    }

}
