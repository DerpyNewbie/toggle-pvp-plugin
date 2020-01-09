package dev.derpynewbie.mc.togglepvp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class TogglePvPPlugin extends JavaPlugin implements CommandExecutor, Listener {

    private static final Pattern TRUE_PATTERN = Pattern.compile("true|enable|yes|on", Pattern.CASE_INSENSITIVE);
    private static final Pattern FALSE_PATTERN = Pattern.compile("false|disable|no|off", Pattern.CASE_INSENSITIVE);
    private static TogglePvPPlugin INSTANCE;
    private static NamespacedKey PVP_KEY;

    @SuppressWarnings("WeakerAccess")
    public static TogglePvPPlugin getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
    public static boolean setPvP(Player player, boolean pvp) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        boolean last = isPvP(player);

        data.set(PVP_KEY, PersistentDataType.BYTE, getByteFromBool(pvp));
        return last;
    }


    // Command

    @SuppressWarnings("WeakerAccess")
    public static boolean isPvP(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();

        return getBoolFromByte(data.getOrDefault(PVP_KEY, PersistentDataType.BYTE, (byte) 0));
    }


    // Event

    private static void sendMessage(Player p, String path, String format) {
        String raw = TogglePvPPlugin.getInstance().getConfig().getString(path);
        String msg = ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);

        if (!msg.isEmpty()) {
            p.sendMessage(String.format(msg, p.getDisplayName(), format));
        }
    }


    // Utils

    private static byte getByteFromBool(boolean bo) {
        return (byte) (bo ? 1 : 0);
    }

    private static boolean getBoolFromByte(byte by) {
        return by == (byte) 1;
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    // Private Utils

    @Override
    public void onEnable() {
        super.onEnable();
        INSTANCE = this;
        PVP_KEY = new NamespacedKey(this, "PvPState");

        saveDefaultConfig();
        reloadConfig();

        Bukkit.getPluginManager().registerEvents(this, this);
        PluginCommand command = this.getCommand("togglepvp");
        if (command == null) {
            getLogger().severe("Could not get plugin command. please report this on " + getDescription().getWebsite() + ".");
            getLogger().severe("Plugin will be disabled due to this problem!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        command.setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Console cannot execute this command.");
            return true;
        }


        Player player = (Player) sender;
        Player target = null;
        boolean pvpState = false;

        if (!player.hasPermission("togglepvp.toggle")) {
            TogglePvPPlugin.sendMessage(player, "message.self.on-perm-lack", player.getName());
            return true;
        }

        if (args.length == 0) { // "/togglepvp"
            target = player;
            pvpState = !TogglePvPPlugin.isPvP(target);
        } else if (args.length == 1) { // "/togglepvp [player|on|off]"
            if (isBoolean(args[0])) {
                target = player;
                pvpState = getBoolFromString(args[0]);
            } else {
                target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    TogglePvPPlugin.sendMessage(player, "message.other.on-offline", args[0]);
                    return true;
                }
                pvpState = !TogglePvPPlugin.isPvP(target);
            }
        } else if (args.length == 2) { // "/togglepvp <player> [on|off]"
            target = Bukkit.getPlayer(args[0]);
            pvpState = getBoolFromString(args[1]);
            if (target == null) {
                TogglePvPPlugin.sendMessage(player, "message.other.on-offline", args[0]);
                return true;
            }
        }

        assert target != null;

        if (player != target && !player.hasPermission("togglepvp.toggle.other")) {
            TogglePvPPlugin.sendMessage(player, "message.other.on-perm-lack", target.getDisplayName());
            return true;
        }

        TogglePvPPlugin.setPvP(target, pvpState);
        TogglePvPPlugin.sendMessage(target, pvpState ? "message.self.on-enable" : "message.self.on-disable", target.getName());
        if (target != player)
            TogglePvPPlugin.sendMessage(player, pvpState ? "message.other.on-enable" : "message.other.on-disable", target.getDisplayName());
        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDamaged(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Player damager = getDamager(event.getDamager());

            if (damager != null) {
                if (!TogglePvPPlugin.isPvP(damager)) {
                    event.setCancelled(true);
                    TogglePvPPlugin.sendMessage(damager, "message.self.on-damage", player.getDisplayName());
                } else if (!TogglePvPPlugin.isPvP(player)) {
                    event.setCancelled(true);
                    TogglePvPPlugin.sendMessage(damager, "message.other.on-damage", player.getDisplayName());
                }
            }
        }
    }

    private boolean getBoolFromString(String s) {
        return TRUE_PATTERN.matcher(s).matches();
    }

    private boolean isBoolean(String s) {
        return TRUE_PATTERN.matcher(s).matches() || FALSE_PATTERN.matcher(s).matches();
    }

    private Player getDamager(Entity e) {
        if (e instanceof Player)
            return (Player) e;
        else if (e instanceof Projectile && ((Projectile) e).getShooter() instanceof Player)
            return (Player) ((Projectile) e).getShooter();
        return null;
    }
}
