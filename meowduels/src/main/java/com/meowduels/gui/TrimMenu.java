/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.Menu;
import com.meowduels.managers.GuiConfigManager;
import com.meowduels.managers.GuiConfigManager.Face;
import com.meowduels.model.Kit;
import com.meowduels.util.Colors;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import com.meowduels.util.Trims;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TrimMenu
extends Menu {
    private static final int HELMET_SLOT = 20;
    private static final int CHEST_SLOT = 21;
    private static final int LEGS_SLOT = 22;
    private static final int BOOTS_SLOT = 23;
    private static final int PATTERN_SLOT = 38;
    private static final int MATERIAL_SLOT = 42;
    private static final int APPLY_ALL_SLOT = 48;
    private static final int CLEAR_SLOT = 50;
    private final MeowDuels plugin;
    private final String kitName;
    private final UUID owner;
    private int patternIndex = 0;
    private int materialIndex = 0;

    public TrimMenu(MeowDuels plugin, String kitName, UUID owner) {
        this.plugin = plugin;
        this.kitName = kitName;
        this.owner = owner;
    }

    /** The name of this screen's file: {@code gui/trim.yml}. */
    private static final String MENU = "trim";

    /** The two lines both cycling buttons share. */
    private static final String[] CYCLE_LORE = new String[]{
            "&7ʟᴇꜰᴛ-ᴄʟɪᴄᴋ &8- &fɴᴇxᴛ",
            "&7ʀɪɢʜᴛ-ᴄʟɪᴄᴋ &8- &fᴘʀᴇᴠɪᴏᴜꜱ",
            "",
            "&8{index}/{total}"};

    @Override
    public void build() {
        GuiConfigManager gui = this.plugin.gui();
        Map<String, String> kitToken = new HashMap<String, String>();
        kitToken.put("kit", this.kitName);
        // Six rows and these slots are fixed: a click here is matched against
        // the numbers below, so a moved item would run the wrong action.
        this.createRaw(6, gui.title(MENU,
                "&d&lᴛʀɪᴍꜱ &7- &f{kit}", kitToken));
        ItemStack filler = gui.pane(MENU);
        for (int i = 0; i < 54; ++i) {
            this.inventory.setItem(i, filler);
        }
        Kit kit = this.plugin.getKitManager().get(this.kitName);
        if (kit == null) {
            return;
        }
        ItemStack[] armor = kit.getArmor();
        this.inventory.setItem(20, this.armorDisplay(gui, armor, 3,
                gui.line(MENU, "armor", "piece-helmet", "ʜᴇʟᴍᴇᴛ", null)));
        this.inventory.setItem(21, this.armorDisplay(gui, armor, 2,
                gui.line(MENU, "armor", "piece-chestplate", "ᴄʜᴇꜱᴛᴘʟᴀᴛᴇ", null)));
        this.inventory.setItem(22, this.armorDisplay(gui, armor, 1,
                gui.line(MENU, "armor", "piece-leggings", "ʟᴇɢɢɪɴɢꜱ", null)));
        this.inventory.setItem(23, this.armorDisplay(gui, armor, 0,
                gui.line(MENU, "armor", "piece-boots", "ʙᴏᴏᴛꜱ", null)));
        Map<String, String> pattern = new HashMap<String, String>();
        pattern.put("pattern", Trims.pretty(Trims.PATTERN_KEYS[this.patternIndex]));
        pattern.put("index", String.valueOf(this.patternIndex + 1));
        pattern.put("total", String.valueOf(Trims.PATTERN_KEYS.length));
        this.inventory.setItem(38,
                gui.face(MENU, "pattern", pattern,
                         Face.of(Material.PAPER,
                                 "&d&lᴘᴀᴛᴛᴇʀɴ&7: &f{pattern}",
                                 CYCLE_LORE))
                        .tag(this.plugin.keyButton(), "trim-pattern").build());
        Map<String, String> material = new HashMap<String, String>();
        material.put("material", Trims.pretty(Trims.MATERIAL_KEYS[this.materialIndex]));
        material.put("index", String.valueOf(this.materialIndex + 1));
        material.put("total", String.valueOf(Trims.MATERIAL_KEYS.length));
        this.inventory.setItem(42,
                gui.face(MENU, "material", material,
                         Face.of(gui.material(MENU, "material", this.materialIcon()),
                                 "&d&lᴍᴀᴛᴇʀɪᴀʟ&7: &f{material}",
                                 CYCLE_LORE))
                        .tag(this.plugin.keyButton(), "trim-material").build());
        this.inventory.setItem(48,
                gui.face(MENU, "apply-all", null,
                         Face.of(Material.LIME_DYE,
                                 "&a&lᴀᴘᴘʟʏ ᴛᴏ ᴀʟʟ ᴘɪᴇᴄᴇꜱ",
                                 "&7ꜱᴇᴛ ᴛʜᴇ ꜱᴇʟᴇᴄᴛᴇᴅ ᴛʀɪᴍ ᴏɴ ᴇᴠᴇʀʏ ᴀʀᴍᴏʀ ᴘɪᴇᴄᴇ."))
                        .tag(this.plugin.keyButton(), "trim-apply-all").build());
        this.inventory.setItem(50,
                gui.face(MENU, "clear", null,
                         Face.of(Material.BARRIER,
                                 "&c&lᴄʟᴇᴀʀ ᴀʟʟ ᴛʀɪᴍꜱ",
                                 "&7ʀᴇᴍᴏᴠᴇ ᴛʀɪᴍꜱ ꜰʀᴏᴍ ᴇᴠᴇʀʏ ᴀʀᴍᴏʀ ᴘɪᴇᴄᴇ."))
                        .tag(this.plugin.keyButton(), "trim-clear").build());
    }

    /**
     * One armour slot: the kit's real piece, wearing the player's trim.
     *
     * <p>The item itself is the kit's, so there is no material or name to
     * configure - only the two lore lines added underneath, and the
     * placeholder shown where the kit has no such piece.
     */
    private ItemStack armorDisplay(GuiConfigManager gui, ItemStack[] armor, int idx,
                                   String pieceLabel) {
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("piece", pieceLabel);
        ItemStack piece = idx >= 0 && idx < armor.length ? armor[idx] : null;
        if (piece == null || piece.getType() == Material.AIR) {
            return gui.face(MENU, "armor-missing", tokens,
                            Face.of(Material.GRAY_STAINED_GLASS_PANE,
                                    "&7ɴᴏ {piece}",
                                    "&8ᴛʜɪꜱ ᴋɪᴛ ʜᴀꜱ ɴᴏ {piece}.")).build();
        }
        ItemStack show = piece.clone();
        String[] saved = this.plugin.getTrimPreferences().get(this.owner, this.kitName, idx);
        if (saved != null) {
            Trims.apply(show, saved[0], saved[1]);
        }
        try {
            ItemMeta meta = show.getItemMeta();
            if (meta != null) {
                ArrayList<String> lore = new ArrayList<String>();
                if (saved != null) {
                    Map<String, String> trim = new HashMap<String, String>();
                    trim.put("trim", Trims.pretty(saved[1]) + " " + Trims.pretty(saved[0]));
                    lore.add(Colors.toSection(gui.line(MENU, "armor", "your-trim",
                            "&7ʏᴏᴜʀ ᴛʀɪᴍ&8: &f{trim}", trim)));
                } else {
                    lore.add(Colors.toSection(gui.line(MENU, "armor", "no-trim",
                            "&8ɴᴏ ᴛʀɪᴍ ꜱᴇᴛ.", null)));
                }
                lore.add(Colors.toSection(gui.line(MENU, "armor", "apply-hint",
                        "&eᴄʟɪᴄᴋ ᴛᴏ ᴀᴘᴘʟʏ ᴛʜᴇ ꜱᴇʟᴇᴄᴛᴇᴅ ᴛʀɪᴍ.", null)));
                meta.setLore(lore);
                show.setItemMeta(meta);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return show;
    }

    private Material materialIcon() {
        switch (Trims.MATERIAL_KEYS[this.materialIndex]) {
            case "quartz": {
                return Material.QUARTZ;
            }
            case "iron": {
                return Material.IRON_INGOT;
            }
            case "gold": {
                return Material.GOLD_INGOT;
            }
            case "copper": {
                return Material.COPPER_INGOT;
            }
            case "netherite": {
                return Material.NETHERITE_INGOT;
            }
            case "redstone": {
                return Material.REDSTONE;
            }
            case "emerald": {
                return Material.EMERALD;
            }
            case "diamond": {
                return Material.DIAMOND;
            }
            case "lapis": {
                return Material.LAPIS_LAZULI;
            }
            case "amethyst": {
                return Material.AMETHYST_SHARD;
            }
        }
        return Material.PAPER;
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        String btn = Items.readTag(clicked, this.plugin.keyButton());
        if ("trim-pattern".equals(btn)) {
            int dir = event.isRightClick() ? -1 : 1;
            this.patternIndex = (this.patternIndex + dir + Trims.PATTERN_KEYS.length) % Trims.PATTERN_KEYS.length;
            this.reopen(player);
            return;
        }
        if ("trim-material".equals(btn)) {
            int dir = event.isRightClick() ? -1 : 1;
            this.materialIndex = (this.materialIndex + dir + Trims.MATERIAL_KEYS.length) % Trims.MATERIAL_KEYS.length;
            this.reopen(player);
            return;
        }
        if ("trim-apply-all".equals(btn)) {
            this.applyAll(player);
            return;
        }
        if ("trim-clear".equals(btn)) {
            this.clearAll(player);
            return;
        }
        int armorIdx = this.slotToArmor(event.getRawSlot());
        if (armorIdx >= 0) {
            this.applyOne(player, armorIdx);
        }
    }

    private int slotToArmor(int raw) {
        switch (raw) {
            case 20: {
                return 3;
            }
            case 21: {
                return 2;
            }
            case 22: {
                return 1;
            }
            case 23: {
                return 0;
            }
        }
        return -1;
    }

    private void applyOne(Player player, int armorIdx) {
        Kit kit = this.plugin.getKitManager().get(this.kitName);
        if (kit == null) {
            return;
        }
        ItemStack[] armor = kit.getArmor();
        if (armorIdx >= armor.length || armor[armorIdx] == null || armor[armorIdx].getType() == Material.AIR) {
            player.sendMessage(this.plugin.messages().get("notice.trim.kit-no-armor-slot"));
            return;
        }
        if (!Trims.available()) {
            player.sendMessage(this.plugin.messages().get("notice.trim.could-not-apply-trim"));
            return;
        }
        this.plugin.getTrimPreferences().set(this.owner, this.kitName, armorIdx, Trims.PATTERN_KEYS[this.patternIndex], Trims.MATERIAL_KEYS[this.materialIndex]);
        player.sendMessage(this.plugin.messages().get("notice.trim.applied-trim-only-will", "value", String.valueOf(Trims.pretty(Trims.MATERIAL_KEYS[this.materialIndex])), "value2", String.valueOf(Trims.pretty(Trims.PATTERN_KEYS[this.patternIndex]))));
        this.reopen(player);
    }

    private void applyAll(Player player) {
        Kit kit = this.plugin.getKitManager().get(this.kitName);
        if (kit == null) {
            return;
        }
        if (!Trims.available()) {
            player.sendMessage(this.plugin.messages().get("notice.trim.no-armor-trim-trims"));
            return;
        }
        this.plugin.getTrimPreferences().setAll(this.owner, this.kitName, Trims.PATTERN_KEYS[this.patternIndex], Trims.MATERIAL_KEYS[this.materialIndex]);
        player.sendMessage(this.plugin.messages().get("notice.trim.applied-trim-all-armor"));
        this.reopen(player);
    }

    private void clearAll(Player player) {
        this.plugin.getTrimPreferences().clear(this.owner, this.kitName);
        player.sendMessage(this.plugin.messages().get("notice.trim.cleared-trims", "value", String.valueOf(this.kitName)));
        this.reopen(player);
    }

    private void reopen(Player player) {
        this.build();
        player.openInventory(this.inventory);
    }
}

