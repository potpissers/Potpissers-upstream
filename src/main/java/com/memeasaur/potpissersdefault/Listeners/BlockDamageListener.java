package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.LocationCoordinate;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;

import static com.memeasaur.potpissersdefault.PotpissersDefault.claimsBlocks;

public class BlockDamageListener implements Listener {
    @EventHandler
    void onBlockDamage(BlockDamageEvent e) {
        Block block = e.getBlock();
        switch (block.getType()) {
            case RED_SHULKER_BOX,
                 SHULKER_BOX,
                 BLUE_SHULKER_BOX,
                 BLACK_SHULKER_BOX,
                 YELLOW_SHULKER_BOX,
                 BROWN_SHULKER_BOX,
                 LIME_SHULKER_BOX,
                 GRAY_SHULKER_BOX,
                 LIGHT_GRAY_SHULKER_BOX,
                 MAGENTA_SHULKER_BOX,
                 GREEN_SHULKER_BOX,
                 WHITE_SHULKER_BOX,
                 ORANGE_SHULKER_BOX,
                 PINK_SHULKER_BOX,
                 PURPLE_SHULKER_BOX,
                 CYAN_SHULKER_BOX,
                 LIGHT_BLUE_SHULKER_BOX -> {
                if (claimsBlocks.containsKey(new LocationCoordinate(block)))
                    e.setInstaBreak(true); // if owner, no instant break TODO
            }
        }
    }
}
