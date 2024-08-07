package com.memeasaur.potpissersdefault.Classes;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.ATTACK_SPEED_VALUES;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.RANDOM;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Postgres.Methods.executeArenaNamesCacheUpdate;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Postgres.Methods.executeDefaultKitNamesCacheUpdate;

public record DuelOptions(String kitName, String attackSpeedName, String arenaName) {
    public static DuelOptions getNullableDuelOptionsOfArgs(String[] args) {
        executeDefaultKitNamesCacheUpdate();
        executeArenaNamesCacheUpdate();

        String attackSpeedName = defaultAttackSpeedName;
        String kitName = defaultKitName;
        String arenaName = null;
        for (String arg : args) {
            if (immutableDefaultKitNamesCache.contains(arg))
                kitName = arg;
            else if (arenaNamesCache.contains(arg))
                arenaName = arg;
            else if (ATTACK_SPEED_VALUES.containsKey(arg))
                attackSpeedName = arg;
            else
                return null;
        }
        return new DuelOptions(kitName, attackSpeedName, arenaName);
    }
    public DuelOptions getNullableFinalDuelOptions(DuelOptions duelOptions) {
        executeDefaultKitNamesCacheUpdate();

        if ((kitName != null && duelOptions.kitName != null && !kitName.equals(duelOptions.kitName)) || (attackSpeedName != null && duelOptions.attackSpeedName != null && !attackSpeedName.equals(duelOptions.attackSpeedName)) || (arenaName != null && duelOptions.arenaName != null && !arenaName.equals(duelOptions.arenaName)))
            return null;
        else
            return new DuelOptions(
                    kitName != null ? kitName : duelOptions.kitName() != null ? duelOptions.kitName() : immutableDefaultKitNamesCache.get(RANDOM.nextInt(immutableDefaultKitNamesCache.size())),
                    attackSpeedName != null ? attackSpeedName : duelOptions.attackSpeedName != null ? duelOptions.attackSpeedName : ATTACK_SPEED_VALUES.keySet().stream().skip(RANDOM.nextInt(ATTACK_SPEED_VALUES.size())).findAny().orElse(null),
                    arenaName != null ? arenaName : duelOptions.arenaName != null ? duelOptions.arenaName : arenaNamesCache.get(RANDOM.nextInt(arenaNamesCache.size())));
    }
}
