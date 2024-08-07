package com.memeasaur.potpissersdefault.Util.CubecoreSwords;

import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

import static com.memeasaur.potpissersdefault.Util.CubecoreSwords.Methods.getDiamondSwordDamageDifference;

public class Constants {
    // Cubecore swords start
    public static final NamespacedKey KEY_EVENT_SWORD = new NamespacedKey("eventswordkey", "eventswordkey");
    public static final NamespacedKey KEY_EVENT_SWORD_COOLDOWN = new NamespacedKey("eventswordcooldown", "eventswordcooldown");

    public static final String LEGENDARY_SWORD_AGNIS_RAGE = "Agni's Rage";
    // agni's rage bow
    public static final String LEGENDARY_SWORD_VAMPYR = "Vampyr";
    public static final String LEGENDARY_SWORD_BERSERKER = "Berserker";
    public static final String LEGENDARY_SWORD_LAST_STAND = "Last Stand";
    public static final String LEGENDARY_SWORD_COUP_DE_GRACE = "Coup De Grace"; // nerf this to be scaled to diamond rather than diamond + 1, it's currently +1 diamond damage for free
    public static final String LEGENDARY_SWORD_OVERKILL = "Overkill"; // coup de grace diamond sword + 1 on low hp current sword -1 on high hp or something similar. probably just merge this with coup de grace
    public static final String LEGENDARY_SWORD_PHANTOM_BLADE = "Phantom Blade";
    public static final String LEGENDARY_BOW_PHANTOM_BOW = "Phantom Bow";
    public static final String LEGENDARY_SWORD_LONE_SWORD = "Lone Sword"; // combined with masamune
    public static final String LEGENDARY_SWORD_MURASAME = "Murasame"; // give tenseiga heal aura to right-click
    public static final String LEGENDARY_SWORD_THERUMS_STRENGTH = "Therum's Strength";
    public static final String LEGENDARY_SWORD_PLUVIAS_STORM = "Pluvia's Storm"; // combined with muramasa
    public static final String LEGENDARY_SWORD_SIMOONS_SONG = "Simoon's Song"; // kb2 + slow-falling + launches vertically
    // simoon's song bow
    public static final String LEGENDARY_SWORD_SACRIFICIAL_SWORD = "Sacrificial Sword"; // clears inventory, gives bad effects, gives buffs to nearby players. maybe only gives bad effects
    public static final String LEGENDARY_SWORD_SIMOONS_DEAL = "REDACTED's Deal"; // gives buffs, then gives negative effects. or vice versa. less duration than sugar. something like speed 2 + strength 3 (strength is weaker -> strength 3 is 1.9 strength 1). shows constant particles
    public static final String LEGENDARY_BOW_HEAL = "Heal Bow";
    public static final String LEGENDARY_BOW_SIMOONS_MELODY = "Simoon's Melody"; // levitation (?) + slow-falling

    public static final String LEGENDARY_SWORD_CONQUEROR = "Conqueror"; // stacks up by 1 per hit, goes down by per damaged OR stacks up and falls off very fast, or stacks up 1 by per hit, stacks down to 0 when damaged
    public static final String LEGENDARY_SWORD_EARTH_SHAKER = "Earth Shaker";
    public static final String LEGENDARY_BOW_SHOTBOW = "Shotbow";
    public static final String LEGENDARY_BOW_LONGBOW = "Longbow";
    public static final String LEGENDARY_BOW_AGNIS_FURY = "Agni's Fury";
    // sword that gives target speed self slow
    // sword that gives both slowness
    // sword that gives both speed

    public static final ItemStack IRON_SWORD_ITEM_STACK = new ItemStack(Material.IRON_SWORD);
    public static final double DIAMOND_SWORD_ATTRIBUTE_DAMAGE = Material.DIAMOND_SWORD.getDefaultAttributeModifiers().get(Attribute.ATTACK_DAMAGE).stream().mapToDouble(AttributeModifier::getAmount).sum();
    public static final PotionEffect THERUMS_STRENGTH_RESISTANCE = new PotionEffect(PotionEffectType.RESISTANCE, 20, 0);

    public static final double IRON_SWORD_DAMAGE_DIFFERENCE = getDiamondSwordDamageDifference(IRON_SWORD_ITEM_STACK);

    public static final BlockData IRON_BLOCK_DATA = Bukkit.createBlockData(Material.IRON_BLOCK);
    public static final BlockData REDSTONE_WIRE_BLOCK_DATA = Bukkit.createBlockData(Material.REDSTONE_WIRE);

    public static final PotionEffect LONE_SWORD_HEALTH_BOOST = new PotionEffect(PotionEffectType.HEALTH_BOOST, 160, 1);
    public static final PotionEffect LONE_SWORD_REGENERATION = new PotionEffect(PotionEffectType.REGENERATION, 160, 0);

    public static final PotionEffect SIMOONS_SONG_SLOW_FALLING = new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 0);

    public static final HashSet<DamageType> CUBECORE_SWORD_DAMAGE_TYPES = new HashSet<>(List.of(DamageType.PLAYER_ATTACK, DamageType.MOB_ATTACK));
    public static final EnumSet<EntityKnockbackEvent.Cause> CUBECORE_SWORD_MELEE_KNOCKBACK_CAUSES = EnumSet.of(EntityKnockbackEvent.Cause.ENTITY_ATTACK, EntityKnockbackEvent.Cause.SWEEP_ATTACK);

    public static final NamespacedKey KEY_WIND_SURGE_CHARGE = new NamespacedKey("windsurgecharge", "windsurgecharge");
    // Cubecore swords end
}
