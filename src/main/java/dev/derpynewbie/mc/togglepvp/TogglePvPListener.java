package dev.derpynewbie.mc.togglepvp;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class TogglePvPListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDamaged(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Player damager = getDamager(event.getDamager());

            if (damager != null && player != damager) {
                if (!TogglePvPPlugin.isPvP(damager)) {
                    event.setCancelled(true);
                    TogglePvPPlugin.sendMessage(damager, "message.self.on-damage", player, true);
                } else if (!TogglePvPPlugin.isPvP(player)) {
                    event.setCancelled(true);
                    TogglePvPPlugin.sendMessage(damager, "message.other.on-damage", player, true);
                }
            }
            TogglePvPPlugin.updateEffect(player);
        }
    }

    private Player getDamager(Entity e) {
        if (e instanceof Player)
            return (Player) e;
        else if (e instanceof Projectile && ((Projectile) e).getShooter() instanceof Player)
            return (Player) ((Projectile) e).getShooter();
        return null;
    }

}
