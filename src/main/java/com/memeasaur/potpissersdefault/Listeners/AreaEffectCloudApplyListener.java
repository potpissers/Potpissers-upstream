package com.memeasaur.potpissersdefault.Listeners;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;

import static com.memeasaur.potpissersdefault.PotpissersDefault.playerDataCache;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.SPAWN_CLAIM;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants.Potion.DEBUFF_EFFECTS;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.COMBAT_TAG;
import static com.memeasaur.potpissersdefault.Util.Combat.Methods.handleCombatTag;

public class AreaEffectCloudApplyListener implements Listener {
    @EventHandler
    void onLingeringTick(AreaEffectCloudApplyEvent e) {
        if (e.getEntity().getSource() instanceof ThrownPotion tp && tp.getShooter() instanceof Player p) {
            if (DEBUFF_EFFECTS.contains(tp.getPotionMeta().getBasePotionType())) {
                for (LivingEntity entity : e.getAffectedEntities())
                    if (entity instanceof Player p1) {
                        // Claims start
                        if (playerDataCache.get(p1).currentClaim.equals(SPAWN_CLAIM)) {
                            e.setCancelled(true);
                            return;
                        }
                        else
                            // Claims end
                            handleCombatTag(playerDataCache.get(p), COMBAT_TAG); // Claims
                    }
            }
        }
    }
}
