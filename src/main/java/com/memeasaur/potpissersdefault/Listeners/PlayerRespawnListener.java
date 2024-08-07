package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.ClaimCoordinate;
import com.memeasaur.potpissersdefault.Classes.DuelTracker;
import com.memeasaur.potpissersdefault.Classes.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Methods.getClaim;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods1.executeDeathbanCheck;

public class PlayerRespawnListener implements Listener {
    @EventHandler
    void onPlayerRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        PlayerData data = playerDataCache.get(p);
        if (e.getRespawnReason().equals(PlayerRespawnEvent.RespawnReason.DEATH) && (data.playerFightTracker == null || !(data.playerFightTracker.statTracker instanceof DuelTracker))) {
            e.setRespawnLocation(Bukkit.getWorld("world").getSpawnLocation());

            // Deathbanz start
            executeDeathbanCheck(data.uuid, p, data.hostAddress);
//            if (deathBanCache.getOrDefault(data.uuid, null) instanceof PunishmentData punishmentData && ZonedDateTime.now().isBefore(punishmentData.expirationDate())) {
//                p.kick(Component.text("death-ban: " + Duration.between(ZonedDateTime.now(), punishmentData.expirationDate()).toMinutes() + " minutes remaining"));
//                return; // TODO -> test this
//            }
            // Deathbanz end

            // Spawn kit start
            if (hubKit != null)
                p.getInventory().setContents(hubKit);
            // Spawn kit end
        }
        data.currentClaim = getClaim(new ClaimCoordinate(e.getRespawnLocation()));
    }
}
