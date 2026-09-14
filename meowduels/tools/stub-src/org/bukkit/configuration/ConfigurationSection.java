package org.bukkit.configuration;

public interface ConfigurationSection {
    boolean getBoolean(java.lang.String a0);
    boolean getBoolean(java.lang.String a0, boolean a1);
    org.bukkit.configuration.ConfigurationSection getConfigurationSection(java.lang.String a0);
    double getDouble(java.lang.String a0);
    int getInt(java.lang.String a0, int a1);
    java.lang.String getString(java.lang.String a0);
    java.lang.String getString(java.lang.String a0, java.lang.String a1);
    boolean isConfigurationSection(java.lang.String a0);
    void set(java.lang.String a0, java.lang.Object a1);
    java.util.Set<String> getKeys(boolean a0);
    java.util.List<String> getStringList(String a0);
    java.util.List<?> getList(String a0);
    org.bukkit.inventory.ItemStack getItemStack(String a0);
}
