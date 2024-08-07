package com.memeasaur.potpissersdefault.Util.Potpissers.Constants;

import org.bukkit.potion.PotionType;

import java.util.EnumSet;

import static org.bukkit.potion.PotionType.*;

public class Potion {
    public static final EnumSet<PotionType> NOT_NERFED_SPLASH_EFFECTS = EnumSet.of(PotionType.HEALING, PotionType.STRONG_HEALING, PotionType.HARMING, PotionType.STRONG_HARMING);

    // Combat tag start
    public static final EnumSet<PotionType> DEBUFF_EFFECTS = EnumSet.of(HARMING, STRONG_HARMING, LONG_POISON, LONG_SLOW_FALLING, LONG_WEAKNESS, POISON, SLOW_FALLING, STRONG_POISON, WEAKNESS, STRONG_SLOWNESS, LONG_SLOWNESS, SLOWNESS);
    // Combat tag end
}
