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
     * Sends configurable message to player.
     *
     * @param p The player that receives message.
     * @param path The String path of config.yml
     * @param additional The Additional String information
     */
    public void sendConfigMessage(Player p, String path, String additional) {
        String msg = config.getString(path);
        if (msg != null) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format(msg, p.getDisplayName(), additional)));
        }
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
    public Boolean setPvP(Player p, Boolean b) {
        if (b)
            PvPEnabledPlayerSet.add(p);
        else
            PvPEnabledPlayerSet.remove(p);
        return b;
    }

    /**
     * Toggles players PvP Status.
     *
     * @param p The player that toggles PvP.
     * @return The PvP Status.
     */
    public Boolean togglePvP(Player p) {
        if (isPvPEnabled(p)) {
            PvPEnabledPlayerSet.remove(p);
            return false;
        } else {
            PvPEnabledPlayerSet.add(p);
            return true;
        }
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
}
