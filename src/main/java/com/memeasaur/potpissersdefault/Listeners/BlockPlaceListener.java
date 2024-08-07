package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.ClaimCoordinate;
import com.memeasaur.potpissersdefault.Classes.LocationCoordinate;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Methods.*;

public class BlockPlaceListener implements Listener {
    @EventHandler
    void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (!p.getGameMode().isInvulnerable()) {
            Block block = e.getBlockPlaced();
            Object blockClaim = getClaim(new ClaimCoordinate(block));
            if (isUnbreakableClaim(blockClaim)) {
                Material blockType = block.getType();
                int timerDuration = CLAIM_BLOCK_TIMER;
                if (blockClaim.equals(SPAWN_CLAIM)) {
                    if (!SHULKER_BOXES.contains(blockType)) {
                        e.setCancelled(true);
                        return;
                    } else
                        handleClaimBlockTask(block, timerDuration, Material.AIR);
                } else {
                    if (!EVENT_PLACEABLE_BLOCKS.contains(blockType) &&
                            (!EVENT_NON_COMBAT_PLACEABLE_BLOCKS.contains(blockType) || playerDataCache.get(p).combatTag != 0)) {
                        e.setCancelled(true);
                        return;
                    } else if (claimsBlocks.containsKey(new LocationCoordinate(block)))
                        claimsBlocks.get(new LocationCoordinate(block))[0] = timerDuration;
                    else
                        handleClaimBlockTask(block, timerDuration, Material.AIR);
                }
            }

        }
    }
}
