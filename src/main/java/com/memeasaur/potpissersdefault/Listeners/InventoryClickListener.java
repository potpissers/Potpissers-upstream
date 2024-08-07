package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.PlayerData;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;

import static com.memeasaur.potpissersdefault.Commands.PotpissersCommands.getShulkerTimer;
import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.KEY_CUBECORE_CHEST;
import static com.memeasaur.potpissersdefault.Util.Item.Constants.KEY_SERVER_COMPASS_ITEM;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.KEY_UNCLICKABLE_ITEM;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getDangerComponent;

public class InventoryClickListener implements Listener {
    @EventHandler
    void onInventoryClick(InventoryClickEvent e) {
        HumanEntity p = e.getWhoClicked();
        if (!p.getGameMode().isInvulnerable()) {
            if (e.getClickedInventory() instanceof Inventory clickInv) {
                ItemStack is = e.getCurrentItem();
                if (is != null) {
                    // UnclickableItems start
                    if (is.getPersistentDataContainer().has(KEY_UNCLICKABLE_ITEM)) {
                        e.setCancelled(true);

                        if (is.getPersistentDataContainer().get(KEY_SERVER_COMPASS_ITEM, PersistentDataType.STRING) instanceof String serverName)
                            ((Player)p).sendPluginMessage(plugin, "potpissers:serverswitcher", serverName.getBytes(StandardCharsets.UTF_8));

                        return;
                    }
                    // UnclickableItems end
                    switch (clickInv.getType()) {
                        case PLAYER -> {
                            if (e.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)
                                    && (e.getInventory().getHolder() instanceof DoubleChest doubleChest
                                    && (((Chest) doubleChest.getLeftSide()).getPersistentDataContainer().has(KEY_CUBECORE_CHEST) || ((Chest) doubleChest.getRightSide()).getPersistentDataContainer().has(KEY_CUBECORE_CHEST))
                                    || (e.getInventory().getHolder() instanceof Chest chest && chest.getPersistentDataContainer().has(KEY_CUBECORE_CHEST)))) {
                                e.setCancelled(true);
                                return;
                            }
                        }
                        case CHEST -> {
                            if (e.getInventory().getHolder() instanceof DoubleChest doubleChest
                                    && (((Chest) doubleChest.getLeftSide()).getPersistentDataContainer().has(KEY_CUBECORE_CHEST) || ((Chest) doubleChest.getRightSide()).getPersistentDataContainer().has(KEY_CUBECORE_CHEST))
                                    || (e.getInventory().getHolder() instanceof Chest chest && chest.getPersistentDataContainer().has(KEY_CUBECORE_CHEST))) {
                                e.setCancelled(true);
                                p.getInventory().addItem(is);
                                return;
                            }
                        }
                        // Shulker cd start
                        case SHULKER_BOX, ENDER_CHEST -> {
                            PlayerData data = playerDataCache.get((Player) e.getWhoClicked());
                            if (data.shulkerCd < getShulkerTimer(data)) {
                                e.setCancelled(true);
                                e.getWhoClicked().sendMessage(getDangerComponent("cancelled (shulker access)"));
                            }
                        }
                        // Shulker cd end
                    }
                }
            }
        }
    }
}
