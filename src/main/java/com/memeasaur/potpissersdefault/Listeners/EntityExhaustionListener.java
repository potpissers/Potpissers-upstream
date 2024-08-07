package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExhaustionEvent;

import static com.memeasaur.potpissersdefault.PotpissersDefault.playerDataCache;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.SPAWN_CLAIM;

public class EntityExhaustionListener implements Listener {
    @EventHandler
    void onPlayerHunger(EntityExhaustionEvent e) {
        Player p = (Player) e.getEntity();
        PlayerData data = playerDataCache.get(p);
        if (data == null)
            return; // TODO retarded
        if (data.currentClaim.equals(SPAWN_CLAIM)) {
            e.setCancelled(true);
            return;
        }
    }
}
