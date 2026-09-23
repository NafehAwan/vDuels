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
import com.meowduels.model.Arena;
import com.meowduels.util.Items;
import com.meowduels.util.Text;
import java.util.ArrayList;
import java.util.Arrays;
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

    @Override
    public void build() {
        this.create(6, "&d&lArena: &5" + this.arena.getName());
        ItemStack filler = Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; ++i) {
            this.inventory.setItem(i, filler);
        }
        if (!this.arena.isConfigured()) {
            this.inventory.setItem(31, Items.of(Material.MAGENTA_STAINED_GLASS_PANE).name("&d&lSetup").lore("&7Click to start arena setup.", "&7You'll be asked to walk to each", "&7spawn and corner and type &edone&7.").build());
            return;
        }
        this.inventory.setItem(9, Items.of(Material.MAGENTA_STAINED_GLASS_PANE).name("&d&lSetup").lore("&7Re-run the position setup wizard.").build());
        this.inventory.setItem(10, this.toggle("&dAuto-Regenerate", this.arena.isAutoRegenerate(), "&7Restore the arena after each fight."));
        this.inventory.setItem(11, Items.of(Material.NETHER_STAR).name("&d&lRegen Now").lore("&7Force-restore the arena to its", "&7saved snapshot right now.").build());
        this.inventory.setItem(12, Items.of(Material.ENDER_EYE).name("&d&lSnapshot Now").lore("&7Save the arena's current blocks", "&7as the new regen baseline.").build());
        this.inventory.setItem(13, this.toggle("&dEnabled (in rotation)", this.arena.isEnabled(), "&7Turn off to hide from matchmaking", "&7without deleting the arena."));
        this.inventory.setItem(14, this.numberItem("&dRegen Delay", this.arena.getRegenDelayTicks(), "ticks", "&7Delay before regenerating."));
        this.inventory.setItem(15, this.numberItem("&dCountdown Override", this.arena.getCountdownOverride(), "s (-1 = default)", "&7Custom fight countdown length."));
        this.inventory.setItem(16, Items.of(Material.CHEST).name("&e&lCompatible Kits").lore("&7Choose which kits can be used here.", "&7Currently: &f" + this.arena.getKits().size() + " kit(s)", "&8(none = all kits allowed)").build());
        this.inventory.setItem(17, Items.of(Material.BOOK).name("&7Arena Info").lore("&7World: &f" + this.arena.getWorldName(), "&7Region: &f" + this.describeRegion(), "&8Changes save automatically.").build());
        this.inventory.setItem(18, this.toggle("&dAllow Building", this.arena.isAllowBuild(), "&7Players can place blocks", "&7anywhere inside the arena."));
        this.inventory.setItem(19, this.toggle("&dAllow Breaking", this.arena.isAllowBreak(), "&7Players can break blocks", "&7anywhere inside the arena."));
        this.inventory.setItem(20, this.toggle("&dBreak Self-Placed Only", this.arena.isAllowRemoveAdded(), "&7Even with Breaking off, players", "&7can still break blocks they", "&7placed this match."));
        String eventSpawnState = this.arena.getEventSpawn() != null ? "&aset" : "&cnot set";
        this.inventory.setItem(21, Items.of(Material.BEACON).name("&e&lFFA / Event Spawn").lore("&7Where FFA event AND party match", "&7players are teleported in.", "&7Current: " + eventSpawnState, "", "&cRequired&7 - an arena without this", "&7can't host FFA or party matches.", "", "&eClick&7, walk to the spot, type &edone&7.").build());
        String borderState = this.arena.hasBorderRegion() ? "&aset &7(" + (int)this.arena.getBorderStartSize() + " blocks)" : "&cnot set &7(auto-fit)";
        this.inventory.setItem(22, Items.of(Material.FILLED_MAP).name("&e&lEvent Border Area").lore("&7The starting border box.", "&7Current: " + borderState, "", "&eClick&7, walk to &fcorner 1&7 type &edone&7,", "&7then &fcorner 2&7 type &edone&7.").build());
        this.inventory.setItem(23, this.numberItem("&eEvent Border End", this.arena.getEventBorderEnd(), "blocks", "&7The border stops shrinking here."));
        this.inventory.setItem(24, this.numberItem("&eEvent Shrink Interval", this.arena.getEventBorderInterval(), "seconds", "&7Time between each shrink step."));
        this.inventory.setItem(25, this.numberItem("&eEvent Shrink Step", this.arena.getEventBorderStep(), "blocks", "&7How much smaller each shrink."));
        this.inventory.setItem(26, this.toggle("&dCloning Enabled", this.arena.isCloningEnabled(), "&7Allow &e/arena " + this.arena.getName() + " copy&7 and", "&e/arena " + this.arena.getName() + " paste&7."));
        this.inventory.setItem(30, this.toggle("&dAllow Spectators", this.arena.isAllowSpectators(), "&7Can players /spectate this fight?"));
        this.inventory.setItem(36, this.toggle("&dAllow Explosions", this.arena.isAllowExplosions(), "&7TNT / creeper explosions."));
        this.inventory.setItem(37, this.toggle("&dAllow Fire Spread", this.arena.isAllowFireSpread(), "&7Fire spreading / burning blocks."));
        this.inventory.setItem(38, this.toggle("&dAllow Liquid Flow", this.arena.isAllowLiquidFlow(), "&7Water / lava flowing."));
        this.inventory.setItem(39, this.toggle("&dAllow Mob Spawns", this.arena.isAllowMobSpawns(), "&7Natural mob spawning."));
        this.inventory.setItem(40, this.toggle("&dAllow Fall Damage", this.arena.isAllowFallDamage(), "&7Fall damage while fighting."));
        this.inventory.setItem(41, this.toggle("&dAllow Hunger Loss", this.arena.isAllowHungerLoss(), "&7Hunger drains during a fight."));
        this.inventory.setItem(42, this.toggle("&dLock Clear Weather", this.arena.isLockClearWeather(), "&7Keep the sky clear during fights."));
        this.inventory.setItem(43, this.toggle("&dLock Day Time", this.arena.isLockDayTime(), "&7Keep it daytime during fights."));
        this.inventory.setItem(45, Items.of(Material.BLUE_STAINED_GLASS_PANE).name("&bVoid Rescue").lore("&7Falling into the void teleports", "&7players back to their spawn.", "&8Always on.").build());
        this.inventory.setItem(46, this.toggle("&dBorder Particles", this.arena.isBorderParticles(), "&7Cosmetic particle outline (coming soon)."));
        this.inventory.setItem(47, this.toggle("&dGlowing Opponent", this.arena.isGlowingOpponent(), "&7Both fighters glow during the match."));
        this.inventory.setItem(52, Items.of(Material.NAME_TAG).name("&7Description").lore("&7" + (this.arena.getDescription().isEmpty() ? "&8(none set)" : this.arena.getDescription())).build());
        this.inventory.setItem(48, Items.of(Material.ENDER_PEARL).name("&b&lTP \u2192 Player 1 Spawn").lore(this.arena.getSpawn1() != null ? "&7Click to teleport there." : "&8Not set.").build());
        this.inventory.setItem(49, Items.of(Material.ENDER_PEARL).name("&b&lTP \u2192 Player 2 Spawn").lore(this.arena.getSpawn2() != null ? "&7Click to teleport there." : "&8Not set.").build());
        this.inventory.setItem(50, Items.of(Material.ENDER_EYE).name("&b&lTP \u2192 Event Spawn").lore(this.arena.getEventSpawn() != null ? "&7Click to teleport there." : "&8Not set.").build());
    }

    private String describeRegion() {
        if (this.arena.getMin() == null) {
            return "not set";
        }
        return this.arena.getMin().getBlockX() + "," + this.arena.getMin().getBlockY() + "," + this.arena.getMin().getBlockZ() + " \u2192 " + this.arena.getMax().getBlockX() + "," + this.arena.getMax().getBlockY() + "," + this.arena.getMax().getBlockZ();
    }

    private ItemStack toggle(String name, boolean on, String ... lore) {
        Material mat = on ? Material.PINK_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        ArrayList<String> lines = new ArrayList<String>(Arrays.asList(lore));
        lines.add("");
        lines.add(on ? "&dENABLED &8(click to disable)" : "&7DISABLED &8(click to enable)");
        return Items.of(mat).name(name).lore(lines).build();
    }

    private ItemStack numberItem(String name, int value, String suffix, String ... lore) {
        ArrayList<String> lines = new ArrayList<String>(Arrays.asList(lore));
        lines.add("");
        lines.add("&fCurrent: &d" + value + " " + suffix);
        lines.add("&7Left-click: &a+1 &8| &7Right-click: &c-1");
        return Items.of(Material.CLOCK).name(name).lore(lines).build();
    }

    private ItemStack numberDisplay(String name, String valueLabel, String suffix) {
        ArrayList<String> lines = new ArrayList<String>();
        lines.add("&7" + suffix);
        lines.add("");
        lines.add("&fCurrent: &d" + valueLabel);
        lines.add("&7Left-click: &a+5 &8| &7Right-click: &c-5");
        return Items.of(Material.CLOCK).name(name).lore(lines).build();
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
                    player.sendMessage(Text.prefixed("&cNo snapshot saved yet - click &dSnapshot Now&c first."));
                } else {
                    player.sendMessage(Text.prefixed("&aRegenerated &d" + written + "&a block(s)."));
                }
                return;
            }
            case 12: {
                this.plugin.getArenaManager().snapshotArena(this.arena);
                player.sendMessage(Text.prefixed("&aSnapshot saved for &d" + this.arena.getName() + "&a."));
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
            player.sendMessage(Text.prefixed("&cThe " + label + " isn't set yet."));
            return;
        }
        player.closeInventory();
        player.teleport(loc);
        player.sendMessage(Text.prefixed("&aTeleported to the " + label + " of &d" + this.arena.getName() + "&a."));
    }

    private static int clampCountdown(int v) {
        if (v < -1) {
            return -1;
        }
        return v;
    }
}

