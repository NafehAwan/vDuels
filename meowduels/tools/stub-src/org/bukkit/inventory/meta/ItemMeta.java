package org.bukkit.inventory.meta;

public interface ItemMeta {
    void addItemFlags(org.bukkit.inventory.ItemFlag[] a0);
    void displayName(net.kyori.adventure.text.Component a0);
    java.lang.String getDisplayName();
    org.bukkit.persistence.PersistentDataContainer getPersistentDataContainer();
    boolean hasDisplayName();
    void setDisplayName(java.lang.String a0);
    void setEnchantmentGlintOverride(java.lang.Boolean a0);
    void setLore(java.util.List a0);
}
