package com.meowduels.managers;

import com.meowduels.MeowDuels;
import com.meowduels.model.Kit;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Per-player kit layouts, stored in kitlayouts.yml.
 *
 * <p>Editing a kit used to write straight back to the shared kit, so one player
 * rearranging their hotbar changed that kit for everyone on the server. A layout
 * saved here belongs to one player and one kit; the kit itself stays the server
 * default and is what anyone without a personal layout receives.
 *
 * <p>Only the item arrangement is personal. What the kit CONTAINS is still the
 * server's decision - a layout is applied as "these items in this order", so it
 * can't be used to smuggle in items the kit doesn't grant.
 */
public class KitLayoutManager {

    private final MeowDuels plugin;
    private final File file;
    private YamlConfiguration data;

    public KitLayoutManager(MeowDuels plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kitlayouts.yml");
        this.load();
    }

    private void load() {
        this.data = this.file.exists()
                ? YamlConfiguration.loadConfiguration(this.file)
                : new YamlConfiguration();
    }

    public void save() {
        try {
            this.data.save(this.file);
        } catch (IOException ex) {
            this.plugin.getLogger().warning("Failed to save kitlayouts.yml: " + ex.getMessage());
        }
    }

    private static String path(UUID id, String kit) {
        return "layouts." + id + "." + kit.toLowerCase(java.util.Locale.ROOT);
    }

    /** True if this player has saved their own layout for this kit. */
    public boolean has(UUID id, String kit) {
        return id != null && kit != null && this.data.getConfigurationSection(path(id, kit)) != null;
    }

    /** Stores one player's arrangement of a kit. */
    public void store(UUID id, String kit, ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
        String base = path(id, kit);
        this.data.set(base + ".contents", java.util.Arrays.asList(contents == null ? new ItemStack[0] : contents));
        this.data.set(base + ".armor", java.util.Arrays.asList(armor == null ? new ItemStack[0] : armor));
        this.data.set(base + ".offhand", offhand);
        this.save();
    }

    /** Drops a player's layout, so they go back to the server default. */
    public void clear(UUID id, String kit) {
        this.data.set(path(id, kit), null);
        this.save();
    }

    private ItemStack[] read(UUID id, String kit, String key, int size) {
        ConfigurationSection section = this.data.getConfigurationSection(path(id, kit));
        if (section == null) {
            return null;
        }
        List<?> raw = section.getList(key);
        if (raw == null) {
            return null;
        }
        ItemStack[] out = new ItemStack[size];
        for (int i = 0; i < size && i < raw.size(); i++) {
            Object entry = raw.get(i);
            out[i] = entry instanceof ItemStack ? (ItemStack) entry : null;
        }
        return out;
    }

    public ItemStack[] contents(UUID id, String kit) {
        return this.read(id, kit, "contents", 36);
    }

    public ItemStack[] armor(UUID id, String kit) {
        return this.read(id, kit, "armor", 4);
    }

    public ItemStack offhand(UUID id, String kit) {
        ConfigurationSection section = this.data.getConfigurationSection(path(id, kit));
        if (section == null) {
            return null;
        }
        return section.getItemStack("offhand");
    }

    /** True if a submitted layout holds exactly the kit's items - same materials,
     *  same totals - just arranged differently.
     *
     *  <p>The editor lets a player drag from their own inventory, so without this
     *  a personal "layout" would be a way to walk into a ranked duel carrying
     *  whatever they liked. Rearranging is personal; the contents are not.
     */
    public boolean matchesKit(Kit kit, ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
        java.util.Map<org.bukkit.Material, Integer> expected = new java.util.HashMap<org.bukkit.Material, Integer>();
        tally(expected, kit.getContents());
        tally(expected, kit.getArmor());
        tally(expected, new ItemStack[] {kit.getOffhand()});
        java.util.Map<org.bukkit.Material, Integer> actual = new java.util.HashMap<org.bukkit.Material, Integer>();
        tally(actual, contents);
        tally(actual, armor);
        tally(actual, new ItemStack[] {offhand});
        return expected.equals(actual);
    }

    private static void tally(java.util.Map<org.bukkit.Material, Integer> into, ItemStack[] items) {
        if (items == null) {
            return;
        }
        for (ItemStack item : items) {
            if (item == null || item.getType() == org.bukkit.Material.AIR) {
                continue;
            }
            Integer had = into.get(item.getType());
            into.put(item.getType(), (had == null ? 0 : had) + Math.max(1, item.getAmount()));
        }
    }

    /** A stored layout is only honoured while it still holds exactly the kit's
     *  items. An admin editing the kit therefore retires every personal layout
     *  for it automatically, instead of handing out the old item set forever. */
    private boolean usable(UUID id, Kit kit) {
        if (!this.has(id, kit.getName())) {
            return false;
        }
        if (this.matchesKit(kit, this.contents(id, kit.getName()),
                this.armor(id, kit.getName()), this.offhand(id, kit.getName()))) {
            return true;
        }
        this.clear(id, kit.getName()); // kit changed under it - drop the stale one
        return false;
    }

    /** Gives the player this kit, using their own layout when they have one. */
    public void applyTo(Player player, Kit kit) {
        if (player == null || kit == null) {
            return;
        }
        UUID id = player.getUniqueId();
        if (!this.usable(id, kit)) {
            kit.applyTo(player);
            return;
        }
        PlayerInventory inv = player.getInventory();
        inv.clear();
        ItemStack[] contents = this.contents(id, kit.getName());
        if (contents != null) {
            inv.setStorageContents(contents);
        }
        ItemStack[] armor = this.armor(id, kit.getName());
        if (armor != null) {
            inv.setArmorContents(armor);
        }
        inv.setItemInOffHand(this.offhand(id, kit.getName()));
        player.updateInventory();
    }

    /** Re-applies the offhand a tick later (some clients drop it otherwise). */
    public void applyOffhand(Player player, Kit kit) {
        if (player == null || kit == null) {
            return;
        }
        if (!this.usable(player.getUniqueId(), kit)) {
            kit.applyOffhand(player);
            return;
        }
        player.getInventory().setItemInOffHand(this.offhand(player.getUniqueId(), kit.getName()));
        player.updateInventory();
    }
}
