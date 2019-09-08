package com.github.derpynewbie.togglepvp.command;

import com.github.derpynewbie.togglepvp.TogglePvP;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

public class TogglePvPCommand implements CommandExecutor {

    // true/false pattern
    private static final Pattern TRUE_PATTERN = Pattern.compile("true|enable|yes|on", Pattern.CASE_INSENSITIVE);
    private static final Pattern FALSE_PATTERN = Pattern.compile("false|disable|no|off", Pattern.CASE_INSENSITIVE);

    private static final String PERMISSION_CHANGE_OTHER = "togglepvp.toggle.other";

    // Configurable message path
    private static final String MESSAGE_ON_ENABLE = "message.self.on-enable";
    private static final String MESSAGE_ON_DISABLE = "message.self.on-disable";
    private static final String MESSAGE_ON_ENABLE_OTHER = "message.other.on-enable";
    private static final String MESSAGE_ON_DISABLE_OTHER = "message.other.on-disable";
    private static final String MESSAGE_ON_OFFLINE_OTHER = "message.other.on-offline";
    private static final String MESSAGE_ON_PERM_LACK = "message.other.on-perm-lack";

    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        // Only for players
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("Console cannot use this command.");
            return true;
        }

        Player sender = (Player) commandSender;
        Player target;
        Boolean isEnable;

        switch (strings.length) {
            case 0: { // Case of "/togglePvP"
                target = sender;
                isEnable = null;
                break;
            }
            case 1: { // Case of "/togglePvP [on|off]" or "/togglePvP <player>"
                if (getBoolFromString(strings[0]) == null) {
                    if (isOnlinePlayer(strings[0])) {
                        target = Bukkit.getPlayer(strings[0]);
                        isEnable = null;
                        break;
                    } else {
                        TogglePvP.getInstance().sendConfigMessage(sender, MESSAGE_ON_OFFLINE_OTHER, strings[0]);
                        return true;
                    }
                } else {
                    target = sender;
                    isEnable = getBoolFromString(strings[0]);
                    break;
                }
            }
            default:
            case 2: { // Case of "/togglePvP <player> [on|off]"
                if (isOnlinePlayer(strings[0])) {
                    if (getBoolFromString(strings[1]) != null) {
                        target = Bukkit.getPlayer(strings[0]);
                        isEnable = getBoolFromString(strings[1]);
                        break;
                    } else
                        return false;
                } else {
                    TogglePvP.getInstance().sendConfigMessage(sender, MESSAGE_ON_OFFLINE_OTHER, strings[0]);
                    return true;
                }
            }
        }

        Boolean result = setPvP(sender, target, isEnable);

        // Send message
        if (result == null)
            return true;
        else if (result)
            sendMessage(target, sender, MESSAGE_ON_ENABLE, MESSAGE_ON_ENABLE_OTHER);
        else
            sendMessage(target, sender, MESSAGE_ON_DISABLE, MESSAGE_ON_DISABLE_OTHER);
        return true;
    }

    private void sendMessage(Player target, Player sender, String path, String path2) {
        TogglePvP.getInstance().sendConfigMessage(target, path, sender.getDisplayName());
        if (target != null && !sender.equals(target)) {
            TogglePvP.getInstance().sendConfigMessage(sender, path2, target.getDisplayName());
        }
    }

    private Boolean setPvP(Player sender, Player target, Boolean b) {
        if (!sender.equals(target) && !sender.hasPermission(PERMISSION_CHANGE_OTHER)) {
            TogglePvP.getInstance().sendConfigMessage(sender, MESSAGE_ON_PERM_LACK, target.getDisplayName());
            return null; // Should throw exception here?
        }

        if (b == null)
            return TogglePvP.getInstance().togglePvP(target);
        else
            return TogglePvP.getInstance().setPvP(target, b);
    }

    private Boolean isOnlinePlayer(String s) {
        return Bukkit.getPlayer(s) != null;
    }

    private Boolean getBoolFromString(String s) {
        if (TRUE_PATTERN.matcher(s).matches())
            return true;
        else if (FALSE_PATTERN.matcher(s).matches())
            return false;
        else
            return null;
    }
}
