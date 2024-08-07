package com.memeasaur.potpissersdefault.Util.Item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.OminousBottleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.memeasaur.potpissersdefault.Util.CubecoreSwords.Constants.KEY_EVENT_SWORD;
import static com.memeasaur.potpissersdefault.Util.CubecoreSwords.Constants.KEY_WIND_SURGE_CHARGE;
import static com.memeasaur.potpissersdefault.Util.Item.Constants.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.KEY_UNCLICKABLE_ITEM;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.RANDOM;

public class Methods {
    public static ItemStack getPotion(Material potionMaterial, PotionType potionType) {
        ItemStack potion = new ItemStack(potionMaterial);
        PotionMeta potionMeta = ((PotionMeta)potion.getItemMeta());
        potionMeta.setBasePotionType(potionType);
        potion.setItemMeta(potionMeta);
        return potion;
    }
    public static ItemStack getTippedArrow(int amount, PotionType potionType) {
        ItemStack potion = new ItemStack(Material.TIPPED_ARROW, amount);
        PotionMeta potionMeta = ((PotionMeta)potion.getItemMeta());
        potionMeta.setBasePotionType(potionType);
        potion.setItemMeta(potionMeta);
        return potion;
    }
    public static ItemStack getCustomPotion(Material potionCategory, String potionName, Color potionColor, PotionEffect potionEffect) {
        ItemStack potion = new ItemStack(potionCategory);
        PotionMeta potionMeta = ((PotionMeta)potion.getItemMeta());
        potionMeta.displayName(Component.text(potionName).decoration(TextDecoration.ITALIC, false));
        potionMeta.setColor(potionColor);
//        potionMeta.lore(List.of(Component.text("Food and Drinks").decoration(TextDecoration.ITALIC, false)));
        potionMeta.addCustomEffect(potionEffect, true);
        potion.setItemMeta(potionMeta);
        return potion;
    }
    public static ItemStack getEnchantedDamageableItemStack(Material material, @Nullable Map<Enchantment, Integer> enchantments, int damageDivisor) {
        ItemStack itemStack = new ItemStack(material);
        if (enchantments != null)
            itemStack.addUnsafeEnchantments(enchantments);
        if (damageDivisor != 0) {
            Damageable damageable = (Damageable) itemStack.getItemMeta();
            short maxDurability = material.getMaxDurability();
            damageable.setDamage(maxDurability - (maxDurability / damageDivisor));
            itemStack.setItemMeta(damageable);
        }
        return itemStack;
    }
    public static ItemStack getStackedPotion(Material potionCategory, int potionAmount, PotionType potionType) {
        ItemStack potion = new ItemStack(potionCategory, potionAmount);
        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
        potionMeta.setBasePotionType(potionType);
        potion.setItemMeta(potionMeta);
        return potion;
    }
    public static ItemStack getEnchantedBook(Enchantment enchantment) {
        ItemStack enchantedBook = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta enchantmentStorageMeta = (EnchantmentStorageMeta) enchantedBook.getItemMeta();
        enchantmentStorageMeta.addStoredEnchant(enchantment, 1, false);
        enchantedBook.setItemMeta(enchantmentStorageMeta);
        return enchantedBook;
    }
    public static ItemStack getOminousBottle() {
        ItemStack ominousBottle = new ItemStack(Material.OMINOUS_BOTTLE);
        OminousBottleMeta ominousBottleMeta = (OminousBottleMeta) ominousBottle.getItemMeta();
        ominousBottleMeta.setAmplifier(4);
        ominousBottle.setItemMeta(ominousBottleMeta);
        return ominousBottle;
    }
    public static ItemStack getFireworkRocket(int power) {
        ItemStack fireworkRocket = new ItemStack(Material.FIREWORK_ROCKET);
        FireworkMeta fireworkMeta = (FireworkMeta) fireworkRocket.getItemMeta();
        fireworkMeta.setPower(power);
        fireworkRocket.setItemMeta(fireworkMeta);
        return fireworkRocket;
    }

    // Grapple start
    public static ItemStack getGrapple(int damage) {
        ItemStack grapple = new ItemStack(Material.FISHING_ROD);
        ItemMeta im = grapple.getItemMeta();
        im.lore(List.of(Component.text(GRAPPLE_LORE.get(new Random().nextInt(GRAPPLE_LORE.size())))));
        im.displayName(Component.text("Grapple").decoration(TextDecoration.ITALIC, false));
        im.setRarity(ItemRarity.RARE);
        im.getPersistentDataContainer().set(KEY_GRAPPLE, PersistentDataType.BOOLEAN, Boolean.TRUE);
        if (damage != 0)
            ((Damageable)im).setDamage(damage);
        grapple.setItemMeta(im);
        return grapple;
    }
    // Grapple end

    // Cubecore swords start
    public static ItemStack doEventSword(Material weapon, Component name, @Nullable Map<Enchantment, Integer> enchantments, String type) {
        ItemStack eventSword = new ItemStack(weapon);
        if (enchantments != null) eventSword.addEnchantments(enchantments);
        ItemMeta im = eventSword.getItemMeta();
        im.displayName(name);
        im.getPersistentDataContainer().set(KEY_EVENT_SWORD, PersistentDataType.STRING, type);
        eventSword.setItemMeta(im);
        return eventSword;
    }

    public static ItemStack getWindSurgeCharge() {
        ItemStack windSurgeCharge = new ItemStack(Material.WIND_CHARGE);
        ItemMeta im = windSurgeCharge.getItemMeta();
        im.displayName(Component.text("Wind Surge Charge").decoration(TextDecoration.ITALIC, false));
        im.getPersistentDataContainer().set(KEY_WIND_SURGE_CHARGE, PersistentDataType.BOOLEAN, Boolean.TRUE);
        im.setEnchantmentGlintOverride(true);
        windSurgeCharge.setItemMeta(im);
        return windSurgeCharge;
    }
    // Cubecore swords end

    // SpawnCannon start
    public static ItemStack getSpawnCannonElytra() {
        ItemStack spawnElytra = new ItemStack(Material.ELYTRA);
        spawnElytra.addEnchantment(Enchantment.BINDING_CURSE, 1);
        ItemMeta im = spawnElytra.getItemMeta();
        im.getPersistentDataContainer().set(KEY_UNCLICKABLE_ITEM, PersistentDataType.BOOLEAN, Boolean.TRUE);
        spawnElytra.setItemMeta(im);
        return spawnElytra;
    }
    // SpawnCannon end
}
