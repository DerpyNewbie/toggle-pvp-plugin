package dev.derpynewbie.mc.togglepvp;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.EnumFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class TogglePvPPlugin extends JavaPlugin {

    private static boolean IS_DEBUG = false;

    private static TogglePvPPlugin INSTANCE;
    private static NamespacedKey PVP_KEY;
    private static NamespacedKey TIME_KEY;
    private static long COOLDOWN_TIME = -1;

    static EnumFlag<PvPFlagState> PvPFlag;

    static PotionEffectType type = PotionEffectType.GLOWING;
    static int duration = Integer.MAX_VALUE;
    static int amplifier = 1;
    static boolean ambient = true;
    static boolean particles = true;
    static boolean icon = false;

    @SuppressWarnings("WeakerAccess")
    public static TogglePvPPlugin getInstance() {
        return INSTANCE;
    }


    // Bukkit methods

    static void updateEffect(Player p) {
        if (TogglePvPPlugin.isPvP(p)) {
            p.addPotionEffect(new PotionEffect(type, duration, amplifier, ambient, particles, icon));
        } else {
            p.removePotionEffect(type);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static boolean isPvP(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();

        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));

        PvPFlagState state = set.queryValue(localPlayer, PvPFlag);

        return state == PvPFlagState.FORCE_ALLOW || (state != PvPFlagState.FORCE_DENY && getBoolFromByte(data.getOrDefault(PVP_KEY, PersistentDataType.BYTE, (byte) 0)));
    }

    static void sendMessage(Player p, String path, Object o, boolean cooldown) {
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

    // Maybe future actual API
    @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
    public static boolean setPvP(Player player, boolean pvp) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        boolean last = isPvP(player);

        data.set(PVP_KEY, PersistentDataType.BYTE, getByteFromBool(pvp));
        return last;
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            EnumFlag<PvPFlagState> flag = new EnumFlag<>("force-toggle-pvp", PvPFlagState.class);
            registry.register(flag);
            PvPFlag = flag;
        } catch (FlagConflictException e) {
            try {
                // TODO: Somehow check "existing's type == EnumFlag<PvPFlagState>"
                Flag<?> existing = registry.get("force-toggle-pvp");
                //noinspection unchecked
                PvPFlag = (EnumFlag<PvPFlagState>) existing;
            } catch (ClassCastException e2) {
                getLogger().severe("Flag conflicted. cannot continue.");
                Bukkit.getPluginManager().disablePlugin(this);
            }
        }
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

        Bukkit.getPluginManager().registerEvents(new TogglePvPListener(), this);
        PluginCommand command = this.getCommand("togglepvp");
        assert command != null;
        command.setExecutor(new TogglePvPCommand());

        for (Player p :
                Bukkit.getOnlinePlayers()) {
            updateEffect(p);
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

    public enum PvPFlagState {
        RESPECT_EACH,
        FORCE_ALLOW,
        FORCE_DENY,
        ;
    }

}
