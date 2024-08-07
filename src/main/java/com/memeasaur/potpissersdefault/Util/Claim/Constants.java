package com.memeasaur.potpissersdefault.Util.Claim;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

import static org.bukkit.Material.*;
import static org.bukkit.Material.COBWEB;

public class Constants {
    public static final String WILDERNESS_CLAIM = "wilderness";
    public static final String SPAWN_CLAIM = "spawn";
    public static final NamespacedKey KEY_CUBECORE_CHEST = new NamespacedKey("cubecorechest", "cubecorechest");
    public static final NamespacedKey KEY_SUPPLY_DROP_CHEST = new NamespacedKey("supplychest", "supplychest");
    public static final NamespacedKey KEY_SUPPLY_DROP_LOCKED_CHEST = new NamespacedKey("supplylockedchest", "supplylockedchest");

    public static final HashSet<String> MOBLESS_CLAIMS = new HashSet<>(List.of(SPAWN_CLAIM));

    public static final EnumSet<Material> EVENT_BREAKABLE_BLOCKS = EnumSet.of(LILY_PAD, COBWEB, SHORT_GRASS, TALL_GRASS, ROSE_BUSH, TRIPWIRE, FIRE, SOUL_FIRE, TWISTING_VINES, TWISTING_VINES_PLANT, Material.TNT, RAIL, POWERED_RAIL, DETECTOR_RAIL, ACTIVATOR_RAIL, LADDER, VINE, SCAFFOLDING, CAKE); // TODO BONE MEAL
    public static final EnumSet<Material> EVENT_PLACEABLE_BLOCKS = EnumSet.of(LILY_PAD, FIRE, SOUL_FIRE, TWISTING_VINES, TWISTING_VINES_PLANT, SCAFFOLDING, CAKE, SHULKER_BOX,
            BLUE_SHULKER_BOX,
            BLACK_SHULKER_BOX,
            YELLOW_SHULKER_BOX,
            BROWN_SHULKER_BOX,
            LIME_SHULKER_BOX,
            GRAY_SHULKER_BOX,
            LIGHT_GRAY_SHULKER_BOX,
            MAGENTA_SHULKER_BOX,
            GREEN_SHULKER_BOX,
            WHITE_SHULKER_BOX,
            ORANGE_SHULKER_BOX,
            PINK_SHULKER_BOX,
            PURPLE_SHULKER_BOX,
            CYAN_SHULKER_BOX,
            LIGHT_BLUE_SHULKER_BOX
    ); // TODO WATER, LAVA - add placing block removes water/lava
    public static final EnumSet<Material> EVENT_NON_COMBAT_PLACEABLE_BLOCKS = EnumSet.of(COBWEB, ROSE_BUSH, LADDER, VINE, RAIL, POWERED_RAIL, DETECTOR_RAIL, ACTIVATOR_RAIL, TRIPWIRE); // TODO  water and lava, maybe no spread

    public static final int FAST_ITEM_DESPAWN_TICKS_LIVED = 4800;
    public static final int CLAIM_BLOCK_TIMER = (6000 - FAST_ITEM_DESPAWN_TICKS_LIVED) / 20;

    public static final BlockData SPAWN_GLASS_BLOCK_DATA = Bukkit.createBlockData(Material.RED_STAINED_GLASS);

    public static final EnumSet<Material> SHULKER_BOXES = EnumSet.of(
            RED_SHULKER_BOX,
            SHULKER_BOX,
            BLUE_SHULKER_BOX,
            BLACK_SHULKER_BOX,
            YELLOW_SHULKER_BOX,
            BROWN_SHULKER_BOX,
            LIME_SHULKER_BOX,
            GRAY_SHULKER_BOX,
            LIGHT_GRAY_SHULKER_BOX,
            MAGENTA_SHULKER_BOX,
            GREEN_SHULKER_BOX,
            WHITE_SHULKER_BOX,
            ORANGE_SHULKER_BOX,
            PINK_SHULKER_BOX,
            PURPLE_SHULKER_BOX,
            CYAN_SHULKER_BOX,
            LIGHT_BLUE_SHULKER_BOX
    );
}
