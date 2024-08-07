package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.*;
import com.memeasaur.potpissersdefault.Classes.ClaimCoordinate;
import com.memeasaur.potpissersdefault.Classes.PlayerData;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

import java.util.HashMap;

import static com.memeasaur.potpissersdefault.Classes.LoggerData.KEY_PIGLIN_LOGGER;
import static com.memeasaur.potpissersdefault.Listeners.PlayerInteractListener.doEnchantmentUpdate;
import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.*;
import static com.memeasaur.potpissersdefault.Util.Combat.Constants.HARMING_DAMAGE_CD;
import static com.memeasaur.potpissersdefault.Util.CubecoreSwords.Constants.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants.LogoutTeleport.TAG_LOGOUT_TIMER;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Methods.getClaim;
import static com.memeasaur.potpissersdefault.Util.Claim.Methods.isUnbreakableClaim;
import static com.memeasaur.potpissersdefault.Util.Combat.Methods.handleCombatTag;
import static com.memeasaur.potpissersdefault.Util.CubecoreSwords.Methods.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getDangerComponent;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getFocusComponent;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Timer.doHarmingDamageCd;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Timer.handleScoreboardTimer;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods1.spawnEntityParticles;
import static org.bukkit.enchantments.Enchantment.FIRE_ASPECT;
import static org.bukkit.enchantments.Enchantment.SHARPNESS;
import static org.bukkit.potion.PotionEffectType.STRENGTH;
import static org.bukkit.potion.PotionEffectType.WEAKNESS;

