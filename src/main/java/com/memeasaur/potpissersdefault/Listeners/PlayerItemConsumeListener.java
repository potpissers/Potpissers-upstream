package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.PlayerData;
import com.memeasaur.potpissersdefault.Classes.ScoreboardTimer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.UUID;

import static com.memeasaur.potpissersdefault.PotpissersDefault.playerDataCache;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getDangerComponent;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Timer.handleScoreboardTimer;
import static org.bukkit.potion.PotionEffectType.*;
import static org.bukkit.potion.PotionEffectType.SLOW_FALLING;

public class PlayerItemConsumeListener implements Listener {
    @EventHandler
    void onPlayerMaterialEat(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        switch (e.getItem().getType()) {
            case ENCHANTED_GOLDEN_APPLE -> {
                PlayerData data = playerDataCache.get(p);
                if (e.getItem().getItemMeta().getPersistentDataContainer().has(KEY_OPPLE_TOTEM)) {
                    e.setCancelled(true);
                    if (data.totemTimer.timer != 0 || data.oppleTimer.timer != 0) {
                        p.sendMessage(getDangerComponent("cancelled (cooldown)"));
                        return;
                    }
                    else {
                        p.getWorld().playSound(p, Sound.ENTITY_PLAYER_BURP, 1f, 1f);
                        ItemStack is = p.getInventory().getItem(e.getHand());
                        is.setAmount(is.getAmount() - 1);
                        p.setFoodLevel(Math.min(p.getFoodLevel() + 4, 20));
                        p.setSaturation(Math.min(p.getSaturation() + 9.6F, 20F));
                        p.removePotionEffect(POISON);
                        p.removePotionEffect(SLOWNESS);
                        p.removePotionEffect(WEAKNESS);
                        p.removePotionEffect(SLOW_FALLING);
                        p.addPotionEffects(List.of(new PotionEffect(ABSORPTION, 2400, 3), new PotionEffect(PotionEffectType.REGENERATION, 600, 4), new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 6000, 0), new PotionEffect(RESISTANCE, 6000, 0)));
                        UUID uuid = data.uuid;
                        int sqliteId = data.sqliteId;
                        handleScoreboardTimer(data.oppleTimer, OPPLE_CD, data, uuid, sqliteId);
                        handleScoreboardTimer(data.totemTimer, TOTEM_CD, data, uuid, sqliteId);
                        p.setCooldown(Material.TOTEM_OF_UNDYING, TICK_TOTEM_CD);
                    }
                }
                else {
                    if (data.oppleTimer.timer == 0) {
                        handleScoreboardTimer(data.oppleTimer, OPPLE_CD, data, data.uuid, data.sqliteId);
                        data.oppleTimer.optionalSpamBuffer = CONSUMABLE_BUFFER;
                    }
                    else if (data.oppleTimer.optionalSpamBuffer > 0) {
                        handleScoreboardTimer(data.oppleTimer, OPPLE_CD, data, data.uuid, data.sqliteId);
                        handleScoreboardTimer(data.movementTimer, CONSUMABLE_BUFFER, data, data.uuid, data.sqliteId);
                        data.oppleTimer.optionalSpamBuffer = CONSUMABLE_BUFFER;
                    }
                    else {
                        e.setCancelled(true);
                        p.sendMessage(getDangerComponent("cancelled (cooldown)"));
                        return;
                    }
                }
            }
            case TOTEM_OF_UNDYING -> { // TODO impl eating totems
                if (handleTotemCdReturnValid(p, e, e.getItem())) {
                    p.clearActivePotionEffects();
                    doAbstractTotemConsume(p, 1, 0.6f, p.getWorld(), List.of(new PotionEffect(PotionEffectType.ABSORPTION, 900, 1), new PotionEffect(PotionEffectType.REGENERATION, 900, 1), new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 900, 0)));
                }
            }
        }
    }
    public static boolean handleTotemCdReturnValid(Player p, Cancellable e, ItemStack is) {
        PlayerData data = playerDataCache.get(p);
        ScoreboardTimer totemTimer = data.totemTimer;
        if (totemTimer.timer != 0) {
            e.setCancelled(true);
            p.sendMessage(getDangerComponent("cancelled (totem cd)"));
            return false;
        }
        else if (is.getItemMeta().getPersistentDataContainer().has(KEY_OPPLE_TOTEM)) {
            e.setCancelled(true);
            if (data.oppleTimer.timer != 0)
                p.sendMessage(getDangerComponent("cancelled (opple cd)"));
            else {
                if (e instanceof PlayerItemConsumeEvent)
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);

                doAbstractTotemConsume(p, 4, 9.6f, p.getWorld(), List.of(new PotionEffect(PotionEffectType.RESISTANCE, 0, 1000)));
                p.removePotionEffect(POISON);
                p.removePotionEffect(SLOWNESS);
                p.removePotionEffect(WEAKNESS);
                p.removePotionEffect(SLOW_FALLING);

                handleScoreboardTimer(data.oppleTimer, OPPLE_CD, data, data.uuid, data.sqliteId);
                p.setCooldown(Material.ENCHANTED_GOLDEN_APPLE, TICK_OPPLE_CD / 2);

                handleScoreboardTimer(totemTimer, TOTEM_CD, data, data.uuid, data.sqliteId);
                p.setCooldown(Material.TOTEM_OF_UNDYING, TICK_TOTEM_CD / 2);

                is.setAmount(is.getAmount() - 1);
            }
            return false;
        }
        else {
            handleScoreboardTimer(totemTimer, TOTEM_CD, data, data.uuid, data.sqliteId);
            p.setCooldown(Material.TOTEM_OF_UNDYING, TICK_TOTEM_CD / 2);
            return true;
        }
    }
    static void doAbstractTotemConsume(Player p, int hungerGain, float saturationGain, World world, List<PotionEffect> potionEffects) {
        p.setHealth(p.getHealth() + 1);
        p.setFoodLevel(Math.min(p.getFoodLevel() + hungerGain, 20));
        p.setSaturation(Math.min(p.getSaturation() + saturationGain, 20));
        p.playEffect(EntityEffect.PROTECTED_FROM_DEATH);
        world.playSound(p, Sound.ITEM_TOTEM_USE, 1L, 1L);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation(), 1);
        p.addPotionEffects(potionEffects);
    }
}
