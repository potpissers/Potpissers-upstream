package com.memeasaur.potpissersdefault.Commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import static com.memeasaur.potpissersdefault.Util.CubecoreSwords.Constants.*;
import static com.memeasaur.potpissersdefault.Util.Item.Constants.KEY_SERVER_COMPASS;
import static com.memeasaur.potpissersdefault.Util.Item.Methods.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.RANDOM;

public class CustomItemOpCommands implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof Player p) {
            switch (command.getName().toLowerCase()) {
                case "getservercompass" -> {
                    if (strings.length != 0)
                        p.sendMessage("?");

                    ItemStack compass = new ItemStack(RANDOM.nextBoolean() ? Material.COMPASS : Material.RECOVERY_COMPASS);
                    ItemMeta im = compass.getItemMeta();
                    im.getPersistentDataContainer().set(KEY_SERVER_COMPASS, PersistentDataType.BOOLEAN, Boolean.TRUE);
                    compass.setItemMeta(im); // TODO -> make server compass method call every time logging in so it's a different one

                    p.getInventory().addItem(compass);
                    return true;
                }
                case "getgrapple" -> {
                    if (strings.length != 0)
                        p.sendMessage("?");
                    p.getInventory().addItem(getGrapple(0));
                    return true;
                }
                case "getcubecoreswords" -> {
                    PlayerInventory pi = p.getInventory();
                    pi.addItem(doEventSword(Material.IRON_SWORD, Component.text(LEGENDARY_SWORD_AGNIS_RAGE).decoration(TextDecoration.ITALIC, false), null, LEGENDARY_SWORD_AGNIS_RAGE));
                    pi.addItem(doEventSword(Material.IRON_SWORD, Component.text(LEGENDARY_SWORD_VAMPYR).decoration(TextDecoration.ITALIC, false), null, LEGENDARY_SWORD_VAMPYR));
                    pi.addItem(doEventSword(Material.IRON_SWORD, Component.text(LEGENDARY_SWORD_LAST_STAND).decoration(TextDecoration.ITALIC, false), null, LEGENDARY_SWORD_LAST_STAND));
                    pi.addItem(doEventSword(Material.DIAMOND_SWORD, Component.text("Agni's sperg-out").decoration(TextDecoration.ITALIC, false), null, LEGENDARY_SWORD_AGNIS_RAGE));
                    pi.addItem(doEventSword(Material.IRON_SWORD, Component.text(LEGENDARY_SWORD_PHANTOM_BLADE).decoration(TextDecoration.ITALIC, false), null, LEGENDARY_SWORD_PHANTOM_BLADE));

                    pi.addItem(doEventSword(Material.IRON_SWORD, Component.text(LEGENDARY_SWORD_BERSERKER).decoration(TextDecoration.ITALIC, false), null, LEGENDARY_SWORD_BERSERKER));
                    pi.addItem(doEventSword(Material.IRON_SWORD, Component.text(LEGENDARY_SWORD_LONE_SWORD).decoration(TextDecoration.ITALIC, false), null, LEGENDARY_SWORD_LONE_SWORD));
                    pi.addItem(doEventSword(Material.IRON_SWORD, Component.text(LEGENDARY_SWORD_MURASAME).decoration(TextDecoration.ITALIC, false), null, LEGENDARY_SWORD_MURASAME));
                    pi.addItem(doEventSword(Material.IRON_SWORD, Component.text(LEGENDARY_SWORD_THERUMS_STRENGTH).decoration(TextDecoration.ITALIC, false), null, LEGENDARY_SWORD_THERUMS_STRENGTH));
                    pi.addItem(doEventSword(Material.IRON_SWORD, Component.text(LEGENDARY_SWORD_PLUVIAS_STORM).decoration(TextDecoration.ITALIC, false), null, LEGENDARY_SWORD_PLUVIAS_STORM));
                    pi.addItem(doEventSword(Material.IRON_SWORD, Component.text(LEGENDARY_SWORD_SIMOONS_SONG).decoration(TextDecoration.ITALIC, false), null, LEGENDARY_SWORD_SIMOONS_SONG));

                    pi.addItem(doEventSword(Material.BOW, Component.text(LEGENDARY_BOW_HEAL).decoration(TextDecoration.ITALIC, false), null, LEGENDARY_BOW_HEAL));
                    pi.addItem(doEventSword(Material.BOW, Component.text(LEGENDARY_BOW_PHANTOM_BOW).decoration(TextDecoration.ITALIC, false), null, LEGENDARY_BOW_PHANTOM_BOW));
                    pi.addItem(doEventSword(Material.BOW, Component.text(LEGENDARY_BOW_SIMOONS_MELODY).decoration(TextDecoration.ITALIC, false), null, LEGENDARY_BOW_SIMOONS_MELODY));
                    return true;
                }
            }
        }
        return false;
    }
}
