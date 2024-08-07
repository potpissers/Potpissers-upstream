package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.ClaimCoordinate;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import static com.memeasaur.potpissersdefault.Util.Claim.Constants.MOBLESS_CLAIMS;
import static com.memeasaur.potpissersdefault.Util.Claim.Methods.getClaim;

public class CreatureSpawnListener implements Listener {
    @EventHandler
    void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.NATURAL)) {
            Location location = e.getEntity().getLocation();
            if (MOBLESS_CLAIMS.contains(getClaim(new ClaimCoordinate(location)))) {
                e.setCancelled(true);
                return;
            }
        }
    }
}
