package com.memeasaur.potpissersdefault.Listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionType;

import static com.memeasaur.potpissersdefault.PotpissersDefault.headSplashPotions;
import static com.memeasaur.potpissersdefault.PotpissersDefault.playerDataCache;
import static com.memeasaur.potpissersdefault.PotpissersDefault.duelEntities;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants.Potion.DEBUFF_EFFECTS;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants.Potion.NOT_NERFED_SPLASH_EFFECTS;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.COMBAT_TAG;
import static com.memeasaur.potpissersdefault.Util.Combat.Methods.handleCombatTag;

public class PotionSplashListener implements Listener {
    @EventHandler
    void onSplashPotion (PotionSplashEvent e) {
        if (e.getPotion().getShooter() instanceof Player p) {
            ThrownPotion thrownPotion = e.getPotion();
            PotionType potionType = thrownPotion.getPotionMeta().getBasePotionType();
            if (!NOT_NERFED_SPLASH_EFFECTS.contains(potionType) && !headSplashPotions.contains(thrownPotion)) {
                for (Entity entity : e.getAffectedEntities()) {
                    if (entity == p) {
                        e.setIntensity(p, e.getIntensity(p) * .85);
                    }
                }
            }
            // Combat tag start
            if (DEBUFF_EFFECTS.contains(potionType) && e.getAffectedEntities().stream().anyMatch(entity -> entity instanceof Player p1 && e.getIntensity(p1) > 0 && p != p1))
                handleCombatTag(playerDataCache.get(p), COMBAT_TAG);
            // Combat tag end

            // Duels start
            if (duelEntities.containsKey(e.getPotion())) {
                for (LivingEntity entity : e.getAffectedEntities())
                    if (entity instanceof Player player && !player.canSee(e.getPotion()))
                        e.setIntensity(player, 0);
            }
            // Duels end
        }
    }
}
