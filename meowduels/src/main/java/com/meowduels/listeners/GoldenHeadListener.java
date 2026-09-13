/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.minimessage.MiniMessage
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerItemConsumeEvent
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 */
package com.meowduels.listeners;

import com.meowduels.MeowDuels;
import com.meowduels.util.GoldenHead;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GoldenHeadListener
implements Listener {
    private static final int REGEN_TICKS = 200;
    private static final int REGEN_AMPLIFIER = 1;
    private static final int ABSORPTION_TICKS = 2400;
    private static final int ABSORPTION_AMPLIFIER = 0;
    private static final int HUNGER_RESTORE = 4;
    private static final float SATURATION_RESTORE = 9.6f;
    private final MeowDuels plugin;
    private final Map<UUID, Long> lastEat = new HashMap<UUID, Long>();

    public GoldenHeadListener(MeowDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!GoldenHead.isGoldenHead((Plugin)this.plugin, event.getItem())) {
            return;
        }
        event.setCancelled(true);
        this.eat(event.getPlayer());
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!GoldenHead.isGoldenHead((Plugin)this.plugin, event.getItem())) {
            return;
        }
        event.setCancelled(true);
        this.eat(event.getPlayer());
    }

    private void eat(Player player) {
        long cooldownMs = (long)(this.plugin.getConfig().getDouble("golden-head.cooldown-seconds", 5.0) * 1000.0);
        long now = System.currentTimeMillis();
        if (cooldownMs > 0L) {
            Long last = this.lastEat.get(player.getUniqueId());
            if (last != null && now - last < cooldownMs) {
                double remaining = (double)(cooldownMs - (now - last)) / 1000.0;
                player.sendActionBar(GoldenHeadListener.mm("<red>Golden Head on cooldown: <yellow>" + String.format("%.1f", remaining) + "s"));
                return;
            }
            this.lastEat.put(player.getUniqueId(), now);
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0));
        int food = Math.min(20, player.getFoodLevel() + 4);
        player.setFoodLevel(food);
        player.setSaturation(Math.min((float)food, player.getSaturation() + 9.6f));
        try {
            player.playSound(player.getLocation(), "entity.player.burp", 0.7f, 1.0f);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        this.consumeOne(player);
    }

    private static Component mm(String miniMessage) {
        return MiniMessage.miniMessage().deserialize((Object)miniMessage);
    }

    private void consumeOne(Player player) {
        PlayerInventory inv = player.getInventory();
        if (GoldenHead.isGoldenHead((Plugin)this.plugin, inv.getItemInMainHand())) {
            inv.setItemInMainHand(GoldenHeadListener.shrink(inv.getItemInMainHand()));
        } else if (GoldenHead.isGoldenHead((Plugin)this.plugin, inv.getItemInOffHand())) {
            inv.setItemInOffHand(GoldenHeadListener.shrink(inv.getItemInOffHand()));
        }
    }

    private static ItemStack shrink(ItemStack stack) {
        if (stack.getAmount() <= 1) {
            return new ItemStack(Material.AIR);
        }
        stack.setAmount(stack.getAmount() - 1);
        return stack;
    }
}

