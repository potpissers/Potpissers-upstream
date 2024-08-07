package com.memeasaur.potpissersdefault.Listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import static com.memeasaur.potpissersdefault.Util.Item.Constants.KEY_GRAPPLE;

public class PrepareItemCraftListener implements Listener {
    // Grapple repair start
    @EventHandler
    void onCraftItemListener(PrepareItemCraftEvent e) {
        if (e.isRepair()) {
            CraftingInventory inventory = e.getInventory();
            ItemStack result = inventory.getResult();
            if (result != null) {
                switch (result.getType()) {
                    case FISHING_ROD -> {
                        ItemStack[] matrix = inventory.getMatrix();
                        ItemMeta grappleMeta = null;
                        for (ItemStack itemStack : matrix) {
                            if (itemStack != null) {
                                if (itemStack.getPersistentDataContainer().has(KEY_GRAPPLE)) {
                                    grappleMeta = itemStack.getItemMeta();
                                    continue;
                                }
                                else {
                                    grappleMeta = null;
                                    break;
                                }
                            }
                        }
                        if (grappleMeta != null) {
                            ItemMeta itemMeta = result.getItemMeta();
                            itemMeta.getPersistentDataContainer().set(KEY_GRAPPLE, PersistentDataType.BOOLEAN, Boolean.TRUE);
                            itemMeta.displayName(grappleMeta.displayName());
                            itemMeta.lore(grappleMeta.lore());
                            itemMeta.setRarity(grappleMeta.getRarity());
                            result.setItemMeta(itemMeta);
                            inventory.setResult(result);
                        }
                    }
                }
            }
        }
    }
    // Grapple repair end
}
