package com.memeasaur.potpissersdefault.Listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;

public class ProjectileHitListener implements Listener {
    @EventHandler
    void onProjectileHit(ProjectileHitEvent e) {
        if (e.getEntity().getShooter() instanceof Player p) {
            switch (e.getEntity().getType()) {
                case FISHING_BOBBER -> {
                    FishHook fh = (FishHook) e.getEntity();
                    if (grappleHooks.contains(fh) && e.getHitBlockFace() != null)
                        fh.setNoPhysics(true);
                    // if rogue go through teammate thanks kayla // TODO
                    if (e.getHitEntity() instanceof Player p1 && playerDataCache.get(p1).combatTag != 0) {
                        p1.hideEntity(plugin, fh);
                        p1.sendActionBar(Component.text("hiding fish hook"));
                    }
                }
                case ENDER_PEARL -> {
                    EnderPearl enderPearl = (EnderPearl) e.getEntity();
                    if (enderPearl.getShooter() instanceof Piglin piglin && loggerDataCache.containsKey(piglin))
                        piglin.damage(5);
                }
            }
        }
    }
}
