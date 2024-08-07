package com.memeasaur.potpissersdefault.Util.CubecoreSwords;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

import static com.memeasaur.potpissersdefault.Util.CubecoreSwords.Constants.DIAMOND_SWORD_ATTRIBUTE_DAMAGE;
import static com.memeasaur.potpissersdefault.Util.CubecoreSwords.Constants.IRON_BLOCK_DATA;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods1.spawnEntityParticles;

public class Methods {
    public static double getDiamondSwordDamageDifference(ItemStack weapon) {
        double currentWeaponTypeDamage = weapon.getType().getDefaultAttributeModifiers().get(Attribute.ATTACK_DAMAGE).stream().mapToDouble(AttributeModifier::getAmount).sum();
        return DIAMOND_SWORD_ATTRIBUTE_DAMAGE - currentWeaponTypeDamage; // TODO this only works properly with combat-revert formula, the new formula fucks everything with burst damage meme
    }

    public static double getCubecoreSwordLostDamage(EntityDamageByEntityEvent e, double damageDifference) {
        double originalDamage = e.getDamage();
        e.setDamage(e.isCritical() ? damageDifference * 1.5 : damageDifference); // TODO methodize this and impl strength/weakness etc
        double lostDamage = e.getFinalDamage(); // TODO test this
        e.setDamage(originalDamage);
        return lostDamage;
    }

    public static void doVampyrEffects(LivingEntity le, int lostDamageInt, LivingEntity le1) {
        World world = le.getWorld();
        spawnEntityParticles(world, Particle.HEART, le.getBoundingBox().getCenter(), lostDamageInt);
        BoundingBox le1BoundingBox = le1.getBoundingBox();
        world.spawnParticle(Particle.BLOCK, le1BoundingBox.getCenterX(), le1BoundingBox.getCenterY(), le1BoundingBox.getCenterZ(), lostDamageInt, IRON_BLOCK_DATA);
        world.playSound(le, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 1, 1);
    }

    public static void doAgnisRageEffects(LivingEntity le, LivingEntity le1) {
        World world = le.getWorld();
        // le particle
        spawnEntityParticles(world, Particle.LAVA, le1.getBoundingBox().getCenter(), 2); // 2 -> agni's rage effect damage result
        world.playSound(le, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 1, 1);
    }

    public static void doTherumsStrengthEffects(LivingEntity le, int damageMitigated) {
        World world = le.getWorld();
        BoundingBox boundingBox = le.getBoundingBox();
        world.spawnParticle(Particle.BLOCK, boundingBox.getCenterX(), boundingBox.getCenterY(), boundingBox.getCenterZ(), damageMitigated, IRON_BLOCK_DATA);
        world.playSound(le, Sound.BLOCK_BEACON_AMBIENT, 1, 1);
    }

    public static void doLoneSwordEffects(LivingEntity le) {
        World world = le.getWorld();
        BoundingBox boundingBox = le.getBoundingBox();
        world.spawnParticle(Particle.HEART, boundingBox.getCenterX(), boundingBox.getCenterY(), boundingBox.getCenterZ(), 1); // TODO find how much health is healed
        world.playSound(le, Sound.BLOCK_BEACON_AMBIENT, 1, 1);
    }

    public static void doPluviasStormEffects(LivingEntity le, int amount) {
        World world = le.getWorld();
        BoundingBox boundingBox = le.getBoundingBox();
        world.spawnParticle(Particle.COMPOSTER, boundingBox.getCenterX(), boundingBox.getCenterY(), boundingBox.getCenterZ(), amount); // find how much health is healed
        world.playSound(le, Sound.BLOCK_BEACON_AMBIENT, 1, 1);
    }
}
