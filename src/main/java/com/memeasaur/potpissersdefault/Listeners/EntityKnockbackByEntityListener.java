package com.memeasaur.potpissersdefault.Listeners;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import static com.memeasaur.potpissersdefault.PotpissersDefault.plugin;
import static com.memeasaur.potpissersdefault.Util.CubecoreSwords.Constants.*;

public class EntityKnockbackByEntityListener implements Listener {
    @EventHandler
    void onKnockedBack(EntityKnockbackByEntityEvent e) {
        if (e.getHitBy() instanceof LivingEntity livingEntity) {
            EntityEquipment entityEquipment = livingEntity.getEquipment();
            if (entityEquipment != null && CUBECORE_SWORD_MELEE_KNOCKBACK_CAUSES.contains(e.getCause())) {
                ItemStack weapon = entityEquipment.getItemInMainHand();
                if (!weapon.getType().isAir() && weapon.getPersistentDataContainer().has(KEY_EVENT_SWORD)) {
                    String eventSwordType = weapon.getPersistentDataContainer().getOrDefault(KEY_EVENT_SWORD, PersistentDataType.STRING, "null");
                    switch (eventSwordType) {
                        case LEGENDARY_SWORD_SIMOONS_SONG -> {
                            e.setKnockback(e.getKnockback().multiply(6));
                            new BukkitRunnable() {
                                final LivingEntity livingEntity1 = e.getEntity();
                                final double originalY = livingEntity1.getY();
                                @Override
                                public void run() {
                                    if (livingEntity1.getY() >= originalY && !livingEntity1.isOnGround())
                                        livingEntity1.addPotionEffect(SIMOONS_SONG_SLOW_FALLING);
                                    else {
                                        cancel();
                                        return;
                                    }
                                }
                            }.runTaskTimer(plugin, 1L, 1L);
                        }
                    }
                }
            }
        }
    }
}
