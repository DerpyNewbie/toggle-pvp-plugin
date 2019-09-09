package com.github.derpynewbie.togglepvp.event;

import com.github.derpynewbie.togglepvp.TogglePvP;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PvPListener implements Listener {

    // Configurable message path
    private static final String MESSAGE_ON_DAMAGE = "message.self.on-damage";
    private static final String MESSAGE_ON_DAMAGE_OTHER = "message.other.on-damage";

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDamaged(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Player damager = getDamager(event.getDamager());
            if (damager != null) {
                if (!TogglePvP.getInstance().isPvPEnabled(damager)) {
                    event.setCancelled(true);
                    TogglePvP.getInstance().sendConfigMessage(damager, MESSAGE_ON_DAMAGE, player.getDisplayName());
                } else if (!TogglePvP.getInstance().isPvPEnabled(player)) {
                    event.setCancelled(true);
                    TogglePvP.getInstance().sendConfigMessage(damager, MESSAGE_ON_DAMAGE_OTHER, player.getDisplayName());
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        TogglePvP.getInstance().cleanPlayerData(event.getPlayer());
    }

    private Player getDamager(Entity e) {
        if (e instanceof Player)
            return (Player) e;
        else if (e instanceof Projectile && ((Projectile) e).getShooter() instanceof Player)
            return (Player) ((Projectile) e).getShooter();
        return null;
    }
}
