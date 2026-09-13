package org.bukkit.event;

public @interface EventHandler {
    org.bukkit.event.EventPriority priority() default org.bukkit.event.EventPriority.NORMAL;
    boolean ignoreCancelled() default false;
}
