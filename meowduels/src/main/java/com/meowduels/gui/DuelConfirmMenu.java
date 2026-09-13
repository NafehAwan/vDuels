/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.ItemStack
 */
package com.meowduels.gui;

import com.meowduels.MeowDuels;
import com.meowduels.gui.KitPickMenu;
import com.meowduels.gui.MapSelectMenu;
import com.meowduels.gui.Menu;
import com.meowduels.model.Arena;
import com.meowduels.model.Kit;
import com.meowduels.util.Items;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class DuelConfirmMenu
extends Menu {
    private static final int[] ROUND_OPTIONS = new int[]{1, 2, 3, 5};
    private final MeowDuels plugin;
    private final Player target;
    private String selectedKit;
    private String selectedArena;
    private int roundsIndex = 1;
    private boolean ranked = false;

    public DuelConfirmMenu(MeowDuels plugin, Player target) {
        this.plugin = plugin;
        this.target = target;
    }

    public Player getTarget() {
        return this.target;
    }

    public String getSelectedKit() {
        return this.selectedKit;
    }

    public void setSelectedKit(String kit) {
        Arena arena;
        this.selectedKit = kit;
        if (!(this.selectedArena == null || (arena = this.plugin.getArenaManager().get(this.selectedArena)) != null && arena.supportsKit(kit))) {
            this.selectedArena = null;
        }
    }

    public String getSelectedArena() {
        return this.selectedArena;
    }

    public void setSelectedArena(String arena) {
        this.selectedArena = arena;
    }

    public void reopen(Player player) {
        this.build();
        player.openInventory(this.inventory);
    }

    private int currentRounds() {
        return ROUND_OPTIONS[this.roundsIndex];
    }

    @Override
    public void build() {
        this.create(1, "&8DUEL CONFIRM: &d" + this.target.getName());
        HashMap<String, Integer> buttonSlots = new HashMap<String, Integer>();
        if (this.plugin.getGuiLayoutManager().has("duelconfirm")) {
            for (Map.Entry<Integer, ItemStack> e : this.plugin.getGuiLayoutManager().get("duelconfirm").entrySet()) {
                if (e.getKey() >= 9) continue;
                String id = Items.readTag(e.getValue(), this.plugin.keyButton());
                if (id != null) {
                    buttonSlots.put(id, e.getKey());
                    continue;
                }
                this.inventory.setItem(e.getKey().intValue(), e.getValue());
            }
        } else {
            ItemStack gray = Items.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
            for (int i = 0; i < 9; ++i) {
                this.inventory.setItem(i, gray);
            }
        }
        this.inventory.setItem(buttonSlots.getOrDefault("map", 1).intValue(), this.renderButton("map"));
        this.inventory.setItem(buttonSlots.getOrDefault("kit", 3).intValue(), this.renderButton("kit"));
        this.inventory.setItem(buttonSlots.getOrDefault("clock", 5).intValue(), this.renderButton("clock"));
        this.inventory.setItem(buttonSlots.getOrDefault("ranked", 8).intValue(), this.renderButton("ranked"));
        this.inventory.setItem(buttonSlots.getOrDefault("confirm", 7).intValue(), this.renderButton("confirm"));
    }

    private ItemStack renderButton(String id) {
        return switch (id) {
            case "map" -> Items.of(Material.FILLED_MAP).name("&dArena").lore("&7Selected: &f" + (this.selectedArena == null ? "Random" : this.selectedArena), "", "&eClick to choose the map.").tag(this.plugin.keyButton(), "map").build();
            case "kit" -> {
                Kit kit = this.selectedKit == null ? null : this.plugin.getKitManager().get(this.selectedKit);
                yield Items.of(kit != null ? kit.getIcon() : Material.GOLDEN_APPLE).name("&6Kit").lore("&7Selected: &f" + (this.selectedKit == null ? "none" : this.selectedKit), "", "&eClick to choose a kit.").glow(this.selectedKit != null).tag(this.plugin.keyButton(), "kit").build();
            }
            case "clock" -> Items.of(Material.CLOCK).name("&eRounds").lore("&7First to &f" + this.currentRounds(), "", "&eClick to change.").tag(this.plugin.keyButton(), "clock").build();
            case "ranked" -> Items.of(this.ranked ? Material.LIME_DYE : Material.GRAY_DYE).name(this.ranked ? "&b&lRanked" : "&7Unranked").lore(this.ranked ? "&aCounts toward ELO rating." : "&7Casual - no ELO change.", "", "&eClick to toggle.").glow(this.ranked).tag(this.plugin.keyButton(), "ranked").build();
            default -> {
                boolean ready = this.selectedKit != null;
                yield Items.of(ready ? Material.GREEN_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE).name(ready ? "&a&lCONFIRM & SEND" : "&fSelect a kit first").lore("&fChallenge &e" + this.target.getName() + "&f.").tag(this.plugin.keyButton(), "confirm").build();
            }
        };
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        String id = Items.readTag(event.getCurrentItem(), this.plugin.keyButton());
        if (id == null) {
            return;
        }
        switch (id) {
            case "map": {
                new MapSelectMenu(this.plugin, this).open(player);
                break;
            }
            case "kit": {
                new KitPickMenu(this.plugin, this).open(player);
                break;
            }
            case "clock": {
                this.roundsIndex = (this.roundsIndex + 1) % ROUND_OPTIONS.length;
                this.reopen(player);
                break;
            }
            case "ranked": {
                this.ranked = !this.ranked;
                this.reopen(player);
                break;
            }
            case "confirm": {
                if (this.selectedKit == null) {
                    player.sendMessage(this.plugin.messages().get("menu.select-kit-first", new String[0]));
                    return;
                }
                Player online = this.plugin.getServer().getPlayer(this.target.getUniqueId());
                if (online == null) {
                    player.sendMessage(this.plugin.messages().get("menu.target-offline", "target", this.target.getName()));
                    player.closeInventory();
                    return;
                }
                this.plugin.getDuelManager().sendRequest(player, online, this.selectedKit, this.currentRounds(), this.selectedArena, this.ranked);
                player.closeInventory();
                break;
            }
        }
    }
}

