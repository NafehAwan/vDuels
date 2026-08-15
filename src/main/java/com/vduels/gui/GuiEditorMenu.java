package com.vduels.gui;

import com.vduels.VDuels;
import com.vduels.managers.GuiLayoutManager;
import com.vduels.model.Arena;
import com.vduels.model.Kit;
import com.vduels.util.Items;
import com.vduels.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One editor for every customisable menu. The editable area (top rows) is
 * pre-filled with the menu's current markers/decoration; the admin rearranges
 * them freely and adds decoration. The bottom row holds Save / Cancel / a
 * decoration palette / Reset. Saving stores the arrangement as the menu's
 * layout (see {@link GuiLayoutManager}).
 */
public class GuiEditorMenu extends Menu {

    private static final int CANCEL_SLOT = 45;
    private static final int PALETTE_SLOT = 47;
    private static final int SAVE_SLOT = 49;
    private static final int RESET_SLOT = 51;

    private final VDuels plugin;
    private final String menuId;
    private final int editableSize;
    private boolean done = false;

    public GuiEditorMenu(VDuels plugin, String menuId) {
        this.plugin = plugin;
        this.menuId = menuId;
        this.editableSize = GuiLayoutManager.editableSize(menuId);
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public boolean isProtectedSlot(int rawSlot) {
        // Locked: the control row and any slot beyond the menu's editable area.
        return rawSlot >= editableSize;
    }

    @Override
    public void build() {
        create(6, "&8Editing: &b" + menuId);

        if (plugin.getGuiLayoutManager().has(menuId)) {
            for (Map.Entry<Integer, ItemStack> entry : plugin.getGuiLayoutManager().get(menuId).entrySet()) {
                if (entry.getKey() < editableSize) {
                    inventory.setItem(entry.getKey(), entry.getValue());
                }
            }
        } else {
            fillDefaults();
        }

        // Lock the slots between the menu area and the control row.
        ItemStack locked = Items.of(Material.GRAY_STAINED_GLASS_PANE).name("&8(outside the menu)").build();
        for (int i = editableSize; i < 45; i++) {
            inventory.setItem(i, locked);
        }

        // Control row.
        ItemStack bar = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, bar);
        }
        inventory.setItem(CANCEL_SLOT, Items.of(Material.BARRIER)
                .name("&cCancel").lore("&7Close without saving.").build());
        inventory.setItem(PALETTE_SLOT, Items.of(Material.GRAY_STAINED_GLASS_PANE)
                .name("&8Decoration").lore("&7Click for decoration panes to place.").build());
        inventory.setItem(SAVE_SLOT, Items.of(Material.LIME_DYE)
                .name("&a&lSave Layout").lore("&7Save this arrangement.").build());
        inventory.setItem(RESET_SLOT, Items.of(Material.TNT)
                .name("&cReset to Default").lore("&7Restore the default layout.").build());
    }

    /** Places the menu's default markers when there is no saved layout. */
    private void fillDefaults() {
        switch (menuId) {
            case GuiLayoutManager.DUEL_CONFIRM -> {
                ItemStack gray = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
                for (int i = 0; i < 27; i++) {
                    inventory.setItem(i, gray);
                }
                inventory.setItem(10, buttonMarker("map", Material.FILLED_MAP, "&bArena button"));
                inventory.setItem(12, buttonMarker("kit", Material.GOLDEN_APPLE, "&6Kit button"));
                inventory.setItem(14, buttonMarker("clock", Material.CLOCK, "&eRounds button"));
                inventory.setItem(16, buttonMarker("confirm", Material.GREEN_STAINED_GLASS_PANE, "&aConfirm button"));
            }
            case GuiLayoutManager.MAP_SELECT -> {
                int slot = 0;
                for (Arena arena : plugin.getArenaManager().all()) {
                    if (slot >= editableSize - 1) {
                        break;
                    }
                    if (arena.isConfigured()) {
                        inventory.setItem(slot++, Items.of(Material.FILLED_MAP)
                                .name("&e" + arena.getName())
                                .lore("&fArena icon - drag to arrange.")
                                .tag(plugin.keyArena(), arena.getName())
                                .build());
                    }
                }
                inventory.setItem(editableSize - 1, buttonMarker("random", Material.ENDER_PEARL, "&eRandom button"));
            }
            default -> {
            }
        }
    }

    private ItemStack buttonMarker(String id, Material icon, String name) {
        return Items.of(icon).name(name)
                .lore("&7Menu button - drag to arrange.")
                .tag(plugin.keyButton(), id)
                .build();
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        switch (event.getRawSlot()) {
            case SAVE_SLOT -> {
                persist();
                done = true;
                player.sendMessage(Text.prefixed("&aSaved the &b" + menuId + "&a menu layout."));
                player.closeInventory();
            }
            case CANCEL_SLOT -> {
                done = true;
                player.sendMessage(Text.prefixed("&7Editor closed without saving."));
                player.closeInventory();
            }
            case PALETTE_SLOT -> {
                player.getInventory().addItem(Items.of(Material.GRAY_STAINED_GLASS_PANE, 16)
                        .name("&8Decoration").build());
                player.sendMessage(Text.prefixed("&7Added decoration panes to your inventory."));
            }
            case RESET_SLOT -> {
                plugin.getGuiLayoutManager().clear(menuId);
                fillDefaultsFresh();
                player.openInventory(inventory);
                player.sendMessage(Text.prefixed("&7Reset &b" + menuId + "&7 to default."));
            }
            default -> {
            }
        }
    }

    private void fillDefaultsFresh() {
        for (int i = 0; i < editableSize; i++) {
            inventory.setItem(i, null);
        }
        fillDefaults();
    }

    @Override
    public void onClose(Player player, InventoryCloseEvent event) {
        if (done) {
            return;
        }
        persist();
        player.sendMessage(Text.prefixed("&aSaved the &b" + menuId + "&a menu layout."));
    }

    private void persist() {
        Map<Integer, ItemStack> layout = new LinkedHashMap<>();
        for (int i = 0; i < editableSize; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                layout.put(i, item.clone());
            }
        }
        // Keep each menu functional: re-add any missing button at a free slot.
        switch (menuId) {
            case GuiLayoutManager.DUEL_CONFIRM -> {
                ensureButton(layout, "map", Material.FILLED_MAP, "&bArena button", 10);
                ensureButton(layout, "kit", Material.GOLDEN_APPLE, "&6Kit button", 12);
                ensureButton(layout, "clock", Material.CLOCK, "&eRounds button", 14);
                ensureButton(layout, "confirm", Material.GREEN_STAINED_GLASS_PANE, "&aConfirm button", 16);
            }
            case GuiLayoutManager.MAP_SELECT ->
                    ensureButton(layout, "random", Material.ENDER_PEARL, "&eRandom button", editableSize - 1);
            default -> {
            }
        }
        plugin.getGuiLayoutManager().set(menuId, layout);
    }

    private void ensureButton(Map<Integer, ItemStack> layout, String id, Material icon, String name, int defaultSlot) {
        for (ItemStack item : layout.values()) {
            if (id.equals(Items.readTag(item, plugin.keyButton()))) {
                return; // already present
            }
        }
        int slot = layout.containsKey(defaultSlot) ? firstFree(layout) : defaultSlot;
        if (slot >= 0) {
            layout.put(slot, buttonMarker(id, icon, name));
        }
    }

    private int firstFree(Map<Integer, ItemStack> layout) {
        for (int i = 0; i < editableSize; i++) {
            if (!layout.containsKey(i)) {
                return i;
            }
        }
        return -1;
    }
}
