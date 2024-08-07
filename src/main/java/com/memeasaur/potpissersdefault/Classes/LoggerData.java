package com.memeasaur.potpissersdefault.Classes;

import org.bukkit.*;
import org.bukkit.entity.Piglin;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;

import static com.memeasaur.potpissersdefault.PotpissersDefault.plugin;
import static com.memeasaur.potpissersdefault.PotpissersDefault.SCHEDULER;

public class LoggerData {
    public static final NamespacedKey KEY_PIGLIN_LOGGER = new NamespacedKey("piglinlogger", "piglinlogger");
    static final int CONSUME_TIME = 32; // verify this TODO
    public static final List<PotionEffect> GAPPLE_EFFECT = List.of(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1), new PotionEffect(PotionEffectType.REGENERATION, 100, 2));
    public static final List<PotionEffect> OPPLE_EFFECT = List.of(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 3), new PotionEffect(PotionEffectType.REGENERATION, 400, 1), new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 6000, 0), new PotionEffect(PotionEffectType.RESISTANCE, 6000, 0));
    static final List<PotionEffect> TOTEM_EFFECT = List.of(new PotionEffect(PotionEffectType.ABSORPTION, 100, 2), new PotionEffect(PotionEffectType.REGENERATION, 900, 2), new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 1));

    public final Piglin piglin;
    public final PlayerData playerData;
    public final ItemStack[] playerInventory; // TODO this doesn't save to sqlite on every update. probably fine for now, the player gets the correct inventory when logging in to live logger, and it gets saved when the logger is gone
    public final float exp;
    public final Scoreboard scoreboard;
    public boolean eating = false;

    public LoggerData(Piglin piglin, PlayerData data, ItemStack[] pi, float exp, Scoreboard scoreboard) {
        this.piglin = piglin;
        this.playerData = data;
        this.playerInventory = pi;
        this.exp = exp;
        this.scoreboard = scoreboard;
    }
    public void doTotemLogger() {
        World world = piglin.getWorld(); // find vanilla particles + location TODO
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, piglin.getLocation(), 1);
        world.playSound(piglin, Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
        piglin.clearActivePotionEffects();
        piglin.addPotionEffects(TOTEM_EFFECT);
    }
    public void doAppleConsume(ItemStack is, List<PotionEffect> potionEffect) {
        EntityEquipment ee = piglin.getEquipment();
        ItemStack weapon = ee.getItemInMainHand();
        ee.setItemInMainHand(is);
        this.eating = true;
        SCHEDULER.runTaskLater(plugin, () -> {
            if (piglin.isValid()) {
                piglin.addPotionEffects(potionEffect);
                is.setAmount(is.getAmount() - 1);
                ee.setItemInMainHand(weapon);
                this.eating = false;
            }
        }, CONSUME_TIME);
    }
}
