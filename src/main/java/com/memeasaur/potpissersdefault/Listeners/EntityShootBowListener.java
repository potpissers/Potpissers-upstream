package com.memeasaur.potpissersdefault.Listeners;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import static com.memeasaur.potpissersdefault.Listeners.PlayerInteractListener.doEnchantmentUpdate;
import static com.memeasaur.potpissersdefault.PotpissersDefault.plugin;
import static com.memeasaur.potpissersdefault.PotpissersDefault.powerLimit;
import static org.bukkit.enchantments.Enchantment.POWER;

public class EntityShootBowListener implements Listener {
    @EventHandler
    void onShootBowCubecore(EntityShootBowEvent e) {
        if (e.getEntity() instanceof Player p) {
            if (e.getBow() instanceof ItemStack is && is.getEnchantmentLevel(Enchantment.POWER) > powerLimit) {
                doEnchantmentUpdate(is, POWER, powerLimit, p, e);
                e.setCancelled(true);
                return;
            }
            switch (e.getProjectile().getType()) {
                case ARROW, SPECTRAL_ARROW -> {
                    if (e.getForce() < 0.4) {
                        AbstractArrow aa = (AbstractArrow) e.getProjectile();
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (!aa.isValid() || aa.isInBlock())
                                    cancel();
                                else if (aa.getBoundingBox().overlaps(p.getBoundingBox())) {
                                    aa.hitEntity(p);
                                    cancel();
                                }
                            }
                        }.runTaskTimer(plugin, 3L, 1L);
                    }
                }
            }
//            if (aa instanceof Arrow arrow
//                    && (!isLowForce || debuffEffects.contains(arrow.getBasePotionType()))) {
//                handleTippedArrow(arrow, data, p, e, plugin);
//            }
//            handleKnockbackArrow(aa, data, e, p, plugin);
        }
    }
}