public class EntityDamageByEntityListener implements Listener {
    @EventHandler
    void onDamagedCubecore(EntityDamageByEntityEvent e) {
        if (e.getDamageSource().getCausingEntity() instanceof Player p) {
            if (e.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) {
                ItemStack is = p.getInventory().getItemInMainHand();
                if (!is.getPersistentDataContainer().has(KEY_EVENT_SWORD)) {
                    if (is.getEnchantmentLevel(SHARPNESS) > sharpnessLimit) {
                        doEnchantmentUpdate(is, SHARPNESS, sharpnessLimit, p, e);
                        e.setCancelled(true);
                        return;
                    }
                    if (is.getEnchantmentLevel(FIRE_ASPECT) > 0) {
                        is.removeEnchantment(FIRE_ASPECT);
                        p.sendMessage(getDangerComponent("fire aspect removed"));
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            if (e.getEntity() instanceof Player p1) {
                PlayerData data = playerDataCache.get(p);
                PlayerData data1 = playerDataCache.get(p1);
                if (data.frozen || data1.frozen) {
                    e.setCancelled(true);
                    p.sendMessage(getDangerComponent("cancelled (frozen)"));
                    return;
                }
                // Combat-tag start
                boolean isSelfInflicted = p == p1;
                // Combat-tag end
                // Claims start
                if (!isSelfInflicted && data.currentClaim.equals(SPAWN_CLAIM)) {
                    e.setCancelled(true);
                    return;
                }
                // Claims end
                // Watcher start
                if ((p.hasPermission("potpissers.watcher") && !p.hasPermission("potpissers.mod"))
                        || (p1.hasPermission("potpissers.watcher") && !p1.hasPermission("potpissers.mod")))
                    e.setDamage(0);
                // Watcher end

                // FightTracker start
                if (!isSelfInflicted) {
                    if (data1.playerFightTracker == null) {
                        if (data.playerFightTracker == null)
                            new FightTracker(p, data, p1, data1);
                        else
                            data1.playerFightTracker = new PlayerFightTracker(data1, data1.getActiveMutableParty()[0], (FightTracker) data.playerFightTracker.statTracker, p1);
                    } else if (data.playerFightTracker == null)
                        data.playerFightTracker = new PlayerFightTracker(data, data.getActiveMutableParty()[0], (FightTracker) data1.playerFightTracker.statTracker, p);

                    if (data.playerFightTracker.statTracker != data1.playerFightTracker.statTracker)
                        ((FightTracker) data.playerFightTracker.statTracker).handleMerge((FightTracker) data1.playerFightTracker.statTracker);

                    if (data.playerFightTracker.statTracker instanceof FightTracker fightTracker) {
                        if (fightTracker.getTeamSize(data.getActiveMutableParty()[0]) < fightTracker.getTeamSize(data1.getActiveMutableParty()[0])) // smaller parties might not be able to hit larger parties
                            if (fightTracker.handleIsFighterProtected(data1, data)) // main fighters might not be able to be hit by non-main fighters
                                if (fightTracker.isOutnumbered(data1.getActiveMutableParty()[0], data1.playerFightTracker)) { // any non-main fighters can join the fight by hitting non-outnumbered main fighters
                                    if (!fightTracker.handleIsAllyForceable(data1, data)) {
                                        e.setCancelled(true);
                                        p.sendMessage(getDangerComponent("cancelled (fight protection)"));
                                        return;
                                    }
                                } else {
                                    if (fightTracker.mainAbstractPartyData1 == data1.getActiveMutableParty()[0] || fightTracker.party1Allies.contains(data1.playerFightTracker))
                                        fightTracker.party2Allies.add(data.playerFightTracker);
                                    else
                                        fightTracker.party1Allies.add(data.playerFightTracker);
                                }
                    }
                }
                // FightTracker end

                switch (e.getCause()) {
                    case ENTITY_ATTACK -> {

                        String attackSpeedName = data.playerFightTracker instanceof PlayerFightTracker playerFightTracker && playerFightTracker.statTracker instanceof DuelTracker duelTracker ? duelTracker.duelOptions.attackSpeedName() : defaultAttackSpeedName; // Claims
                        double attackSpeedValue = ATTACK_SPEED_VALUES.get(attackSpeedName);

                        AttributeInstance attackSpeedInstance = p.getAttribute(Attribute.ATTACK_SPEED);
                        if (attackSpeedInstance.getBaseValue() != attackSpeedValue) { // Claims
                            attackSpeedInstance.setBaseValue(attackSpeedValue); // Claims
                            p.sendMessage(getFocusComponent("attack speed: " + attackSpeedName)); // Claims
                        }
                        else if (!ATTACK_SPEED_VANILLA_NAMES.contains(attackSpeedName) && p.getAttackCooldown() != 1) {
                            e.setCancelled(true);
                            p.sendMessage(getDangerComponent("attack cancelled (cooldown). current attack speed: " + attackSpeedName));
                            p.resetCooldown();
                            return;
                        }

                        // Movement cd start
                        ItemStack weapon = p.getInventory().getItemInMainHand();
                        if (weapon.getEnchantmentLevel(Enchantment.KNOCKBACK) > 0) {
                            ScoreboardTimer movementTimer = data.movementTimer;
                            if (movementTimer.timer == 0) {
                                handleScoreboardTimer(movementTimer, KNOCKBACK_CD * weapon.getEnchantmentLevel(Enchantment.KNOCKBACK), data, data.uuid, data.sqliteId);
                            }
                            else {
                                e.setCancelled(true);
                                p.sendMessage(getDangerComponent("cancelled (movement cd)"));
                                return;
                            }

                        }
                        // Movement cd end

                        // Mace/trident/sharpness start
                        Material weaponType = weapon.getType();
                        switch (weaponType) {
                            case NETHERITE_SWORD -> {
                                if (IS_NETHERITE_SWORD_NERFED)
                                    e.setDamage(e.getDamage() - 1);
                            }
                            case TRIDENT -> {
                                if (p1.isInWater()) {
                                    e.setDamage(e.getDamage() - 1); // trident better than sword in water
                                    int adjustedImpaling = weapon.getEnchantmentLevel(Enchantment.IMPALING);
                                    e.setDamage(e.getDamage() + adjustedImpaling * 1.25F);
                                } else
                                    e.setDamage(e.getDamage() - 2); // trident == unEnchanted diamond_sword out of water
                            }
                            case MACE -> {
                                double damage = e.isCritical() ? p.getAttribute(Attribute.ATTACK_DAMAGE).getValue() + 2
                                        : p.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
                                if (e.isCritical())
                                    damage *= 1.5F;
                                float adjustedBreach = (float) weapon.getEnchantmentLevel(Enchantment.BREACH);
                                e.setDamage(damage + adjustedBreach * 1.25F);
                                if (p.getFallDistance() >= 4) {
                                    PlayerInventory pi = p1.getInventory();
                                    int protection = 12; // ff4
                                    if (pi.getHelmet() != null)
                                        protection += pi.getHelmet().getEnchantmentLevel(Enchantment.PROTECTION);
                                    if (pi.getChestplate() != null)
                                        protection += pi.getChestplate().getEnchantmentLevel(Enchantment.PROTECTION);
                                    if (protection <= 20 && pi.getLeggings() != null)
                                        protection += pi.getLeggings().getEnchantmentLevel(Enchantment.PROTECTION);
                                    if (protection <= 20 && pi.getBoots() != null)
                                        protection += pi.getBoots().getEnchantmentLevel(Enchantment.PROTECTION);
                                    protection = Math.min(20, protection);
                                    double maceDropDmg = (p.getFallDistance() - 3) * (1 - protection / 25F);

                                    p1.setHealth(Math.max(p1.getHealth() - maceDropDmg, 0));
                                    // TODO the regular hit should totem disable but that would require moving setHealth lower
                                }
                            }
                        }
                        if (IS_REVERTED_STRENGTH) {
                            int sharpness = weapon.getEnchantmentLevel(Enchantment.SHARPNESS);
                            if (sharpness > 0)
                                e.setDamage(e.getDamage() - (0.5 * sharpness + 0.5) + (sharpness * 1.25F));
                        }
                        // Mace/trident/sharpness end

                        // Strength/weakness start
                        if (IS_REVERTED_STRENGTH) {
                            if (p.getPotionEffect(STRENGTH) instanceof PotionEffect strength) {
                                int strengthLevel = strength.getAmplifier() + 1;
                                double damageMinusStrength = e.getDamage() - (3 * strengthLevel);
                                e.setDamage(damageMinusStrength * (1 + .3 * strengthLevel));
//      e.setDamage(damageMinusStrength + (damageMinusStrength * (strengthLevel * 1.3))); this is vanilla 1.7, which is too insane!
                            }
                            if (p.getPotionEffect(WEAKNESS) instanceof PotionEffect weakness) {
                                int weaknessLevel = weakness.getAmplifier() + 1;
                                e.setDamage(e.getDamage() + (weaknessLevel * 4) - (weaknessLevel * 0.5));
                            }
                        }
                        // Strength/weakness end

                        if (p.isSprinting())
                            SCHEDULER.runTaskLater(plugin, () ->
                                    p.setSprinting(true), 1L);
                    }
                    // Explosions start
                    case ENTITY_EXPLOSION, BLOCK_EXPLOSION -> {
                        // Harming cd start
                        if (e.getDamager() instanceof Firework && !isSelfInflicted)
                            handleHarmingCdDamage(e, isSelfInflicted, data1);
                        // Harming cd end

                        p1.setHealth(Math.max(p1.getHealth() - e.getDamage() / 2.25, 0));
                        e.setDamage(0);
                    }
                    // Explosions end
                    // Fishing rod start
                    case PROJECTILE -> {
                        switch (e.getDamager().getType()) {
                            case FISHING_BOBBER -> e.setCancelled(true);
                            // Movement cd start
                            case ARROW -> {
                                Arrow aa = (Arrow) e.getDamager();
                                handleKnockbackArrow(aa, data, e, p);
                                // Cubecore swords start
                                ItemStack weapon = aa.getWeapon();
                                if (weapon != null && weapon.getPersistentDataContainer().get(KEY_EVENT_SWORD, PersistentDataType.STRING) instanceof String cubecoreSwordType)
                                    switch (cubecoreSwordType) {
                                        case LEGENDARY_BOW_HEAL -> {
                                            // TODO transfer health meme (?)
                                            e.setDamage(0);
                                            if (weapon.getEnchantments().containsKey(Enchantment.INFINITY))
                                                p1.heal(1);
                                            else
                                                p1.heal(1.875);
                                        }
                                        case LEGENDARY_BOW_PHANTOM_BOW -> {
                                            // TODO handle get damage difference
                                            handlePhantomBladeCdRefund(data, 1);
                                        }
                                        case LEGENDARY_BOW_SIMOONS_MELODY -> {}
                                    }
                                // Cubecore swords end
                            }
                            case SPECTRAL_ARROW -> {
                                SpectralArrow aa = (SpectralArrow) e.getDamager();
                                handleKnockbackArrow(aa, data, e, p);
                            }
                            // Movement cd end
                        }
                    }
                    // Fishing rod end
                    // Harming cd start
                    case MAGIC -> {
                        handleHarmingCdDamage(e, isSelfInflicted, data1);

                        p1.setHealth(Math.max(p1.getHealth() - (e.getDamage() / 3) * 2, 0));
                        e.setDamage(0);
                    }
                    // Harming cd end
                }
                // Combat-tag start
                if (!isSelfInflicted && !e.isCancelled()) {
                    if (p1.isBlocking()) {
                        handleCombatTag(data1, COMBAT_TAG);
                    }
                    if (!e.isCancelled())
                        handleCombatTag(data, COMBAT_TAG);

                    // FightTracker start
                    data.playerFightTracker.fightTrackerTimer = TAG_LOGOUT_TIMER;
                    data1.playerFightTracker.fightTrackerTimer = TAG_LOGOUT_TIMER;

                    double damage = e.getDamage();

                    data.playerFightTracker.partyDamage.compute(data1.getActiveMutableParty()[0], (k, partyDamage) -> (partyDamage == null ? 0 : partyDamage) + damage);

                    data.playerFightTracker.victimStats.computeIfAbsent(data1.getActiveMutableParty()[0] instanceof AbstractPartyData party ? party.uuid : null, k -> new HashMap<>())
                            .computeIfAbsent(data1.uuid, k -> new PvpStats())
                            .damageDealt += damage;
//                    pvpStats.meleeAttacks++; // TODO -> move this to player attacks
                    // FightTracker end
                }
                // Combat-tag end
            }
            else {
                Entity hitEntity = e.getEntity();

                // Combat-tag start
                if (hitEntity instanceof Piglin piglin && loggerDataCache.containsKey(piglin))
                    handleCombatTag(playerDataCache.get(p), COMBAT_TAG);
                // Combat-tag end

                if (e.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)
                        && !hitEntity.getPersistentDataContainer().has(KEY_PIGLIN_LOGGER)) {
                    if (!ATTACK_SPEED_VANILLA_VALUES.contains(p.getAttribute(Attribute.ATTACK_SPEED).getBaseValue())) {
                        p.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(ATTACK_SPEED_VALUES.get(REVERTED_VANILLA));
                        p.sendMessage(getFocusComponent("attack speed: vanilla"));
                    }
                }
            }
        }
        // Cubecore swords start
        if (e.getDamager() instanceof LivingEntity le && le.getEquipment() != null && CUBECORE_SWORD_DAMAGE_TYPES.contains(e.getDamageSource().getDamageType())) {
            ItemStack weapon = le.getEquipment().getItemInMainHand();
            if (weapon.getItemMeta() != null && weapon.getItemMeta().getPersistentDataContainer().has(KEY_EVENT_SWORD, PersistentDataType.STRING)) {
                String eventSwordType = weapon.getItemMeta().getPersistentDataContainer().getOrDefault(KEY_EVENT_SWORD, PersistentDataType.STRING, "null");
                switch (eventSwordType) {
                    case LEGENDARY_SWORD_AGNIS_RAGE -> {
                        if (e.getEntity() instanceof LivingEntity le1) { // these should take protection into account (?), remember fire damage is true damage without it
                            double lostIronSwordDamage = getCubecoreSwordLostDamage(e, IRON_SWORD_DAMAGE_DIFFERENCE);
                            double lostIronSwordDamageBasedChance = Math.min(lostIronSwordDamage * .25, 1.0);
                            // this should be +1 damage to both players. ie lower passive brings sword dps up to diamond or certain level, and this brings damage up by 1 but hits both players
                            if (Math.random() < lostIronSwordDamageBasedChance) {
                                le.setFireTicks(Math.min(le.getFireTicks() + 80, 160));
                                // do effects
                            }
                            if (Math.random() < lostIronSwordDamageBasedChance) {
                                le1.setFireTicks(Math.min(le1.getFireTicks() + 80, 160));
                                // do effects
                            }

                            double damageDifference = getDiamondSwordDamageDifference(weapon);
                            if (damageDifference > 0) {
                                double lostDamage = getCubecoreSwordLostDamage(e, damageDifference);
                                double lostDamageBasedChance = Math.min(lostDamage * .25, 1.0);
                                if (Math.random() < lostDamageBasedChance) {
                                    le1.setFireTicks(Math.min(le1.getFireTicks() + 80, 160));
                                    doAgnisRageEffects(le, le1);
                                }
                            }
                        }
                    }
                    case LEGENDARY_SWORD_VAMPYR -> {
                        if (e.getEntity() instanceof LivingEntity le1) {
                            double damageDifference = getDiamondSwordDamageDifference(weapon);
                            if (damageDifference > 0) {
                                AttributeInstance maxHealthInstance = le1.getAttribute(Attribute.MAX_HEALTH);
                                double healthPercentage = le1.getHealth() / (maxHealthInstance != null ? maxHealthInstance.getValue() : le1.getHealth());

                                double lostDamage = getCubecoreSwordLostDamage(e, damageDifference + 1) * healthPercentage; // iron sword is diamond sword + 1 at 0 and iron sword at 100 (%)

                                doVampyrEffects(le, (int) Math.round(lostDamage), le1);

                                le.heal(lostDamage);
                            }
                        }
                    }
                    case LEGENDARY_SWORD_BERSERKER -> {
                        if (e.getEntity() instanceof LivingEntity le1) {
                            double damageDifference = getDiamondSwordDamageDifference(weapon);
                            if (damageDifference > 0) {
                                AttributeInstance maxHealthInstance = le.getAttribute(Attribute.MAX_HEALTH);
                                double healthPercentage = le.getHealth() / (maxHealthInstance != null ? maxHealthInstance.getValue() : le.getHealth());

                                double lostDamage = getCubecoreSwordLostDamage(e, damageDifference + 1) * healthPercentage; // iron sword is diamond sword + 1 at 0 and iron sword at 100 (%)

                                doVampyrEffects(le, (int) Math.round(lostDamage), le1);

                                le.heal(lostDamage);
                            }
                        }
                    }
                    case LEGENDARY_SWORD_LAST_STAND -> {
                        if (e.getEntity() instanceof LivingEntity le1) {
                            double damageDifference = getDiamondSwordDamageDifference(weapon);
                            if (damageDifference > 0) {
                                AttributeInstance maxHealthInstance = le.getAttribute(Attribute.MAX_HEALTH);
                                double healthPercentage = le.getHealth() / (maxHealthInstance != null ? maxHealthInstance.getValue() : le.getHealth());

                                double lostDamage = getCubecoreSwordLostDamage(e, damageDifference + 1) * healthPercentage; // iron sword is diamond sword + 1 at 0 and iron sword at 100 (%)

                                World world = le1.getWorld();
                                spawnEntityParticles(world, Particle.CRIT, le1.getBoundingBox().getCenter(), (int) Math.round(lostDamage));
                                world.playSound(le1, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 1, 1);

                                le1.setHealth(Math.max(le1.getHealth() - lostDamage, 0));
                            }
                        }
                    }
                    case LEGENDARY_SWORD_COUP_DE_GRACE -> {
                        if (e.getEntity() instanceof LivingEntity le1) {
                            double damageDifference = getDiamondSwordDamageDifference(weapon);
                            if (damageDifference > 0) {
                                AttributeInstance maxHealthInstance = le1.getAttribute(Attribute.MAX_HEALTH);
                                double healthPercentage = le1.getHealth() / (maxHealthInstance != null ? maxHealthInstance.getValue() : le1.getHealth());

                                double lostDamage = getCubecoreSwordLostDamage(e, damageDifference + 1) * healthPercentage; // iron sword is diamond sword + 1 at 0 and iron sword at 100 (%)

                                World world = le1.getWorld();
                                spawnEntityParticles(world, Particle.CRIT, le1.getBoundingBox().getCenter(), (int) Math.round(lostDamage));
                                world.playSound(le1, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 1, 1);

                                le1.setHealth(Math.max(le1.getHealth() - lostDamage, 0));
                            }
                        }
                    }
                    case LEGENDARY_SWORD_PHANTOM_BLADE -> { // making a pvp switch might be the move eventually
                        if (e.getDamager() instanceof Player player && e.getEntity() instanceof Player) {
                            double damageDifference = getDiamondSwordDamageDifference(weapon);
                            if (damageDifference > 0) { // this one uses damageDifference rather than damageLost because it isn't a stat-check type effect
                                PlayerData data = playerDataCache.get(player);
                                handlePhantomBladeCdRefund(data, damageDifference);
                            }
                            // use lostDamage if broken, which this very well could be
                        }
                    }
                    case LEGENDARY_SWORD_THERUMS_STRENGTH -> { // maybe do diamond sword +1, even though this item should probably suck cock since it's potentially lame
                        double damageDifference = getDiamondSwordDamageDifference(weapon);
                        if (damageDifference > 0) {
                            double lostDamage = getCubecoreSwordLostDamage(e, damageDifference);
                            // effect lasts for 2 attack windows (20 ticks / 10), resistance 1 negates 20% damage
                            double therumsStrengthDamageReductionAmount = e.getFinalDamage() * 2 * .2; // use the effect for this
                            double lostDamageBasedChance = Math.min(lostDamage / therumsStrengthDamageReductionAmount, 1.0);
                            // this should just give an amount of ticks based on this (?)
                            if (Math.random() < lostDamageBasedChance) {
                                le.addPotionEffect(THERUMS_STRENGTH_RESISTANCE); // maybe make this add to the current resistance amount (?)
                                doTherumsStrengthEffects(le, (int) Math.round(therumsStrengthDamageReductionAmount));
                            }
                        }
                    }
                    case LEGENDARY_SWORD_MURASAME -> {
                        if (e.getEntity() instanceof LivingEntity livingEntity1) {
                            double originalDamage = e.getDamage();
                            double originalFinalDamage = e.getFinalDamage();
                            e.setDamage(0);
                            livingEntity1.heal(originalFinalDamage + 1);
                            le.damage(originalDamage);
                            // test dura loss
                            // impl right-click flat heal meme
                        }
                    }
                    case LEGENDARY_SWORD_SIMOONS_SONG -> { // lone sword, etc
                        if (e.getEntity() instanceof LivingEntity livingEntity1) {
                            // cooldown reduce
                        }
                    }
                    case LEGENDARY_SWORD_PLUVIAS_STORM -> {
                        if (e.getDamager() instanceof Player player && e.getEntity() instanceof Player player1) {
                            double damageDifference = getDiamondSwordDamageDifference(weapon);
                            if (damageDifference > 0) {
                                double lostDamage = getCubecoreSwordLostDamage(e, damageDifference);
                                double lostDamageScaledExhaustion = lostDamage * 4.0f; // nerfed regen exhaustion amount
                                float lostDamageScaledExhaustionFloat = (float) lostDamageScaledExhaustion;
                                player1.setExhaustion(player1.getExhaustion() + lostDamageScaledExhaustionFloat);
                                player.setExhaustion(player.getExhaustion() - lostDamageScaledExhaustionFloat);
                            }
                        }
                    }
                }
            }
        }
        // Cubecore swords end
        switch (e.getEntity().getType()) {
            case PIGLIN -> {
                if (e.getEntity() instanceof Piglin piglin && loggerDataCache.containsKey(piglin)) {
                    piglin.setTarget((LivingEntity) e.getDamageSource().getCausingEntity());
                    piglin.setAI(true);
                }
            }
            // Combat tag start
            case PLAYER -> {
                if (e.getDamager() instanceof Piglin piglin && loggerDataCache.containsKey(piglin))
                    handleCombatTag(loggerDataCache.get(piglin).playerData, COMBAT_TAG);
            }
            // Combat tag end

            // Claims start
            case ITEM_FRAME, GLOW_ITEM_FRAME, ARMOR_STAND, PAINTING -> {
                LivingEntity causingEntity = (LivingEntity) e.getDamageSource().getCausingEntity();
                if (causingEntity != null &&
                        (!(causingEntity instanceof Player player) || !player.getGameMode().isInvulnerable())) {
                    Entity entity = e.getEntity();
                    Object claim = getClaim(new ClaimCoordinate(entity.getLocation()));
                    if (isUnbreakableClaim(claim)) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            // Claims end
        }
    }

    // Movement cd start
    void handleKnockbackArrow(AbstractArrow aa, PlayerData data, Cancellable e, Player p) {
        ItemStack weapon = aa.getWeapon();
        if (weapon != null && weapon.getEnchantmentLevel(Enchantment.PUNCH) > 0) {
            ScoreboardTimer timer = data.movementTimer;
            if (timer.timer != 0) {
                e.setCancelled(true);
                p.sendMessage(getDangerComponent("punch arrow cancelled (movement cd)"));
                return;
            } else {
                handleScoreboardTimer(timer, KNOCKBACK_CD * weapon.getEnchantmentLevel(Enchantment.PUNCH), data, data.uuid, data.sqliteId);
            }
        }
    }
    // Movement cd end

    // Cubecore swords start
    void handlePhantomBladeCdRefund(PlayerData data, double damageDifference) {
        ScoreboardTimer scoreboardTimer = data.movementTimer;
        if (scoreboardTimer.timer > 0) {
            scoreboardTimer.timer -= Math.max(0, (int) Math.round(damageDifference));
            // TODO support negative cd meme
        }
    }
    // Cubecore swords end
    // Harming cd start
    void handleHarmingCdDamage(EntityDamageByEntityEvent e, boolean isSelfInflicted, PlayerData data1) {
        // Harming cd start
        if (e.getDamager() instanceof Firework && !isSelfInflicted) {
            e.setDamage(e.getDamage() * (((float) HARMING_DAMAGE_CD - data1.harmingDamageCd) / HARMING_DAMAGE_CD));
            doHarmingDamageCd(data1);
        }
        // Harming cd end
    }
    // Harming cd end
}
