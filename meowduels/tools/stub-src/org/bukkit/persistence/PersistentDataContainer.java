package org.bukkit.persistence;

public interface PersistentDataContainer {
    java.lang.Object get(org.bukkit.NamespacedKey a0, org.bukkit.persistence.PersistentDataType a1);
    void set(org.bukkit.NamespacedKey a0, org.bukkit.persistence.PersistentDataType a1, java.lang.Object a2);
}
