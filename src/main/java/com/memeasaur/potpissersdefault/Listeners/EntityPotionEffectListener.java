package com.memeasaur.potpissersdefault.Listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumSet;

import static org.bukkit.event.entity.EntityPotionEffectEvent.Cause.*;
import static org.bukkit.event.entity.EntityPotionEffectEvent.Cause.POTION_SPLASH;
import static org.bukkit.potion.PotionEffectType.SLOW_FALLING;

public class EntityPotionEffectListener implements Listener {
    private static final EnumSet<EntityPotionEffectEvent.Cause> PLAYER_DEBUFF_EFFECT_CAUSES = EnumSet.of(ARROW, AREA_EFFECT_CLOUD, POTION_SPLASH);
    @EventHandler
    void onPotionEffect (EntityPotionEffectEvent e) {
        if (e.getEntity() instanceof Player p && e.getNewEffect() instanceof PotionEffect potionEffect) {
            PotionEffectType potionEffectType = potionEffect.getType();
            EntityPotionEffectEvent.Cause cause = e.getCause();
            if (potionEffectType.equals(SLOW_FALLING) && PLAYER_DEBUFF_EFFECT_CAUSES.contains(cause)) { // TODO -> config this, vanilla combat with it might just be no-combo, which is fun
                e.setCancelled(true);
                p.addPotionEffect(new PotionEffect(SLOW_FALLING, potionEffect.getDuration() / 15, 0));
            }
        }
    }
}
