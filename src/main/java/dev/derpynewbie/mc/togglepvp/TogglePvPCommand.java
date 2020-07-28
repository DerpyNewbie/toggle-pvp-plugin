package dev.derpynewbie.mc.togglepvp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class TogglePvPCommand implements CommandExecutor {

    private static final Pattern TRUE_PATTERN = Pattern.compile("true|enable|yes|on", Pattern.CASE_INSENSITIVE);
    private static final Pattern FALSE_PATTERN = Pattern.compile("false|disable|no|off", Pattern.CASE_INSENSITIVE);

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
        TogglePvPPlugin.updateEffect(target);
        return true;
    }

    private boolean getBoolFromString(String s) {
        return TRUE_PATTERN.matcher(s).matches();
    }

    private boolean isBoolean(String s) {
        return TRUE_PATTERN.matcher(s).matches() || FALSE_PATTERN.matcher(s).matches();
    }

}
