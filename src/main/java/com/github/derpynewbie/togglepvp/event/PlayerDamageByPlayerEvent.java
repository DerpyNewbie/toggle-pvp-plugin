package com.github.derpynewbie.togglepvp.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerEvent;

public class PlayerDamageByPlayerEvent extends PlayerEvent implements Cancellable {

    private static HandlerList handlers = new HandlerList();
    private boolean cancel;

    private Player damager;

    public PlayerDamageByPlayerEvent(Player who, Player damager, EntityDamageByEntityEvent event) {
        super(who);
        this.damager = damager;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public boolean isCancelled() {
        return cancel;
    }

    public void setCancelled(boolean b) {
        this.cancel = b;
    }

    public Player getDamager() {
        return damager;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
