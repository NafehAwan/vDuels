package org.bukkit.event.entity;

public class EntityDamageEvent {
    public org.bukkit.event.entity.EntityDamageEvent.DamageCause getCause() { return null; }
    public org.bukkit.entity.Entity getEntity() { return null; }
    public double getFinalDamage() { return 0.0; }
    public void setCancelled(boolean a0) {}
    public class DamageCause {
        public static org.bukkit.event.entity.EntityDamageEvent.DamageCause FALL;
        public static org.bukkit.event.entity.EntityDamageEvent.DamageCause FIRE;
        public static org.bukkit.event.entity.EntityDamageEvent.DamageCause FIRE_TICK;
        public static org.bukkit.event.entity.EntityDamageEvent.DamageCause LAVA;
        public static org.bukkit.event.entity.EntityDamageEvent.DamageCause VOID;
    }

}
