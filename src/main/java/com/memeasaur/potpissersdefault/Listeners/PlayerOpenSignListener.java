package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.ClaimCoordinate;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;

import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.KEY_CUBECORE_SIGN;
import static com.memeasaur.potpissersdefault.Util.Claim.Methods.getClaim;
import static com.memeasaur.potpissersdefault.Util.Claim.Methods.isUnbreakableClaim;

public class PlayerOpenSignListener implements Listener {
    @EventHandler
    void onSignInteract(PlayerOpenSignEvent e) {
        Player p = e.getPlayer();
        Sign sign = e.getSign();
        // Cubecore sign start
        if (sign.getPersistentDataContainer().get(KEY_CUBECORE_SIGN, PersistentDataType.STRING) instanceof String command) {
            e.setCancelled(true);
            p.performCommand(command);
            return;
        }
        // Cubecore sign end
        if (!p.getGameMode().isInvulnerable()) {
            if (isUnbreakableClaim(getClaim(new ClaimCoordinate(sign.getBlock())))) {
                e.setCancelled(true);
                return;
            }
        }
    }
}
