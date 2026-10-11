/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.ItemStack
 */
package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.KitSelectMenu;
import com.meowduels.gui.Menu;
import com.meowduels.managers.GuiConfigManager;
import com.meowduels.managers.GuiConfigManager.Face;
import com.meowduels.model.Arena;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ArenaMenu
extends Menu {
    private final MeowDuels plugin;
    private final Arena arena;

    public ArenaMenu(MeowDuels plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    /** The name of this screen's file: {@code gui/arena.yml}. */
    private static final String MENU = "arena";

    /** The two panes every toggle here uses, and the lines under them. */
    private static final Material ON_MATERIAL = Material.PINK_STAINED_GLASS_PANE;
    private static final Material OFF_MATERIAL = Material.GRAY_STAINED_GLASS_PANE;
    private static final String TOGGLE_ON = "&d\u1d07\u0274\u1d00\u0299\u029f\u1d07\u1d05 &8(\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d05\u026a\ua731\u1d00\u0299\u029f\u1d07)";
    private static final String TOGGLE_OFF = "&7\u1d05\u026a\ua731\u1d00\u0299\u029f\u1d07\u1d05 &8(\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d07\u0274\u1d00\u0299\u029f\u1d07)";
    private static final String NUMBER_HINT = "&7\u029f\u1d07\ua730\u1d1b-\u1d04\u029f\u026a\u1d04\u1d0b: &a+1 &8| &7\u0280\u026a\u0262\u029c\u1d1b-\u1d04\u029f\u026a\u1d04\u1d0b: &c-1";

    @Override
    public void build() {
        GuiConfigManager gui = this.plugin.gui();
        Map<String, String> arenaToken = new HashMap<String, String>();
        arenaToken.put("arena", this.arena.getName());
        // Six rows and every slot below are fixed: this screen decides what a
        // click did from the slot number, so a moved item would toggle
        // something else. Materials, names and lore are all editable.
        this.createRaw(6, gui.title(MENU, "&d&l\u1d00\u0280\u1d07\u0274\u1d00: &5{arena}", arenaToken));
        ItemStack filler = gui.pane(MENU);
        for (int i = 0; i < 54; ++i) {
            this.inventory.setItem(i, filler);
        }
        if (!this.arena.isConfigured()) {
            this.inventory.setItem(31, gui.face(MENU, "setup-first", null,
                    Face.of(Material.MAGENTA_STAINED_GLASS_PANE, "&d&l\ua731\u1d07\u1d1b\u1d1c\u1d18",
                            "&7\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \ua731\u1d1b\u1d00\u0280\u1d1b \u1d00\u0280\u1d07\u0274\u1d00 \ua731\u1d07\u1d1b\u1d1c\u1d18.",
                            "&7\u028f\u1d0f\u1d1c'\u029f\u029f \u0299\u1d07 \u1d00\ua731\u1d0b\u1d07\u1d05 \u1d1b\u1d0f \u1d21\u1d00\u029f\u1d0b \u1d1b\u1d0f \u1d07\u1d00\u1d04\u029c",
                            "&7\ua731\u1d18\u1d00\u1d21\u0274 \u1d00\u0274\u1d05 \u1d04\u1d0f\u0280\u0274\u1d07\u0280 \u1d00\u0274\u1d05 \u1d1b\u028f\u1d18\u1d07 &e\u1d05\u1d0f\u0274\u1d07&7.")).build());
            return;
        }
        Map<String, String> tokens = this.tokens();
        // Every toggle on this screen wears the same two faces.
        this.inventory.setItem(10, gui.toggle(MENU, "auto-regenerate", this.arena.isAutoRegenerate(), null,
                Face.of(ON_MATERIAL, "&d\u1d00\u1d1c\u1d1b\u1d0f-\u0280\u1d07\u0262\u1d07\u0274\u1d07\u0280\u1d00\u1d1b\u1d07", "&7\u0280\u1d07\ua731\u1d1b\u1d0f\u0280\u1d07 \u1d1b\u029c\u1d07 \u1d00\u0280\u1d07\u0274\u1d00 \u1d00\ua730\u1d1b\u1d07\u0280 \u1d07\u1d00\u1d04\u029c \ua730\u026a\u0262\u029c\u1d1b.", "", TOGGLE_ON),
                Face.of(OFF_MATERIAL, "&d\u1d00\u1d1c\u1d1b\u1d0f-\u0280\u1d07\u0262\u1d07\u0274\u1d07\u0280\u1d00\u1d1b\u1d07", "&7\u0280\u1d07\ua731\u1d1b\u1d0f\u0280\u1d07 \u1d1b\u029c\u1d07 \u1d00\u0280\u1d07\u0274\u1d00 \u1d00\ua730\u1d1b\u1d07\u0280 \u1d07\u1d00\u1d04\u029c \ua730\u026a\u0262\u029c\u1d1b.", "", TOGGLE_OFF)).build());
        this.inventory.setItem(13, gui.toggle(MENU, "enabled", this.arena.isEnabled(), null,
                Face.of(ON_MATERIAL, "&d\u1d07\u0274\u1d00\u0299\u029f\u1d07\u1d05 (\u026a\u0274 \u0280\u1d0f\u1d1b\u1d00\u1d1b\u026a\u1d0f\u0274)", "&7\u1d1b\u1d1c\u0280\u0274 \u1d0f\ua730\ua730 \u1d1b\u1d0f \u029c\u026a\u1d05\u1d07 \ua730\u0280\u1d0f\u1d0d \u1d0d\u1d00\u1d1b\u1d04\u029c\u1d0d\u1d00\u1d0b\u026a\u0274\u0262", "&7\u1d21\u026a\u1d1b\u029c\u1d0f\u1d1c\u1d1b \u1d05\u1d07\u029f\u1d07\u1d1b\u026a\u0274\u0262 \u1d1b\u029c\u1d07 \u1d00\u0280\u1d07\u0274\u1d00.", "", TOGGLE_ON),
                Face.of(OFF_MATERIAL, "&d\u1d07\u0274\u1d00\u0299\u029f\u1d07\u1d05 (\u026a\u0274 \u0280\u1d0f\u1d1b\u1d00\u1d1b\u026a\u1d0f\u0274)", "&7\u1d1b\u1d1c\u0280\u0274 \u1d0f\ua730\ua730 \u1d1b\u1d0f \u029c\u026a\u1d05\u1d07 \ua730\u0280\u1d0f\u1d0d \u1d0d\u1d00\u1d1b\u1d04\u029c\u1d0d\u1d00\u1d0b\u026a\u0274\u0262", "&7\u1d21\u026a\u1d1b\u029c\u1d0f\u1d1c\u1d1b \u1d05\u1d07\u029f\u1d07\u1d1b\u026a\u0274\u0262 \u1d1b\u029c\u1d07 \u1d00\u0280\u1d07\u0274\u1d00.", "", TOGGLE_OFF)).build());
        this.inventory.setItem(18, gui.toggle(MENU, "allow-building", this.arena.isAllowBuild(), null,
                Face.of(ON_MATERIAL, "&d\u1d00\u029f\u029f\u1d0f\u1d21 \u0299\u1d1c\u026a\u029f\u1d05\u026a\u0274\u0262", "&7\u1d18\u029f\u1d00\u028f\u1d07\u0280\ua731 \u1d04\u1d00\u0274 \u1d18\u029f\u1d00\u1d04\u1d07 \u0299\u029f\u1d0f\u1d04\u1d0b\ua731", "&7\u1d00\u0274\u028f\u1d21\u029c\u1d07\u0280\u1d07 \u026a\u0274\ua731\u026a\u1d05\u1d07 \u1d1b\u029c\u1d07 \u1d00\u0280\u1d07\u0274\u1d00.", "", TOGGLE_ON),
                Face.of(OFF_MATERIAL, "&d\u1d00\u029f\u029f\u1d0f\u1d21 \u0299\u1d1c\u026a\u029f\u1d05\u026a\u0274\u0262", "&7\u1d18\u029f\u1d00\u028f\u1d07\u0280\ua731 \u1d04\u1d00\u0274 \u1d18\u029f\u1d00\u1d04\u1d07 \u0299\u029f\u1d0f\u1d04\u1d0b\ua731", "&7\u1d00\u0274\u028f\u1d21\u029c\u1d07\u0280\u1d07 \u026a\u0274\ua731\u026a\u1d05\u1d07 \u1d1b\u029c\u1d07 \u1d00\u0280\u1d07\u0274\u1d00.", "", TOGGLE_OFF)).build());
        this.inventory.setItem(19, gui.toggle(MENU, "allow-breaking", this.arena.isAllowBreak(), null,
                Face.of(ON_MATERIAL, "&d\u1d00\u029f\u029f\u1d0f\u1d21 \u0299\u0280\u1d07\u1d00\u1d0b\u026a\u0274\u0262", "&7\u1d18\u029f\u1d00\u028f\u1d07\u0280\ua731 \u1d04\u1d00\u0274 \u0299\u0280\u1d07\u1d00\u1d0b \u0299\u029f\u1d0f\u1d04\u1d0b\ua731", "&7\u1d00\u0274\u028f\u1d21\u029c\u1d07\u0280\u1d07 \u026a\u0274\ua731\u026a\u1d05\u1d07 \u1d1b\u029c\u1d07 \u1d00\u0280\u1d07\u0274\u1d00.", "", TOGGLE_ON),
                Face.of(OFF_MATERIAL, "&d\u1d00\u029f\u029f\u1d0f\u1d21 \u0299\u0280\u1d07\u1d00\u1d0b\u026a\u0274\u0262", "&7\u1d18\u029f\u1d00\u028f\u1d07\u0280\ua731 \u1d04\u1d00\u0274 \u0299\u0280\u1d07\u1d00\u1d0b \u0299\u029f\u1d0f\u1d04\u1d0b\ua731", "&7\u1d00\u0274\u028f\u1d21\u029c\u1d07\u0280\u1d07 \u026a\u0274\ua731\u026a\u1d05\u1d07 \u1d1b\u029c\u1d07 \u1d00\u0280\u1d07\u0274\u1d00.", "", TOGGLE_OFF)).build());
        this.inventory.setItem(20, gui.toggle(MENU, "break-self-placed", this.arena.isAllowRemoveAdded(), null,
                Face.of(ON_MATERIAL, "&d\u0299\u0280\u1d07\u1d00\u1d0b \ua731\u1d07\u029f\ua730-\u1d18\u029f\u1d00\u1d04\u1d07\u1d05 \u1d0f\u0274\u029f\u028f", "&7\u1d07\u1d20\u1d07\u0274 \u1d21\u026a\u1d1b\u029c \u0299\u0280\u1d07\u1d00\u1d0b\u026a\u0274\u0262 \u1d0f\ua730\ua730, \u1d18\u029f\u1d00\u028f\u1d07\u0280\ua731", "&7\u1d04\u1d00\u0274 \ua731\u1d1b\u026a\u029f\u029f \u0299\u0280\u1d07\u1d00\u1d0b \u0299\u029f\u1d0f\u1d04\u1d0b\ua731 \u1d1b\u029c\u1d07\u028f", "&7\u1d18\u029f\u1d00\u1d04\u1d07\u1d05 \u1d1b\u029c\u026a\ua731 \u1d0d\u1d00\u1d1b\u1d04\u029c.", "", TOGGLE_ON),
                Face.of(OFF_MATERIAL, "&d\u0299\u0280\u1d07\u1d00\u1d0b \ua731\u1d07\u029f\ua730-\u1d18\u029f\u1d00\u1d04\u1d07\u1d05 \u1d0f\u0274\u029f\u028f", "&7\u1d07\u1d20\u1d07\u0274 \u1d21\u026a\u1d1b\u029c \u0299\u0280\u1d07\u1d00\u1d0b\u026a\u0274\u0262 \u1d0f\ua730\ua730, \u1d18\u029f\u1d00\u028f\u1d07\u0280\ua731", "&7\u1d04\u1d00\u0274 \ua731\u1d1b\u026a\u029f\u029f \u0299\u0280\u1d07\u1d00\u1d0b \u0299\u029f\u1d0f\u1d04\u1d0b\ua731 \u1d1b\u029c\u1d07\u028f", "&7\u1d18\u029f\u1d00\u1d04\u1d07\u1d05 \u1d1b\u029c\u026a\ua731 \u1d0d\u1d00\u1d1b\u1d04\u029c.", "", TOGGLE_OFF)).build());
        this.inventory.setItem(26, gui.toggle(MENU, "cloning", this.arena.isCloningEnabled(), arenaToken,
                Face.of(ON_MATERIAL, "&d\u1d04\u029f\u1d0f\u0274\u026a\u0274\u0262 \u1d07\u0274\u1d00\u0299\u029f\u1d07\u1d05", "&7\u1d00\u029f\u029f\u1d0f\u1d21 &e/\u1d00\u0280\u1d07\u0274\u1d00 {arena} \u1d04\u1d0f\u1d18\u028f&7 \u1d00\u0274\u1d05", "&e/\u1d00\u0280\u1d07\u0274\u1d00 {arena} \u1d18\u1d00\ua731\u1d1b\u1d07&7.", "", TOGGLE_ON),
                Face.of(OFF_MATERIAL, "&d\u1d04\u029f\u1d0f\u0274\u026a\u0274\u0262 \u1d07\u0274\u1d00\u0299\u029f\u1d07\u1d05", "&7\u1d00\u029f\u029f\u1d0f\u1d21 &e/\u1d00\u0280\u1d07\u0274\u1d00 {arena} \u1d04\u1d0f\u1d18\u028f&7 \u1d00\u0274\u1d05", "&e/\u1d00\u0280\u1d07\u0274\u1d00 {arena} \u1d18\u1d00\ua731\u1d1b\u1d07&7.", "", TOGGLE_OFF)).build());
        this.inventory.setItem(30, gui.toggle(MENU, "allow-spectators", this.arena.isAllowSpectators(), null,
                Face.of(ON_MATERIAL, "&d\u1d00\u029f\u029f\u1d0f\u1d21 \ua731\u1d18\u1d07\u1d04\u1d1b\u1d00\u1d1b\u1d0f\u0280\ua731", "&7\u1d04\u1d00\u0274 \u1d18\u029f\u1d00\u028f\u1d07\u0280\ua731 /\ua731\u1d18\u1d07\u1d04\u1d1b\u1d00\u1d1b\u1d07 \u1d1b\u029c\u026a\ua731 \ua730\u026a\u0262\u029c\u1d1b?", "", TOGGLE_ON),
                Face.of(OFF_MATERIAL, "&d\u1d00\u029f\u029f\u1d0f\u1d21 \ua731\u1d18\u1d07\u1d04\u1d1b\u1d00\u1d1b\u1d0f\u0280\ua731", "&7\u1d04\u1d00\u0274 \u1d18\u029f\u1d00\u028f\u1d07\u0280\ua731 /\ua731\u1d18\u1d07\u1d04\u1d1b\u1d00\u1d1b\u1d07 \u1d1b\u029c\u026a\ua731 \ua730\u026a\u0262\u029c\u1d1b?", "", TOGGLE_OFF)).build());
        this.inventory.setItem(36, gui.toggle(MENU, "allow-explosions", this.arena.isAllowExplosions(), null,
                Face.of(ON_MATERIAL, "&d\u1d00\u029f\u029f\u1d0f\u1d21 \u1d07x\u1d18\u029f\u1d0f\ua731\u026a\u1d0f\u0274\ua731", "&7\u1d1b\u0274\u1d1b / \u1d04\u0280\u1d07\u1d07\u1d18\u1d07\u0280 \u1d07x\u1d18\u029f\u1d0f\ua731\u026a\u1d0f\u0274\ua731.", "", TOGGLE_ON),
                Face.of(OFF_MATERIAL, "&d\u1d00\u029f\u029f\u1d0f\u1d21 \u1d07x\u1d18\u029f\u1d0f\ua731\u026a\u1d0f\u0274\ua731", "&7\u1d1b\u0274\u1d1b / \u1d04\u0280\u1d07\u1d07\u1d18\u1d07\u0280 \u1d07x\u1d18\u029f\u1d0f\ua731\u026a\u1d0f\u0274\ua731.", "", TOGGLE_OFF)).build());
        this.inventory.setItem(37, gui.toggle(MENU, "allow-fire-spread", this.arena.isAllowFireSpread(), null,
                Face.of(ON_MATERIAL, "&d\u1d00\u029f\u029f\u1d0f\u1d21 \ua730\u026a\u0280\u1d07 \ua731\u1d18\u0280\u1d07\u1d00\u1d05", "&7\ua730\u026a\u0280\u1d07 \ua731\u1d18\u0280\u1d07\u1d00\u1d05\u026a\u0274\u0262 / \u0299\u1d1c\u0280\u0274\u026a\u0274\u0262 \u0299\u029f\u1d0f\u1d04\u1d0b\ua731.", "", TOGGLE_ON),
                Face.of(OFF_MATERIAL, "&d\u1d00\u029f\u029f\u1d0f\u1d21 \ua730\u026a\u0280\u1d07 \ua731\u1d18\u0280\u1d07\u1d00\u1d05", "&7\ua730\u026a\u0280\u1d07 \ua731\u1d18\u0280\u1d07\u1d00\u1d05\u026a\u0274\u0262 / \u0299\u1d1c\u0280\u0274\u026a\u0274\u0262 \u0299\u029f\u1d0f\u1d04\u1d0b\ua731.", "", TOGGLE_OFF)).build());
        this.inventory.setItem(38, gui.toggle(MENU, "allow-liquid-flow", this.arena.isAllowLiquidFlow(), null,
                Face.of(ON_MATERIAL, "&d\u1d00\u029f\u029f\u1d0f\u1d21 \u029f\u026a\ua7af\u1d1c\u026a\u1d05 \ua730\u029f\u1d0f\u1d21", "&7\u1d21\u1d00\u1d1b\u1d07\u0280 / \u029f\u1d00\u1d20\u1d00 \ua730\u029f\u1d0f\u1d21\u026a\u0274\u0262.", "", TOGGLE_ON),
                Face.of(OFF_MATERIAL, "&d\u1d00\u029f\u029f\u1d0f\u1d21 \u029f\u026a\ua7af\u1d1c\u026a\u1d05 \ua730\u029f\u1d0f\u1d21", "&7\u1d21\u1d00\u1d1b\u1d07\u0280 / \u029f\u1d00\u1d20\u1d00 \ua730\u029f\u1d0f\u1d21\u026a\u0274\u0262.", "", TOGGLE_OFF)).build());
        this.inventory.setItem(39, gui.toggle(MENU, "allow-mob-spawns", this.arena.isAllowMobSpawns(), null,
                Face.of(ON_MATERIAL, "&d\u1d00\u029f\u029f\u1d0f\u1d21 \u1d0d\u1d0f\u0299 \ua731\u1d18\u1d00\u1d21\u0274\ua731", "&7\u0274\u1d00\u1d1b\u1d1c\u0280\u1d00\u029f \u1d0d\u1d0f\u0299 \ua731\u1d18\u1d00\u1d21\u0274\u026a\u0274\u0262.", "", TOGGLE_ON),
                Face.of(OFF_MATERIAL, "&d\u1d00\u029f\u029f\u1d0f\u1d21 \u1d0d\u1d0f\u0299 \ua731\u1d18\u1d00\u1d21\u0274\ua731", "&7\u0274\u1d00\u1d1b\u1d1c\u0280\u1d00\u029f \u1d0d\u1d0f\u0299 \ua731\u1d18\u1d00\u1d21\u0274\u026a\u0274\u0262.", "", TOGGLE_OFF)).build());
        this.inventory.setItem(40, gui.toggle(MENU, "allow-fall-damage", this.arena.isAllowFallDamage(), null,
                Face.of(ON_MATERIAL, "&d\u1d00\u029f\u029f\u1d0f\u1d21 \ua730\u1d00\u029f\u029f \u1d05\u1d00\u1d0d\u1d00\u0262\u1d07", "&7\ua730\u1d00\u029f\u029f \u1d05\u1d00\u1d0d\u1d00\u0262\u1d07 \u1d21\u029c\u026a\u029f\u1d07 \ua730\u026a\u0262\u029c\u1d1b\u026a\u0274\u0262.", "", TOGGLE_ON),
                Face.of(OFF_MATERIAL, "&d\u1d00\u029f\u029f\u1d0f\u1d21 \ua730\u1d00\u029f\u029f \u1d05\u1d00\u1d0d\u1d00\u0262\u1d07", "&7\ua730\u1d00\u029f\u029f \u1d05\u1d00\u1d0d\u1d00\u0262\u1d07 \u1d21\u029c\u026a\u029f\u1d07 \ua730\u026a\u0262\u029c\u1d1b\u026a\u0274\u0262.", "", TOGGLE_OFF)).build());
        this.inventory.setItem(41, gui.toggle(MENU, "allow-hunger-loss", this.arena.isAllowHungerLoss(), null,
                Face.of(ON_MATERIAL, "&d\u1d00\u029f\u029f\u1d0f\u1d21 \u029c\u1d1c\u0274\u0262\u1d07\u0280 \u029f\u1d0f\ua731\ua731", "&7\u029c\u1d1c\u0274\u0262\u1d07\u0280 \u1d05\u0280\u1d00\u026a\u0274\ua731 \u1d05\u1d1c\u0280\u026a\u0274\u0262 \u1d00 \ua730\u026a\u0262\u029c\u1d1b.", "", TOGGLE_ON),
                Face.of(OFF_MATERIAL, "&d\u1d00\u029f\u029f\u1d0f\u1d21 \u029c\u1d1c\u0274\u0262\u1d07\u0280 \u029f\u1d0f\ua731\ua731", "&7\u029c\u1d1c\u0274\u0262\u1d07\u0280 \u1d05\u0280\u1d00\u026a\u0274\ua731 \u1d05\u1d1c\u0280\u026a\u0274\u0262 \u1d00 \ua730\u026a\u0262\u029c\u1d1b.", "", TOGGLE_OFF)).build());
        this.inventory.setItem(42, gui.toggle(MENU, "lock-clear-weather", this.arena.isLockClearWeather(), null,
                Face.of(ON_MATERIAL, "&d\u029f\u1d0f\u1d04\u1d0b \u1d04\u029f\u1d07\u1d00\u0280 \u1d21\u1d07\u1d00\u1d1b\u029c\u1d07\u0280", "&7\u1d0b\u1d07\u1d07\u1d18 \u1d1b\u029c\u1d07 \ua731\u1d0b\u028f \u1d04\u029f\u1d07\u1d00\u0280 \u1d05\u1d1c\u0280\u026a\u0274\u0262 \ua730\u026a\u0262\u029c\u1d1b\ua731.", "", TOGGLE_ON),
                Face.of(OFF_MATERIAL, "&d\u029f\u1d0f\u1d04\u1d0b \u1d04\u029f\u1d07\u1d00\u0280 \u1d21\u1d07\u1d00\u1d1b\u029c\u1d07\u0280", "&7\u1d0b\u1d07\u1d07\u1d18 \u1d1b\u029c\u1d07 \ua731\u1d0b\u028f \u1d04\u029f\u1d07\u1d00\u0280 \u1d05\u1d1c\u0280\u026a\u0274\u0262 \ua730\u026a\u0262\u029c\u1d1b\ua731.", "", TOGGLE_OFF)).build());
        this.inventory.setItem(43, gui.toggle(MENU, "lock-day-time", this.arena.isLockDayTime(), null,
                Face.of(ON_MATERIAL, "&d\u029f\u1d0f\u1d04\u1d0b \u1d05\u1d00\u028f \u1d1b\u026a\u1d0d\u1d07", "&7\u1d0b\u1d07\u1d07\u1d18 \u026a\u1d1b \u1d05\u1d00\u028f\u1d1b\u026a\u1d0d\u1d07 \u1d05\u1d1c\u0280\u026a\u0274\u0262 \ua730\u026a\u0262\u029c\u1d1b\ua731.", "", TOGGLE_ON),
                Face.of(OFF_MATERIAL, "&d\u029f\u1d0f\u1d04\u1d0b \u1d05\u1d00\u028f \u1d1b\u026a\u1d0d\u1d07", "&7\u1d0b\u1d07\u1d07\u1d18 \u026a\u1d1b \u1d05\u1d00\u028f\u1d1b\u026a\u1d0d\u1d07 \u1d05\u1d1c\u0280\u026a\u0274\u0262 \ua730\u026a\u0262\u029c\u1d1b\ua731.", "", TOGGLE_OFF)).build());
        this.inventory.setItem(46, gui.toggle(MENU, "border-particles", this.arena.isBorderParticles(), null,
                Face.of(ON_MATERIAL, "&d\u0299\u1d0f\u0280\u1d05\u1d07\u0280 \u1d18\u1d00\u0280\u1d1b\u026a\u1d04\u029f\u1d07\ua731", "&7\u1d04\u1d0f\ua731\u1d0d\u1d07\u1d1b\u026a\u1d04 \u1d18\u1d00\u0280\u1d1b\u026a\u1d04\u029f\u1d07 \u1d0f\u1d1c\u1d1b\u029f\u026a\u0274\u1d07 (\u1d04\u1d0f\u1d0d\u026a\u0274\u0262 \ua731\u1d0f\u1d0f\u0274).", "", TOGGLE_ON),
                Face.of(OFF_MATERIAL, "&d\u0299\u1d0f\u0280\u1d05\u1d07\u0280 \u1d18\u1d00\u0280\u1d1b\u026a\u1d04\u029f\u1d07\ua731", "&7\u1d04\u1d0f\ua731\u1d0d\u1d07\u1d1b\u026a\u1d04 \u1d18\u1d00\u0280\u1d1b\u026a\u1d04\u029f\u1d07 \u1d0f\u1d1c\u1d1b\u029f\u026a\u0274\u1d07 (\u1d04\u1d0f\u1d0d\u026a\u0274\u0262 \ua731\u1d0f\u1d0f\u0274).", "", TOGGLE_OFF)).build());
        this.inventory.setItem(47, gui.toggle(MENU, "glowing-opponent", this.arena.isGlowingOpponent(), null,
                Face.of(ON_MATERIAL, "&d\u0262\u029f\u1d0f\u1d21\u026a\u0274\u0262 \u1d0f\u1d18\u1d18\u1d0f\u0274\u1d07\u0274\u1d1b", "&7\u0299\u1d0f\u1d1b\u029c \ua730\u026a\u0262\u029c\u1d1b\u1d07\u0280\ua731 \u0262\u029f\u1d0f\u1d21 \u1d05\u1d1c\u0280\u026a\u0274\u0262 \u1d1b\u029c\u1d07 \u1d0d\u1d00\u1d1b\u1d04\u029c.", "", TOGGLE_ON),
                Face.of(OFF_MATERIAL, "&d\u0262\u029f\u1d0f\u1d21\u026a\u0274\u0262 \u1d0f\u1d18\u1d18\u1d0f\u0274\u1d07\u0274\u1d1b", "&7\u0299\u1d0f\u1d1b\u029c \ua730\u026a\u0262\u029c\u1d1b\u1d07\u0280\ua731 \u0262\u029f\u1d0f\u1d21 \u1d05\u1d1c\u0280\u026a\u0274\u0262 \u1d1b\u029c\u1d07 \u1d0d\u1d00\u1d1b\u1d04\u029c.", "", TOGGLE_OFF)).build());

        // The four tunable numbers. Left-click adds one, right-click takes one.
        this.inventory.setItem(14, gui.face(MENU, "regen-delay", tokens(this.arena.getRegenDelayTicks()),
                Face.of(Material.CLOCK, "&d\u0280\u1d07\u0262\u1d07\u0274 \u1d05\u1d07\u029f\u1d00\u028f", "&7\u1d05\u1d07\u029f\u1d00\u028f \u0299\u1d07\ua730\u1d0f\u0280\u1d07 \u0280\u1d07\u0262\u1d07\u0274\u1d07\u0280\u1d00\u1d1b\u026a\u0274\u0262.", "", "&f\u1d04\u1d1c\u0280\u0280\u1d07\u0274\u1d1b: &d{value} \u1d1b\u026a\u1d04\u1d0b\ua731", NUMBER_HINT)).build());
        this.inventory.setItem(15, gui.face(MENU, "countdown-override", tokens(this.arena.getCountdownOverride()),
                Face.of(Material.CLOCK, "&d\u1d04\u1d0f\u1d1c\u0274\u1d1b\u1d05\u1d0f\u1d21\u0274 \u1d0f\u1d20\u1d07\u0280\u0280\u026a\u1d05\u1d07", "&7\u1d04\u1d1c\ua731\u1d1b\u1d0f\u1d0d \ua730\u026a\u0262\u029c\u1d1b \u1d04\u1d0f\u1d1c\u0274\u1d1b\u1d05\u1d0f\u1d21\u0274 \u029f\u1d07\u0274\u0262\u1d1b\u029c.", "", "&f\u1d04\u1d1c\u0280\u0280\u1d07\u0274\u1d1b: &d{value} \ua731 (-1 = \u1d05\u1d07\ua730\u1d00\u1d1c\u029f\u1d1b)", NUMBER_HINT)).build());
        this.inventory.setItem(23, gui.face(MENU, "event-border-end", tokens(this.arena.getEventBorderEnd()),
                Face.of(Material.CLOCK, "&e\u1d07\u1d20\u1d07\u0274\u1d1b \u0299\u1d0f\u0280\u1d05\u1d07\u0280 \u1d07\u0274\u1d05", "&7\u1d1b\u029c\u1d07 \u0299\u1d0f\u0280\u1d05\u1d07\u0280 \ua731\u1d1b\u1d0f\u1d18\ua731 \ua731\u029c\u0280\u026a\u0274\u1d0b\u026a\u0274\u0262 \u029c\u1d07\u0280\u1d07.", "", "&f\u1d04\u1d1c\u0280\u0280\u1d07\u0274\u1d1b: &d{value} \u0299\u029f\u1d0f\u1d04\u1d0b\ua731", NUMBER_HINT)).build());
        this.inventory.setItem(24, gui.face(MENU, "event-shrink-interval", tokens(this.arena.getEventBorderInterval()),
                Face.of(Material.CLOCK, "&e\u1d07\u1d20\u1d07\u0274\u1d1b \ua731\u029c\u0280\u026a\u0274\u1d0b \u026a\u0274\u1d1b\u1d07\u0280\u1d20\u1d00\u029f", "&7\u1d1b\u026a\u1d0d\u1d07 \u0299\u1d07\u1d1b\u1d21\u1d07\u1d07\u0274 \u1d07\u1d00\u1d04\u029c \ua731\u029c\u0280\u026a\u0274\u1d0b \ua731\u1d1b\u1d07\u1d18.", "", "&f\u1d04\u1d1c\u0280\u0280\u1d07\u0274\u1d1b: &d{value} \ua731\u1d07\u1d04\u1d0f\u0274\u1d05\ua731", NUMBER_HINT)).build());
        this.inventory.setItem(25, gui.face(MENU, "event-shrink-step", tokens(this.arena.getEventBorderStep()),
                Face.of(Material.CLOCK, "&e\u1d07\u1d20\u1d07\u0274\u1d1b \ua731\u029c\u0280\u026a\u0274\u1d0b \ua731\u1d1b\u1d07\u1d18", "&7\u029c\u1d0f\u1d21 \u1d0d\u1d1c\u1d04\u029c \ua731\u1d0d\u1d00\u029f\u029f\u1d07\u0280 \u1d07\u1d00\u1d04\u029c \ua731\u029c\u0280\u026a\u0274\u1d0b.", "", "&f\u1d04\u1d1c\u0280\u0280\u1d07\u0274\u1d1b: &d{value} \u0299\u029f\u1d0f\u1d04\u1d0b\ua731", NUMBER_HINT)).build());

        this.inventory.setItem(9, gui.face(MENU, "setup", null,
                Face.of(Material.MAGENTA_STAINED_GLASS_PANE, "&d&l\ua731\u1d07\u1d1b\u1d1c\u1d18",
                        "&7\u0280\u1d07-\u0280\u1d1c\u0274 \u1d1b\u029c\u1d07 \u1d18\u1d0f\ua731\u026a\u1d1b\u026a\u1d0f\u0274 \ua731\u1d07\u1d1b\u1d1c\u1d18 \u1d21\u026a\u1d22\u1d00\u0280\u1d05.")).build());
        this.inventory.setItem(11, gui.face(MENU, "regen-now", null,
                Face.of(Material.NETHER_STAR, "&d&l\u0280\u1d07\u0262\u1d07\u0274 \u0274\u1d0f\u1d21",
                        "&7\ua730\u1d0f\u0280\u1d04\u1d07-\u0280\u1d07\ua731\u1d1b\u1d0f\u0280\u1d07 \u1d1b\u029c\u1d07 \u1d00\u0280\u1d07\u0274\u1d00 \u1d1b\u1d0f \u026a\u1d1b\ua731", "&7\ua731\u1d00\u1d20\u1d07\u1d05 \ua731\u0274\u1d00\u1d18\ua731\u029c\u1d0f\u1d1b \u0280\u026a\u0262\u029c\u1d1b \u0274\u1d0f\u1d21.")).build());
        this.inventory.setItem(12, gui.face(MENU, "snapshot-now", null,
                Face.of(Material.ENDER_EYE, "&d&l\ua731\u0274\u1d00\u1d18\ua731\u029c\u1d0f\u1d1b \u0274\u1d0f\u1d21",
                        "&7\ua731\u1d00\u1d20\u1d07 \u1d1b\u029c\u1d07 \u1d00\u0280\u1d07\u0274\u1d00'\ua731 \u1d04\u1d1c\u0280\u0280\u1d07\u0274\u1d1b \u0299\u029f\u1d0f\u1d04\u1d0b\ua731", "&7\u1d00\ua731 \u1d1b\u029c\u1d07 \u0274\u1d07\u1d21 \u0280\u1d07\u0262\u1d07\u0274 \u0299\u1d00\ua731\u1d07\u029f\u026a\u0274\u1d07.")).build());
        this.inventory.setItem(16, gui.face(MENU, "kits", tokens,
                Face.of(Material.CHEST, "&e&l\u1d04\u1d0f\u1d0d\u1d18\u1d00\u1d1b\u026a\u0299\u029f\u1d07 \u1d0b\u026a\u1d1b\ua731",
                        "&7\u1d04\u029c\u1d0f\u1d0f\ua731\u1d07 \u1d21\u029c\u026a\u1d04\u029c \u1d0b\u026a\u1d1b\ua731 \u1d04\u1d00\u0274 \u0299\u1d07 \u1d1c\ua731\u1d07\u1d05 \u029c\u1d07\u0280\u1d07.", "&7\u1d04\u1d1c\u0280\u0280\u1d07\u0274\u1d1b\u029f\u028f: &f{kits} \u1d0b\u026a\u1d1b(\ua731)", "&8(\u0274\u1d0f\u0274\u1d07 = \u1d00\u029f\u029f \u1d0b\u026a\u1d1b\ua731 \u1d00\u029f\u029f\u1d0f\u1d21\u1d07\u1d05)")).build());
        this.inventory.setItem(17, gui.face(MENU, "info", tokens,
                Face.of(Material.BOOK, "&7\u1d00\u0280\u1d07\u0274\u1d00 \u026a\u0274\ua730\u1d0f",
                        "&7\u1d21\u1d0f\u0280\u029f\u1d05: &f{world}", "&7\u0280\u1d07\u0262\u026a\u1d0f\u0274: &f{region}", "&8\u1d04\u029c\u1d00\u0274\u0262\u1d07\ua731 \ua731\u1d00\u1d20\u1d07 \u1d00\u1d1c\u1d1b\u1d0f\u1d0d\u1d00\u1d1b\u026a\u1d04\u1d00\u029f\u029f\u028f.")).build());
        Map<String, String> event_spawn = this.tokens();
        event_spawn.put("state", this.arena.getEventSpawn() != null
                ? gui.line(MENU, "event-spawn", "state-set", "&a\ua731\u1d07\u1d1b", null)
                : gui.line(MENU, "event-spawn", "state-unset", "&c\u0274\u1d0f\u1d1b \ua731\u1d07\u1d1b", null));
        this.inventory.setItem(21, gui.face(MENU, "event-spawn", event_spawn,
                Face.of(Material.BEACON, "&e&l\ua730\ua730\u1d00 / \u1d07\u1d20\u1d07\u0274\u1d1b \ua731\u1d18\u1d00\u1d21\u0274",
                        "&7\u1d21\u029c\u1d07\u0280\u1d07 \ua730\ua730\u1d00 \u1d07\u1d20\u1d07\u0274\u1d1b \u1d00\u0274\u1d05 \u1d18\u1d00\u0280\u1d1b\u028f \u1d0d\u1d00\u1d1b\u1d04\u029c", "&7\u1d18\u029f\u1d00\u028f\u1d07\u0280\ua731 \u1d00\u0280\u1d07 \u1d1b\u1d07\u029f\u1d07\u1d18\u1d0f\u0280\u1d1b\u1d07\u1d05 \u026a\u0274.", "&7\u1d04\u1d1c\u0280\u0280\u1d07\u0274\u1d1b: {state}", "", "&c\u0280\u1d07\ua7af\u1d1c\u026a\u0280\u1d07\u1d05&7 - \u1d00\u0274 \u1d00\u0280\u1d07\u0274\u1d00 \u1d21\u026a\u1d1b\u029c\u1d0f\u1d1c\u1d1b \u1d1b\u029c\u026a\ua731", "&7\u1d04\u1d00\u0274'\u1d1b \u029c\u1d0f\ua731\u1d1b \ua730\ua730\u1d00 \u1d0f\u0280 \u1d18\u1d00\u0280\u1d1b\u028f \u1d0d\u1d00\u1d1b\u1d04\u029c\u1d07\ua731.", "", "&e\u1d04\u029f\u026a\u1d04\u1d0b&7, \u1d21\u1d00\u029f\u1d0b \u1d1b\u1d0f \u1d1b\u029c\u1d07 \ua731\u1d18\u1d0f\u1d1b, \u1d1b\u028f\u1d18\u1d07 &e\u1d05\u1d0f\u0274\u1d07&7.")).build());
        Map<String, String> border_area = this.tokens();
        Map<String, String> blocks = this.tokens();
        blocks.put("blocks", String.valueOf((int) this.arena.getBorderStartSize()));
        border_area.put("state", this.arena.hasBorderRegion()
                ? gui.line(MENU, "border-area", "state-set", "&a\ua731\u1d07\u1d1b &7({blocks} \u0299\u029f\u1d0f\u1d04\u1d0b\ua731)", blocks)
                : gui.line(MENU, "border-area", "state-unset", "&c\u0274\u1d0f\u1d1b \ua731\u1d07\u1d1b &7(\u1d00\u1d1c\u1d1b\u1d0f-\ua730\u026a\u1d1b)", null));
        this.inventory.setItem(22, gui.face(MENU, "border-area", border_area,
                Face.of(Material.FILLED_MAP, "&e&l\u1d07\u1d20\u1d07\u0274\u1d1b \u0299\u1d0f\u0280\u1d05\u1d07\u0280 \u1d00\u0280\u1d07\u1d00",
                        "&7\u1d1b\u029c\u1d07 \ua731\u1d1b\u1d00\u0280\u1d1b\u026a\u0274\u0262 \u0299\u1d0f\u0280\u1d05\u1d07\u0280 \u0299\u1d0fx.", "&7\u1d04\u1d1c\u0280\u0280\u1d07\u0274\u1d1b: {state}", "", "&e\u1d04\u029f\u026a\u1d04\u1d0b&7, \u1d21\u1d00\u029f\u1d0b \u1d1b\u1d0f &f\u1d04\u1d0f\u0280\u0274\u1d07\u0280 1&7 \u1d1b\u028f\u1d18\u1d07 &e\u1d05\u1d0f\u0274\u1d07&7,", "&7\u1d1b\u029c\u1d07\u0274 &f\u1d04\u1d0f\u0280\u0274\u1d07\u0280 2&7 \u1d1b\u028f\u1d18\u1d07 &e\u1d05\u1d0f\u0274\u1d07&7.")).build());
        this.inventory.setItem(45, gui.face(MENU, "void-rescue", null,
                Face.of(Material.BLUE_STAINED_GLASS_PANE, "&b\u1d20\u1d0f\u026a\u1d05 \u0280\u1d07\ua731\u1d04\u1d1c\u1d07",
                        "&7\ua730\u1d00\u029f\u029f\u026a\u0274\u0262 \u026a\u0274\u1d1b\u1d0f \u1d1b\u029c\u1d07 \u1d20\u1d0f\u026a\u1d05 \u1d1b\u1d07\u029f\u1d07\u1d18\u1d0f\u0280\u1d1b\ua731", "&7\u1d18\u029f\u1d00\u028f\u1d07\u0280\ua731 \u0299\u1d00\u1d04\u1d0b \u1d1b\u1d0f \u1d1b\u029c\u1d07\u026a\u0280 \ua731\u1d18\u1d00\u1d21\u0274.", "&8\u1d00\u029f\u1d21\u1d00\u028f\ua731 \u1d0f\u0274.")).build());
        this.inventory.setItem(52, gui.face(MENU, "description", tokens,
                Face.of(Material.NAME_TAG, "&7\u1d05\u1d07\ua731\u1d04\u0280\u026a\u1d18\u1d1b\u026a\u1d0f\u0274",
                        "&7{description}")).build());
        Map<String, String> tp_spawn1 = this.tokens();
        tp_spawn1.put("state", this.arena.getSpawn1() != null
                ? gui.line(MENU, "tp-spawn1", "state-set", "&7\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d1b\u1d07\u029f\u1d07\u1d18\u1d0f\u0280\u1d1b \u1d1b\u029c\u1d07\u0280\u1d07.", null)
                : gui.line(MENU, "tp-spawn1", "state-unset", "&8\u0274\u1d0f\u1d1b \ua731\u1d07\u1d1b.", null));
        this.inventory.setItem(48, gui.face(MENU, "tp-spawn1", tp_spawn1,
                Face.of(Material.ENDER_PEARL, "&b&l\u1d1b\u1d18 \u2192 \u1d18\u029f\u1d00\u028f\u1d07\u0280 1 \ua731\u1d18\u1d00\u1d21\u0274",
                        "{state}")).build());
        Map<String, String> tp_spawn2 = this.tokens();
        tp_spawn2.put("state", this.arena.getSpawn2() != null
                ? gui.line(MENU, "tp-spawn2", "state-set", "&7\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d1b\u1d07\u029f\u1d07\u1d18\u1d0f\u0280\u1d1b \u1d1b\u029c\u1d07\u0280\u1d07.", null)
                : gui.line(MENU, "tp-spawn2", "state-unset", "&8\u0274\u1d0f\u1d1b \ua731\u1d07\u1d1b.", null));
        this.inventory.setItem(49, gui.face(MENU, "tp-spawn2", tp_spawn2,
                Face.of(Material.ENDER_PEARL, "&b&l\u1d1b\u1d18 \u2192 \u1d18\u029f\u1d00\u028f\u1d07\u0280 2 \ua731\u1d18\u1d00\u1d21\u0274",
                        "{state}")).build());
        Map<String, String> tp_event = this.tokens();
        tp_event.put("state", this.arena.getEventSpawn() != null
                ? gui.line(MENU, "tp-event", "state-set", "&7\u1d04\u029f\u026a\u1d04\u1d0b \u1d1b\u1d0f \u1d1b\u1d07\u029f\u1d07\u1d18\u1d0f\u0280\u1d1b \u1d1b\u029c\u1d07\u0280\u1d07.", null)
                : gui.line(MENU, "tp-event", "state-unset", "&8\u0274\u1d0f\u1d1b \ua731\u1d07\u1d1b.", null));
        this.inventory.setItem(50, gui.face(MENU, "tp-event", tp_event,
                Face.of(Material.ENDER_EYE, "&b&l\u1d1b\u1d18 \u2192 \u1d07\u1d20\u1d07\u0274\u1d1b \ua731\u1d18\u1d00\u1d21\u0274",
                        "{state}")).build());
    }

    /**
     * Everything this screen's labels can refer to.
     *
     * <p>Built once per open rather than per item: half of these are string
     * formatting over the same arena, and the set-or-not lines are named
     * lines so a server can reword "not set" without touching the sentence
     * it sits in.
     */
    private Map<String, String> tokens() {
        GuiConfigManager gui = this.plugin.gui();
        Map<String, String> t = new HashMap<String, String>();
        t.put("arena", this.arena.getName());
        t.put("kits", String.valueOf(this.arena.getKits().size()));
        t.put("world", this.arena.getWorldName());
        t.put("region", this.describeRegion());
        t.put("description", this.arena.getDescription().isEmpty()
                ? gui.line(MENU, "description", "none-set", "&8(\u0274\u1d0f\u0274\u1d07 \ua731\u1d07\u1d1b)", null)
                : this.arena.getDescription());
        return t;
    }

    /** The number tokens for one tunable value. */
    private Map<String, String> tokens(int value) {
        Map<String, String> t = this.tokens();
        t.put("value", String.valueOf(value));
        return t;
    }

    private String describeRegion() {
        if (this.arena.getMin() == null) {
            return "not set";
        }
        return this.arena.getMin().getBlockX() + "," + this.arena.getMin().getBlockY() + "," + this.arena.getMin().getBlockZ() + " \u2192 " + this.arena.getMax().getBlockX() + "," + this.arena.getMax().getBlockY() + "," + this.arena.getMax().getBlockZ();
    }


    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) {
            return;
        }
        if (!this.arena.isConfigured()) {
            if (slot == 31) {
                player.closeInventory();
                this.plugin.getSetupManager().begin(player, this.arena);
            }
            return;
        }
        boolean rightClick = event.isRightClick();
        switch (slot) {
            case 9: {
                player.closeInventory();
                this.plugin.getSetupManager().begin(player, this.arena);
                return;
            }
            case 10: {
                this.arena.setAutoRegenerate(!this.arena.isAutoRegenerate());
                break;
            }
            case 11: {
                int written = this.plugin.getArenaManager().regenArena(this.arena);
                if (written < 0) {
                    player.sendMessage(this.plugin.messages().get("notice.arena.no-snapshot-saved-yet"));
                } else {
                    player.sendMessage(this.plugin.messages().get("notice.arena.regenerated-block-s", "written", String.valueOf(written)));
                }
                return;
            }
            case 12: {
                this.plugin.getArenaManager().snapshotArena(this.arena);
                player.sendMessage(this.plugin.messages().get("notice.arena.snapshot-saved", "arena", this.arena.getName()));
                return;
            }
            case 13: {
                this.arena.setEnabled(!this.arena.isEnabled());
                break;
            }
            case 14: {
                this.arena.setRegenDelayTicks(this.arena.getRegenDelayTicks() + (rightClick ? -10 : 10));
                break;
            }
            case 15: {
                this.arena.setCountdownOverride(ArenaMenu.clampCountdown(this.arena.getCountdownOverride() + (rightClick ? -1 : 1)));
                break;
            }
            case 16: {
                new KitSelectMenu(this.plugin, this.arena).open(player);
                return;
            }
            case 18: {
                this.arena.setAllowBuild(!this.arena.isAllowBuild());
                break;
            }
            case 19: {
                this.arena.setAllowBreak(!this.arena.isAllowBreak());
                break;
            }
            case 20: {
                this.arena.setAllowRemoveAdded(!this.arena.isAllowRemoveAdded());
                break;
            }
            case 21: {
                player.closeInventory();
                this.plugin.getSetupManager().beginEventSpawn(player, this.arena);
                return;
            }
            case 22: {
                player.closeInventory();
                this.plugin.getSetupManager().beginBorder(player, this.arena);
                return;
            }
            case 23: {
                this.arena.setEventBorderEnd(this.arena.getEventBorderEnd() + (rightClick ? -5 : 5));
                break;
            }
            case 24: {
                this.arena.setEventBorderInterval(this.arena.getEventBorderInterval() + (rightClick ? -10 : 10));
                break;
            }
            case 25: {
                this.arena.setEventBorderStep(this.arena.getEventBorderStep() + (rightClick ? -5 : 5));
                break;
            }
            case 26: {
                this.arena.setCloningEnabled(!this.arena.isCloningEnabled());
                break;
            }
            case 30: {
                this.arena.setAllowSpectators(!this.arena.isAllowSpectators());
                break;
            }
            case 36: {
                this.arena.setAllowExplosions(!this.arena.isAllowExplosions());
                break;
            }
            case 37: {
                this.arena.setAllowFireSpread(!this.arena.isAllowFireSpread());
                break;
            }
            case 38: {
                this.arena.setAllowLiquidFlow(!this.arena.isAllowLiquidFlow());
                break;
            }
            case 39: {
                this.arena.setAllowMobSpawns(!this.arena.isAllowMobSpawns());
                break;
            }
            case 40: {
                this.arena.setAllowFallDamage(!this.arena.isAllowFallDamage());
                break;
            }
            case 41: {
                this.arena.setAllowHungerLoss(!this.arena.isAllowHungerLoss());
                break;
            }
            case 42: {
                this.arena.setLockClearWeather(!this.arena.isLockClearWeather());
                break;
            }
            case 43: {
                this.arena.setLockDayTime(!this.arena.isLockDayTime());
                break;
            }
            case 46: {
                this.arena.setBorderParticles(!this.arena.isBorderParticles());
                break;
            }
            case 47: {
                this.arena.setGlowingOpponent(!this.arena.isGlowingOpponent());
                break;
            }
            case 48: {
                this.teleportTo(player, this.arena.getSpawn1(), "Player 1 spawn");
                return;
            }
            case 49: {
                this.teleportTo(player, this.arena.getSpawn2(), "Player 2 spawn");
                return;
            }
            case 50: {
                this.teleportTo(player, this.arena.getEventSpawn(), "event spawn");
                return;
            }
            default: {
                return;
            }
        }
        this.plugin.getArenaManager().save();
        this.build();
        player.openInventory(this.inventory);
    }

    private void teleportTo(Player player, Location loc, String label) {
        if (loc == null) {
            player.sendMessage(this.plugin.messages().get("notice.arena.isn-t-set-yet", "label", label));
            return;
        }
        player.closeInventory();
        player.teleport(loc);
        player.sendMessage(this.plugin.messages().get("notice.arena.teleported", "label", label, "arena", this.arena.getName()));
    }

    private static int clampCountdown(int v) {
        if (v < -1) {
            return -1;
        }
        return v;
    }
}

