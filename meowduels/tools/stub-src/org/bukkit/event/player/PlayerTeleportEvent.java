package org.bukkit.event.player;

public class PlayerTeleportEvent extends org.bukkit.event.player.PlayerMoveEvent {
    public org.bukkit.event.player.PlayerTeleportEvent.TeleportCause getCause() { return null; }
    public org.bukkit.entity.Player getPlayer() { return null; }
    public boolean isCancelled() { return false; }
    public class TeleportCause {
    }

}
