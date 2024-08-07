package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.LocationCoordinate;
import com.memeasaur.potpissersdefault.Classes.PlayerData;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

import static com.memeasaur.potpissersdefault.Listeners.PlayerItemConsumeListener.handleTotemCdReturnValid;
import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.PotpissersDefault.lootChestsCache;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.KEY_SUPPLY_DROP_CHEST;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods1.handleLootChestBreak;

public class PotpissersListeners implements Listener {
    @EventHandler
    void onInventoryOpenListener(InventoryOpenEvent e) {
        if (e.getInventory().getHolder() instanceof Chest chest) {
            Map.Entry<Integer, PlayerData> nullableSupplyDropChestData = chest.getPersistentDataContainer().get(KEY_SUPPLY_DROP_CHEST, PersistentDataType.INTEGER) instanceof Integer id ? Map.entry(id, playerDataCache.get((Player)e.getPlayer())) : null;
            if ((lootChestsCache.containsKey(new LocationCoordinate(chest.getLocation())) || nullableSupplyDropChestData != null)
                    && !openedLootChests.contains(chest)) {
                openedLootChests.add(chest);
                new BukkitRunnable() {
                    final Location location = chest.getLocation();
                    final Inventory inventory = chest.getBlockInventory();

                    @Override
                    public void run() {
                        if (!openedLootChests.contains(chest)) { // breaking chest manually removes this, it was causing double loot-fills
                            cancel();
                            return;
                        } else if (inventory.isEmpty() || location.getNearbyPlayers(4).isEmpty()) {
                            handleLootChestBreak(chest.getBlock(), chest, nullableSupplyDropChestData);
                            cancel();
                            return;
                        }
                    }
                }.runTaskTimer(plugin, 0L, 2L);
            }
        }
    }
    @EventHandler
    void onTotemRevive (EntityResurrectEvent e) {
        if (e.getEntity() instanceof Player p && !e.isCancelled() && !handleTotemCdReturnValid(p, e, p.getInventory().getItem(e.getHand())))
            return;
    }
}
