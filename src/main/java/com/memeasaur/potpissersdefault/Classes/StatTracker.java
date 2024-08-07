package com.memeasaur.potpissersdefault.Classes;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

public abstract class StatTracker {
    public abstract void handleJoin(PlayerFightTracker playerFightTracker, AbstractPartyData abstractPartyData, Player player);
    public final UUID uuid = UUID.randomUUID();
    public final ArrayList<PlayerFightTracker> fightData = new ArrayList<>();
}