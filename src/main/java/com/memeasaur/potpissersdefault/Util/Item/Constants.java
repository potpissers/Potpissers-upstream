package com.memeasaur.potpissersdefault.Util.Item;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.KEY_UNCLICKABLE_ITEM;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getNormalComponent;

public class Constants {
    public static final NamespacedKey KEY_SERVER_COMPASS = new NamespacedKey("servercompasskey", "servercompasskey");
    public static final NamespacedKey KEY_SERVER_COMPASS_ITEM = new NamespacedKey("servercompassitemkey", "servercompassitemkey");
    public static final Inventory SERVER_COMPASS_INVENTORY = Bukkit.createInventory(null, 9, Component.text("/servers"));
    static {
        SERVER_COMPASS_INVENTORY.setItem(2, getServerCompassItemStack(Material.NETHERITE_SWORD, "cubecore"));
        SERVER_COMPASS_INVENTORY.setItem(3, getServerCompassItemStack(Material.DIAMOND, "hcf"));

        SERVER_COMPASS_INVENTORY.setItem(5, getServerCompassItemStack(Material.ZOMBIE_HEAD, "mz"));
        SERVER_COMPASS_INVENTORY.setItem(6, getServerCompassItemStack(Material.IRON_SWORD, "kollusion"));
    }
    static ItemStack getServerCompassItemStack(Material material, String serverName) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.itemName(getNormalComponent("/" + serverName));
        itemMeta.getPersistentDataContainer().set(KEY_UNCLICKABLE_ITEM, PersistentDataType.BOOLEAN, Boolean.TRUE);
        itemMeta.getPersistentDataContainer().set(KEY_SERVER_COMPASS_ITEM, PersistentDataType.STRING, serverName);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public static final NamespacedKey KEY_GRAPPLE = new NamespacedKey("grapplekey", "grapplekey");
    public static final List<String> GRAPPLE_LORE = List.of("wow this grapple is very smooth"
            , "use this to die of fall damage"
            , "watch out for that block"
            , "imagine being good at grappling"
            , "users pushing 30 get bonus velocity"
            , "the fishing rod grapple is my invention"
            , "a genius coded this"
            , "a retard coded this"
            , "if this is bugged blame chatgpt");
}
