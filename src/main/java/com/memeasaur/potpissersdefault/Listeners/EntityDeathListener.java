package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.LoggerData;
import com.memeasaur.potpissersdefault.Classes.LoggerUpdate;
import com.memeasaur.potpissersdefault.Classes.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static com.memeasaur.potpissersdefault.Classes.AbstractPartyData.executeNetworkPartyMessage;
import static com.memeasaur.potpissersdefault.Classes.AbstractPartyData.fetchNonnullPartyName;
import static com.memeasaur.potpissersdefault.Classes.LoggerData.KEY_PIGLIN_LOGGER;
import static com.memeasaur.potpissersdefault.Listeners.PlayerDeathListener.handlePlayerDeath;
import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.TOTEM_CD;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getFocusComponent;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getNormalComponent;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Timer.handleScoreboardTimer;

public class EntityDeathListener implements Listener {
    @EventHandler
    void onEntityDeath(EntityDeathEvent e) {
        if (e.getEntity() instanceof Piglin piglin && piglin.getPersistentDataContainer().has(KEY_PIGLIN_LOGGER)) {
            e.getDrops().clear();
            e.setDroppedExp(0);
            if (loggerDataCache.getOrDefault(piglin, null) instanceof LoggerData loggerData) {
                for (ItemStack is : loggerData.playerInventory)
                    if (is != null && is.getType().equals(Material.TOTEM_OF_UNDYING)) {
                        loggerData.doTotemLogger();
                        e.setCancelled(true); // TODO cancel eat?
                        e.setReviveHealth(1);
                        is.setAmount(is.getAmount() - 1);
                        PlayerData data = loggerData.playerData;
                        handleScoreboardTimer(data.totemTimer, TOTEM_CD, data, data.uuid, data.sqliteId);
                        Location location = piglin.getLocation();
                        data.getCurrentParties().forEach(party ->
                                executeNetworkPartyMessage(party.uuid, fetchNonnullPartyName(party.uuid), getFocusComponent(piglin.getName() + ": just used my totem at " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ())));
                        return;
                    }
                e.setDroppedExp((int) loggerData.exp);
                Collections.addAll(e.getDrops(), loggerData.playerInventory);
                loggerDataCache.remove(piglin);

                // todo check piglin logger player data saving, it should already be doing it, though
                new LoggerUpdate(0D, piglin.getLocation(), new ItemStack[0]).handleLoggerUpdateData(loggerData.playerData, piglin);
                handlePlayerDeath(e, loggerData.playerData, getNormalComponent(piglin.getName() + "'s logger has been slain"), loggerData.playerInventory);
                // TODO -> get the piglin death message, wherever that is
            }
        }
    }

}
