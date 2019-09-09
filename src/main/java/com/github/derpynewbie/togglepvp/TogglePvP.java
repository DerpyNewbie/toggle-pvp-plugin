package com.github.derpynewbie.togglepvp;

import com.github.derpynewbie.togglepvp.command.TogglePvPCommand;
import com.github.derpynewbie.togglepvp.event.PvPListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;

public class TogglePvP extends JavaPlugin {

    private static final String MESSAGE_ON_PERM_LACK = "message.other.on-perm-lack";
    private static final String PERMISSION_CHANGE_OTHER = "togglepvp.toggle.other";

    private static TogglePvP instance;
    private FileConfiguration config;
    private HashSet<Player> PvPEnabledPlayerSet = new HashSet<Player>();

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;

        loadConfig();
        // Register Event
        Bukkit.getPluginManager().registerEvents(new PvPListener(), this);
        // Register Command
        this.getCommand("TogglePvP").setExecutor(new TogglePvPCommand());
    }

    /**
     * Loads up config.
     */
    public void loadConfig() {
        saveDefaultConfig();
        this.config = getConfig();
    }

    /**
     * Gets configurable message.
     *
     * @param p          The Player that receives message.
     * @param path       The String path of config.yml.
     * @param additional The Additional String information.
     * @return The formatted configurable message.
     * @see "sendConfigMessage"
     */
    public String getConfigMessage(Player p, String path, String additional) {
        String msg = config.getString(path);
        return msg != null ? ChatColor.translateAlternateColorCodes('&', String.format(msg, p.getDisplayName(), additional)) : null;
    }

    /**
     * Sends configurable message to player.
     *
     * @param p          The player that receives message.
     * @param path       The String path of config.yml
     * @param additional The Additional String information
     */
    public void sendConfigMessage(Player p, String path, String additional) {
        String msg = getConfigMessage(p, path, additional);
        if (msg != null)
            p.sendMessage(msg);
    }

    /**
     * Cleanup players status data.
     *
     * @param p The player that gets status cleaned.
     */
    public void cleanPlayerData(Player p) {
        PvPEnabledPlayerSet.remove(p);
    }

    /**
     * Sets players PvP Status.
     *
     * @param p The player that gets pvp status change.
     * @param b The PvP Status.
     * @return The PvP Status.
     */
    public Boolean setPvP(Player sender, Player p, Boolean b) throws IllegalAccessException {
        if (!sender.equals(p) && !sender.hasPermission(PERMISSION_CHANGE_OTHER)) {
            TogglePvP.getInstance().sendConfigMessage(sender, MESSAGE_ON_PERM_LACK, p.getDisplayName());
            throw new IllegalAccessException("Sender of command does not have permission of change other pvp status.");
        }
        if (b == null)
            b = !isPvPEnabled(p);
        addOrRemove(p, b);
        return isPvPEnabled(p);
    }

    /**
     * Gets PvP status.
     *
     * @param p The source of player PvP Status.
     * @return The PvP Status.
     */
    public Boolean isPvPEnabled(Player p) {
        return PvPEnabledPlayerSet.contains(p);
    }

    /**
     * Gets instance of TogglePvP.
     *
     * @return The TogglePvP instance.
     */
    public static TogglePvP getInstance() {
        return instance;
    }

    /**
     * Adds or Removes {@param who} from PvPEnabledPlayerSet.
     *
     * @param who   The Player that gets added or removed.
     * @param isAdd Is the player getting added?
     * @return Current PvP Status of {@param who}
     */
    private Boolean addOrRemove(Player who, Boolean isAdd) {
        if (isAdd)
            PvPEnabledPlayerSet.add(who);
        else
            PvPEnabledPlayerSet.remove(who);
        return isPvPEnabled(who);
    }
}
