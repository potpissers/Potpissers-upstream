package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.*;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Methods.*;

public class ClaimListeners implements Listener {
    @EventHandler
    void onEntitiesUnloadListener(EntitiesUnloadEvent e) {
        for (Entity entity : e.getEntities()) {
            if (lootChestItems.contains(entity))
                entity.remove();
        }
    }

    @EventHandler
    void onItemSpawn (ItemSpawnEvent e) {
        Item item = e.getEntity();

        if (duelDeathItemStacks.getOrDefault(item.getItemStack(), null) instanceof DuelTracker duelTracker)
            handleDuelItemDrop(item, duelTracker);

        if (IS_KIT_SERVER)
            item.setTicksLived(FAST_ITEM_DESPAWN_TICKS_LIVED);
    }
    @EventHandler
    void onPlayerDropItem(PlayerDropItemEvent e) {
        if (playerDataCache.get(e.getPlayer()).playerFightTracker instanceof PlayerFightTracker playerFightTracker && playerFightTracker.statTracker instanceof DuelTracker duelTracker)
            handleDuelItemDrop(e.getItemDrop(), duelTracker);
    }
    void handleDuelItemDrop(Item item, DuelTracker duelTracker) {
        item.setVisibleByDefault(false);
        for (UUID uuidIteration : duelTracker.players.keySet())
            if (Bukkit.getPlayer(uuidIteration) instanceof Player player)
                player.showPlayer(plugin, player);

        duelEntities.put(item, duelTracker);
    }
    @EventHandler
    void onBlockExplosion(BlockExplodeEvent e) {
        handleClaimExplosion(e.blockList());
    }
    @EventHandler
    void onEntityExplosion(EntityExplodeEvent e) {
        handleClaimExplosion(e.blockList());
    }
    @EventHandler
    void onBlockBurn(BlockBurnEvent e) {
        Block block = e.getBlock();
        if (isUnbreakableClaim(getClaim(new ClaimCoordinate(block)))) {
            LocationCoordinate blockCoordinate = new LocationCoordinate(block);
            if (claimsBlocks.containsKey(blockCoordinate))
                claimsBlocks.get(blockCoordinate)[0] = CLAIM_BLOCK_TIMER;
            else if (EVENT_BREAKABLE_BLOCKS.contains(block.getType()))
                handleClaimBlockTask(block, CLAIM_BLOCK_TIMER, block.getType());
            else
                e.setCancelled(true);
        }
    }
    @EventHandler
    void onFireMove(BlockIgniteEvent e) {
        Block block = e.getBlock();
        if (isUnbreakableClaim(getClaim(new ClaimCoordinate(block)))
                && !claimsBlocks.containsKey(new LocationCoordinate(block)))
            e.setCancelled(true);
    }
    @EventHandler
    void onMobChangeBlock(EntityChangeBlockEvent e) {
        Block block = e.getBlock();
        if (isUnbreakableClaim(getClaim(new ClaimCoordinate(block)))) {
            LocationCoordinate blockCoordinate = new LocationCoordinate(block);
            if (claimsBlocks.containsKey(blockCoordinate))
                claimsBlocks.get(blockCoordinate)[0] = CLAIM_BLOCK_TIMER;
            else if (EVENT_BREAKABLE_BLOCKS.contains(block.getType()))
                handleClaimBlockTask(block, CLAIM_BLOCK_TIMER, block.getType());
            else
                e.setCancelled(true);
        }
    }
    @EventHandler
    void onPlayerBucketEmptyListener(PlayerBucketEmptyEvent e) {
        if (!e.getPlayer().getGameMode().isInvulnerable()) {
            Block block = e.getBlock();
            Object blockClaim = getClaim(new ClaimCoordinate(block));
            if (isUnbreakableClaim(blockClaim)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    void onPlayerBucketFillListener(PlayerBucketFillEvent e) {
        if (!e.getPlayer().getGameMode().isInvulnerable()) {
            Block block = e.getBlock();
            Object blockClaim = getClaim(new ClaimCoordinate(block));
            if (isUnbreakableClaim(blockClaim)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    void onMobSelect(EntityTargetLivingEntityEvent e) {
        if (e.getTarget() instanceof Player player && playerDataCache.get(player).currentClaim.equals(SPAWN_CLAIM))
            e.setCancelled(true);
    }

    void handleClaimExplosion(List<Block> blockList) {
        Iterator<Block> blockIterator = blockList.iterator();
        while (blockIterator.hasNext()) {
            Block block = blockIterator.next();
            if (isUnbreakableClaim(getClaim(new ClaimCoordinate(block)))) {
                LocationCoordinate blockCoordinate = new LocationCoordinate(block);
                if (claimsBlocks.containsKey(blockCoordinate))
                    claimsBlocks.get(blockCoordinate)[0] = CLAIM_BLOCK_TIMER;
                else if (EVENT_BREAKABLE_BLOCKS.contains(block.getType()))
                    handleClaimBlockTask(block, CLAIM_BLOCK_TIMER, block.getType());
                else // this drops items when it probably shouldn't TODO
                    blockIterator.remove();
            }
        }
    }
}
