package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.ClaimCoordinate;
import com.memeasaur.potpissersdefault.Classes.LoggerData;
import com.memeasaur.potpissersdefault.Classes.PlayerData;
import org.bukkit.Material;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Objects;

import static com.memeasaur.potpissersdefault.Classes.AbstractPartyData.executeNetworkPartyMessage;
import static com.memeasaur.potpissersdefault.Classes.AbstractPartyData.fetchNonnullPartyName;
import static com.memeasaur.potpissersdefault.Classes.LoggerData.GAPPLE_EFFECT;
import static com.memeasaur.potpissersdefault.Classes.LoggerData.OPPLE_EFFECT;
import static com.memeasaur.potpissersdefault.Listeners.PlayerInteractListener.doEnchantmentUpdate;
import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.PotpissersDefault.isMobDamageImmunityToggled;
import static com.memeasaur.potpissersdefault.Util.Claim.Methods.getClaim;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.SPAWN_CLAIM;
import static com.memeasaur.potpissersdefault.Util.Combat.Methods.getLocationString;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getFocusComponent;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Timer.handleTimerCancel;
import static org.bukkit.enchantments.Enchantment.PROTECTION;

public class EntityDamageListener implements Listener {
    @EventHandler
    void onDamageCubecore(EntityDamageEvent e) {
        DamageType damageType = e.getDamageSource().getDamageType();
        if (isMobDamageImmunityToggled && !(e.getEntity() instanceof Player) && (damageType.equals(DamageType.FALL) || damageType.equals(DamageType.IN_WALL))) {
            e.setCancelled(true);
            return;
        }
        switch (e.getEntity().getType()) {
            case PLAYER -> {
                Player p = (Player) e.getEntity();
                if (Objects.equals(playerDataCache.get(p).currentClaim, SPAWN_CLAIM) && !damageType.equals(DamageType.OUT_OF_WORLD)) {
                    e.setCancelled(true);
                    return;
                }
                PlayerData data = playerDataCache.get(p);
                handleTimerCancel(data, p);
                PlayerInventory pi = p.getInventory();

                if (pi.getHelmet() instanceof ItemStack helmet && helmet.getEnchantmentLevel(Enchantment.PROTECTION) > protectionLimit)
                    doArmorEnchantCheck(p, helmet);
                if (pi.getChestplate() instanceof ItemStack chestPlate && chestPlate.getEnchantmentLevel(Enchantment.PROTECTION) > protectionLimit)
                    doArmorEnchantCheck(p, chestPlate);
                if (pi.getLeggings() instanceof ItemStack leggings && leggings.getEnchantmentLevel(Enchantment.PROTECTION) > protectionLimit)
                    doArmorEnchantCheck(p, leggings);
                if (pi.getBoots() instanceof ItemStack boots && boots.getEnchantmentLevel(Enchantment.PROTECTION) > protectionLimit)
                    doArmorEnchantCheck(p, boots);
            }
            case PIGLIN -> {
                Piglin piglin = (Piglin) e.getEntity();
                if (loggerDataCache.getOrDefault(piglin, null) instanceof LoggerData piglinData) {

                    // Claims start
                    if (getClaim(new ClaimCoordinate(piglin.getLocation())).equals(SPAWN_CLAIM)) {
                        e.setCancelled(true);
                        return;
                    }
                    // Claims end
                    // Logout + tag start
                    piglinData.playerData.logoutTeleportTimer = 0;
                    // Logout + tag end

                    if (!piglinData.eating) {
                        double health = piglin.getHealth() + piglin.getAbsorptionAmount();
                        if (health <= 16) {
                            for (ItemStack is : piglinData.playerInventory) {
                                if (is != null && is.getType().equals(Material.GOLDEN_APPLE)) {
                                    piglinData.doAppleConsume(is, GAPPLE_EFFECT);
                                    piglinData.playerData.getCurrentParties()
                                            .forEach(party ->
                                                    executeNetworkPartyMessage(party.uuid, fetchNonnullPartyName(party.uuid), getFocusComponent(piglin.getName() + ": just crappled: I have " + is.getAmount() + " left. i'm at " + getLocationString(piglin.getLocation()))));
                                    break;
                                }
                            }
                            if (health <= 13) {
                                for (ItemStack is : piglinData.playerInventory) {
                                    if (is != null && is.getType().equals(Material.ENCHANTED_GOLDEN_APPLE)) {
                                        piglinData.doAppleConsume(is, OPPLE_EFFECT);
                                        piglinData.playerData.getCurrentParties()
                                                .forEach(party ->
                                                        executeNetworkPartyMessage(party.uuid, fetchNonnullPartyName(party.uuid), getFocusComponent(piglin.getName() + ": just oppled: I have " + is.getAmount() + " left. i'm at " + getLocationString(piglin.getLocation()))));
                                        break;
                                    }
                                }
                                for (ItemStack is : piglinData.playerInventory) {
                                    if (is != null && is.getType().equals(Material.SPLASH_POTION) && Objects.equals(((PotionMeta)is.getItemMeta()).getBasePotionType(), PotionType.STRONG_HEALING)) {
                                        SCHEDULER.runTaskLater(plugin, () -> { // do regen, speed, weak_healing etc TODO
                                            ThrownPotion potion = piglin.launchProjectile(ThrownPotion.class);
                                            potion.setItem(is);
                                            is.setAmount(is.getAmount() - 1);
                                            potion.setVelocity(new Vector(0, -1, 0));

                                            piglinData.playerData.getCurrentParties().forEach(party ->
                                                    executeNetworkPartyMessage(party.uuid, fetchNonnullPartyName(party.uuid), getFocusComponent(piglin.getName() + ": i have " + Arrays.stream(piglinData.playerInventory).filter(itemStack -> itemStack.getItemMeta() instanceof PotionMeta potionMeta && Objects.equals(potionMeta.getBasePotionType(), (PotionType.STRONG_HEALING))).count() + " left. i'm at " + getLocationString(piglin.getLocation()))));
                                        }, 10L);
                                        break;
                                    }
                                }
                            } // OFFHAND OPPLE PROBLEMATIC!!!! like totem was TODO
                        }
                    }
                }
            }
        }
    }
    void doArmorEnchantCheck(Player p, ItemStack is) {
        if (is.getEnchantmentLevel(PROTECTION) > protectionLimit)
            doEnchantmentUpdate(is, PROTECTION, protectionLimit, p, null);
    }
}
