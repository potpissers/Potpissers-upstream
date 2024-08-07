package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.ClaimCoordinate;
import com.memeasaur.potpissersdefault.Classes.LocationCoordinate;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Methods.*;

public class BlockBreakListener implements Listener {
    @EventHandler
    void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (!p.getGameMode().isInvulnerable()) {
            Block block = e.getBlock();
            Object blockClaim = getClaim(new ClaimCoordinate(block));
            if (block.getState() instanceof Chest chest && chest.getPersistentDataContainer().has(KEY_SUPPLY_DROP_LOCKED_CHEST)) {
                e.setCancelled(true);
                return;
            }
            if (isUnbreakableClaim(blockClaim)) {
                LocationCoordinate blockCoordinate = new LocationCoordinate(block);
                if (!claimsBlocks.containsKey(blockCoordinate)) {
                    Material blockType = block.getType();
                    if (EVENT_BREAKABLE_BLOCKS.contains(blockType)) {
                        e.setDropItems(false); // explosion still drops :/
                        handleClaimBlockTask(block, CLAIM_BLOCK_TIMER, block.getType());
                    } else {
                        e.setCancelled(true);
                        return;
                    }
                } else {
                    claimsBlocks.get(blockCoordinate)[0] = CLAIM_BLOCK_TIMER;
                }
            }
        }
    }
}
