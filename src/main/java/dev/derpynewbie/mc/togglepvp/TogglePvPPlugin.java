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

    private static boolean IS_DEBUG = false;
    private static final Pattern TRUE_PATTERN = Pattern.compile("true|enable|yes|on", Pattern.CASE_INSENSITIVE);
    private static final Pattern FALSE_PATTERN = Pattern.compile("false|disable|no|off", Pattern.CASE_INSENSITIVE);
    private static TogglePvPPlugin INSTANCE;
    private static NamespacedKey PVP_KEY;
    private static NamespacedKey TIME_KEY;
    private static long COOLDOWN_TIME = -1;

    @SuppressWarnings("WeakerAccess")
    public static TogglePvPPlugin getInstance() {
        return INSTANCE;
    }


    // Maybe future actual API
    @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
    public static boolean setPvP(Player player, boolean pvp) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        boolean last = isPvP(player);

        data.set(PVP_KEY, PersistentDataType.BYTE, getByteFromBool(pvp));
        return last;
    }

    @SuppressWarnings("WeakerAccess")
    public static boolean isPvP(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();

        return getBoolFromByte(data.getOrDefault(PVP_KEY, PersistentDataType.BYTE, (byte) 0));
    }

    private static void sendMessage(Player p, String path, Object o, boolean cooldown) {
        String raw = TogglePvPPlugin.getInstance().getConfig().getString(path);
        String msg = ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
        boolean isCoolingDown = isMessageCoolingDown(p);

        sendDebug(p, "message", raw,
                "isMessageEmpty", msg.isEmpty(),
                "isCooldown", cooldown,
                "isCoolingDown", isCoolingDown);

        if (!msg.isEmpty() && (!cooldown || !isMessageCoolingDown(p))) {
            p.sendMessage(formatMessage(msg, p, o));
            if (cooldown)
                setLastMessageTime(p);
        }
    }

    private static String formatMessage(String msg, Player p, Object o) {
        msg = msg.replaceAll("%player%", p.getName()).replaceAll("%player_displayname%", p.getDisplayName());
        if (o instanceof Player) {
            Player objP = (Player) o;
            return msg.replaceAll("%other%", objP.getName()).replaceAll("%other_displayname%", objP.getDisplayName());
        } else {
            return msg.replaceAll("%other%", o.toString().replaceAll("%other_displayname%", o.toString()));
        }
    }

    private static void sendDebug(Player p, Object... values) {
        if (IS_DEBUG) {
            StringBuilder sb = new StringBuilder();
            ChatColor lastColor = ChatColor.DARK_GRAY;

            sb.append(ChatColor.BOLD).append("\nDEBUG:\n");
            for (int i = 0; i < values.length; i++) {
                if (i % 2 == 0) {
                    lastColor = (lastColor == ChatColor.GRAY ? ChatColor.DARK_GRAY : ChatColor.GRAY);
                    sb.append(lastColor).append(values[i]).append(": ");
                } else {
                    ChatColor nextColor = (lastColor == ChatColor.GRAY ? ChatColor.WHITE : ChatColor.GRAY);
                    sb.append(nextColor).append(values[i]).append("\n");
                }

            }

            p.sendMessage(sb.toString());
        }
    }

    private static void setLastMessageTime(Player p) {
        PersistentDataContainer data = p.getPersistentDataContainer();
        long currentTime = System.currentTimeMillis();

        data.set(TIME_KEY, PersistentDataType.LONG, currentTime);
        sendDebug(p, "lastMessageTime", currentTime);
    }


    // Utils

    private static boolean isMessageCoolingDown(Player p) {
        PersistentDataContainer data = p.getPersistentDataContainer();
        Long lastMessageSent = data.getOrDefault(TIME_KEY, PersistentDataType.LONG, -1L);
        Long currentTime = System.currentTimeMillis();

        sendDebug(p, "lastMessageSent", lastMessageSent,
                "currentTime", currentTime,
                "cooldownTime", COOLDOWN_TIME,
                "calcResult", (lastMessageSent - currentTime));

        return (currentTime - lastMessageSent) <= COOLDOWN_TIME;
    }

    private static byte getByteFromBool(boolean bo) {
        return (byte) (bo ? 1 : 0);
    }

    private static boolean getBoolFromByte(byte by) {
        return by == (byte) 1;
    }

    // Bukkit methods
    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        INSTANCE = this;
        PVP_KEY = new NamespacedKey(this, "PvPState");
        TIME_KEY = new NamespacedKey(this, "LastMessage");

        saveDefaultConfig();
        reloadConfig();

        IS_DEBUG = getConfig().getBoolean("debug", false);
        COOLDOWN_TIME = getConfig().getLong("message.cooldown-time", -1);

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
            TogglePvPPlugin.sendMessage(player, "message.self.on-perm-lack", null, false);
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
                    TogglePvPPlugin.sendMessage(player, "message.other.on-offline", args[0], false);
                    return true;
                }
                pvpState = !TogglePvPPlugin.isPvP(target);
            }
        } else if (args.length == 2) { // "/togglepvp <player> [on|off]"
            target = Bukkit.getPlayer(args[0]);
            pvpState = getBoolFromString(args[1]);
            if (target == null) {
                TogglePvPPlugin.sendMessage(player, "message.other.on-offline", args[0], false);
                return true;
            }
        }

        assert target != null;

        if (player != target && !player.hasPermission("togglepvp.toggle.other")) {
            TogglePvPPlugin.sendMessage(player, "message.other.on-perm-lack", target, false);
            return true;
        }

        TogglePvPPlugin.setPvP(target, pvpState);
        TogglePvPPlugin.sendMessage(target, pvpState ? "message.self.on-enable" : "message.self.on-disable", target, false);
        if (target != player)
            TogglePvPPlugin.sendMessage(player, pvpState ? "message.other.on-enable" : "message.other.on-disable", target, false);
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
                    TogglePvPPlugin.sendMessage(damager, "message.self.on-damage", player, true);
                } else if (!TogglePvPPlugin.isPvP(player)) {
                    event.setCancelled(true);
                    TogglePvPPlugin.sendMessage(damager, "message.other.on-damage", player, true);
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
