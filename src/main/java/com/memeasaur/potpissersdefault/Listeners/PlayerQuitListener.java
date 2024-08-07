package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.LoggerData;
import com.memeasaur.potpissersdefault.Classes.LoggerUpdate;
import com.memeasaur.potpissersdefault.Classes.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static com.memeasaur.potpissersdefault.Classes.LoggerData.KEY_PIGLIN_LOGGER;
import static com.memeasaur.potpissersdefault.Commands.PotpissersCommands.getLogoutTeleportTimer;
import static com.memeasaur.potpissersdefault.Commands.PotpissersCommands.handleLogoutTeleport;
import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Sqlite.Methods.fetchUpdatePlayerDataIntVoid;

public class PlayerQuitListener implements Listener {
    @EventHandler
    void onPlayerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        PlayerData data = playerDataCache.get(p);
        p.getAttribute(Attribute.ARMOR).setBaseValue(0);
        if (data == null)
            return;

        if (!p.getGameMode().isInvulnerable() && data.logoutTeleportTimer < getLogoutTeleportTimer(data)) { // Logout, Claims)
            Location location = p.getLocation();
            Piglin piglinLogger = (Piglin) location.getWorld().spawnEntity(location, EntityType.PIGLIN);
            location.getNearbyEntitiesByType(Mob.class, 16)
                    .forEach(entity -> {
                        if (p.equals(entity.getTarget()))
                            entity.setTarget(piglinLogger);
                    });

            PlayerInventory pi = p.getInventory();

            getPlayerEntity(piglinLogger, p, p.getEnderPearls(), p.getEquipment());
            getPlayerWeapon(pi, piglinLogger.getEquipment());
            piglinLogger.getPersistentDataContainer().set(KEY_PIGLIN_LOGGER, PersistentDataType.BOOLEAN, Boolean.TRUE);

            piglinLogger.setAdult();
            piglinLogger.setImmuneToZombification(true);

            piglinLogger.setHealth(p.getHealth());

            if (piglinLogger.getFallDistance() != 0) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (piglinLogger.isValid()) { // doesn't handle getting hit before hitting the ground perfectly
                            if (piglinLogger.getFallDistance() == 0) {
                                piglinLogger.setAI(false);
                                piglinLogger.setDancing(true);
                            }
                        } else
                            cancel();
                    }
                }.runTaskTimer(plugin, 1L, 1L);
            } else {
                piglinLogger.setAI(false);
                piglinLogger.setDancing(true);
            }
            // TODO -> without the logout-task, this piglin will despawn since it's chunks aren't being force-loaded
            LoggerData loggerData = new LoggerData(piglinLogger, data, pi.getContents(), p.getExp(), p.getScoreboard());
            loggerDataCache.put(piglinLogger, loggerData);
            playerLoggerCache.put(data.uuid, piglinLogger);
            pi.clear();
            p.clearActivePotionEffects();

            // Logout + tag start
            handleLogoutTeleport(p, data, data.logoutTeleportString, data.logoutTeleportLocation);

            piglinLogger.getChunk().setForceLoaded(true);

            new BukkitRunnable() {
                Chunk currentChunk = piglinLogger.getChunk();
                CompletableFuture<Void> saveLogoutTimerQuery = fetchUpdatePlayerDataIntVoid("logout_teleport_timer", data.logoutTeleportTimer, data.sqliteId);
                @Override
                public void run() {
                    if (piglinLogger.isDead()) {
                        currentChunk.setForceLoaded(false);
                        cancel();
                        return;
                    }
                    else {
                        if (piglinLogger.getTarget() == null && piglinLogger.isOnGround()) {
                            piglinLogger.setDancing(true);
                            SCHEDULER.runTaskLater(plugin, () -> {
                                if (piglinLogger.getFallDistance() == 0)
                                    piglinLogger.setAI(false);
                            }, 1L);
                        }

                        if (!piglinLogger.getChunk().equals(currentChunk)) {
                            currentChunk.setForceLoaded(false);
                            currentChunk = piglinLogger.getChunk();
                            currentChunk.setForceLoaded(true);
                        }

                        if (data.logoutTeleportTimer >= getLogoutTeleportTimer(data)) {
                            new LoggerUpdate(piglinLogger.getHealth(), piglinLogger.getLocation(), loggerData.playerInventory)
                                    .handleLoggerUpdateData(data, piglinLogger);
                            cancel();
                        }
                        else if (saveLogoutTimerQuery.isDone())
                            saveLogoutTimerQuery = fetchUpdatePlayerDataIntVoid("logout_teleport_timer", data.logoutTeleportTimer, data.sqliteId);
                    }
                }
            }.runTaskTimer(plugin, 20L, 20L);
            // Logout + tag end
        }
    }
    public static void getPlayerWeapon(PlayerInventory pi, EntityEquipment ee) {
        ItemStack weapon = pi.getItemInMainHand();
        double highestWeaponDamage = 0;
        double iteratorWeaponDamage = 0;
        for (int i = 0; i <= 8; i++) {
            ItemStack is = pi.getItem(i);
            if (is != null) {
                ItemMeta im = is.getItemMeta();
                if (im != null && im.hasAttributeModifiers()) {
                    Collection<AttributeModifier> damageModifiers = im.getAttributeModifiers(Attribute.ATTACK_DAMAGE);
                    if (damageModifiers != null)
                        for (AttributeModifier modifier : damageModifiers)
                            iteratorWeaponDamage += modifier.getAmount();
                    if (iteratorWeaponDamage > highestWeaponDamage) {
                        weapon = is;
                        highestWeaponDamage = iteratorWeaponDamage;
                    }
                }
            }
        }
        ee.setItemInMainHand(weapon);
    }
    public static void getPlayerEntity(Ageable entity, LivingEntity playerEntity, Collection<EnderPearl> statelessPearls, EntityEquipment entityEquipment) {
        entity.setCanPickupItems(false);
        entity.setCustomNameVisible(true);
        entity.customName(Component.text(playerEntity.getName()));
        entity.setKiller(playerEntity.getKiller()); // TODO unsure if this works
        entity.setFireTicks(playerEntity.getFireTicks());
        entity.setArrowsInBody(playerEntity.getArrowsInBody());
        entity.setFreezeTicks(playerEntity.getFreezeTicks());
        entity.setVelocity(playerEntity.getVelocity());
        entity.setBeeStingersInBody(playerEntity.getBeeStingersInBody());
        entity.setBodyYaw(playerEntity.getBodyYaw());
        entity.setNoDamageTicks(playerEntity.getNoDamageTicks());
        entity.setRemoveWhenFarAway(false);
        entity.setAbsorptionAmount(playerEntity.getAbsorptionAmount());
        entity.setFrictionState(playerEntity.getFrictionState());
        entity.setAbsorptionAmount(playerEntity.getAbsorptionAmount());
        entity.addPotionEffects(playerEntity.getActivePotionEffects());
        entity.setFallDistance(playerEntity.getFallDistance());
        entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20);
        entity.setAdult();

        for (EnderPearl enderPearl : statelessPearls)
            enderPearl.setShooter(entity);

        EntityEquipment ee = entity.getEquipment();
        ee.setHelmet(entityEquipment.getHelmet());
        ee.setChestplate(entityEquipment.getChestplate());
        ee.setLeggings(entityEquipment.getLeggings());
        ee.setBoots(entityEquipment.getBoots());
        ee.setItemInMainHand(entityEquipment.getItemInMainHand());
        ee.setItemInOffHand(entityEquipment.getItemInOffHand());
        //  TODO SIMULATE/TRACK DURABILITY LOSS

        if (ee.getHelmet() == null && playerEntity instanceof Player player) {
            ItemStack zombieHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) zombieHead.getItemMeta();
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()));
            zombieHead.setItemMeta(skullMeta);
            ee.setHelmet(zombieHead);
        }
    }
}
