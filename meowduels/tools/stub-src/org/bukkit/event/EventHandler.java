package org.bukkit.event;

@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface EventHandler {
    org.bukkit.event.EventPriority priority() default org.bukkit.event.EventPriority.NORMAL;
    boolean ignoreCancelled() default false;
}
