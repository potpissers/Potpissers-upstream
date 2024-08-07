package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.ScoreboardTimer;
import net.kyori.adventure.util.TriState;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Item.Constants.KEY_GRAPPLE;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.COMBAT_TAG;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.GRAPPLE_CD;
import static com.memeasaur.potpissersdefault.Util.Combat.Methods.handleAirborneSpammableMovementCd;
import static com.memeasaur.potpissersdefault.Util.Combat.Methods.handleCombatTag;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getDangerComponent;

public class PlayerFishListener implements Listener {
    @EventHandler
    void onFish(PlayerFishEvent e) {
        Player p = e.getPlayer();
        if (!e.getState().equals(org.bukkit.event.player.PlayerFishEvent.State.FISHING)) {
            FishHook hook = e.getHook();
            Entity pulledEntity = hook.getHookedEntity();
            if (pulledEntity != null)
                handleCombatTag(playerDataCache.get(p), COMBAT_TAG);
            if (p.getInventory().getItem(e.getHand()).getItemMeta().getPersistentDataContainer().has(KEY_GRAPPLE)) {
                if (pulledEntity != null) {
                    doGrapple(p, hook.getBoundingBox());
                    return;
                }
                float radius = 0.1255F; // .25x.25x.25 -> .26x.26x.26 for ceilings/walls
                World world = hook.getWorld();
                Block block;
                Block blockChangeCheck = null;
                for (double x = -radius; x <= radius; x += radius)
                    for (double y = -radius; y <= radius; y += radius)
                        for (double z = -radius; z <= radius; z += radius) {
                            block = world.getBlockAt(hook.getBoundingBox().getCenter().toLocation(world).add(x, y, z));
                            if (blockChangeCheck == null || block.getType() != blockChangeCheck.getType()) {
                                if (block.isSolid()) {
                                    ItemStack is = p.getInventory().getItem(e.getHand());
                                    Damageable im = (Damageable) is.getItemMeta();
                                    if (hook.isOnGround())
                                        im.setDamage(im.hasDamage() ? im.getDamage() + 3 : 3);
                                    else
                                        im.setDamage(im.hasDamage() ? im.getDamage() + 5 : 5);
                                    is.setItemMeta(im);

                                    doGrapple(p, hook.getBoundingBox());
                                    return;
                                }
                            }
                            blockChangeCheck = block;
                        }
            }
        } else {
            PersistentDataContainer pdc = p.getInventory().getItem(e.getHand()).getItemMeta().getPersistentDataContainer();
            if (pdc.has(KEY_GRAPPLE)) { // TODO make hashset to accompany this
                ScoreboardTimer movementTimer = playerDataCache.get(p).movementTimer;
                if (movementTimer.timer != 0 && movementTimer.optionalSpamBuffer == 0) {
                    e.setCancelled(true);
                    p.sendMessage(getDangerComponent("movement cd active"));
                    return;
                }
                else
                    grappleHooks.add(e.getHook());
            }
        }
    }
    void doGrapple(Player p, BoundingBox fishHookBoundingBox) {
        BoundingBox boundingBox = p.getBoundingBox();
        double hookMinY = fishHookBoundingBox.getMinY();
        double hookMaxY = fishHookBoundingBox.getMaxY();
        double pMinY = boundingBox.getMinY();
        double pMaxY = boundingBox.getMinY();
        final double finalHookY = hookMinY > pMaxY
                ? hookMinY
                : hookMaxY < pMinY
                ? hookMaxY
                : fishHookBoundingBox.getCenterY();
        final double finalPlayerY = pMinY > hookMaxY
                ? pMinY
                : pMaxY < hookMinY
                ? pMinY
                : boundingBox.getCenterY();

        p.setVelocity(p.getVelocity().add(new Vector(fishHookBoundingBox.getCenterX(), finalHookY, fishHookBoundingBox.getCenterZ()).subtract(new Vector(boundingBox.getCenterX(), finalPlayerY, boundingBox.getCenterZ()))).multiply(0.3F)); // TODO -> get closest point (?)
        p.getWorld().playSound(p, Sound.ENTITY_ZOMBIE_INFECT, 1F, 1F);
        // Movement cd start
        handleAirborneSpammableMovementCd(p, playerDataCache.get(p), GRAPPLE_CD);
        // Movement cd end
    }
}
