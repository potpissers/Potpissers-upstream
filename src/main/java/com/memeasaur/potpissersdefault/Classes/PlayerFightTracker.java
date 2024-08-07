package com.memeasaur.potpissersdefault.Classes;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants.LogoutTeleport.TAG_LOGOUT_TIMER;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getConsoleComponent;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.fetchPgCallNonnullT;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.fetchSqliteNonnullT;

public class PlayerFightTracker {
    public final UUID uuid;
    public final UUID partyUid; // TODO -> store duel team (?)

    public transient StatTracker statTracker;
    public int fightTrackerTimer;

    public WeakHashMap<AbstractPartyData, Double> partyDamage = new WeakHashMap<>();

    public final OffsetDateTime joinedTimestamp = OffsetDateTime.now();
    public OffsetDateTime leftTimestamp;

    public HashMap<UUID, HashMap<UUID, PvpStats>> victimStats = new HashMap<>();
    public int gapplesConsumed;
    public double instantHealthConsumed;
    public int movementCooldowns;
    public int splashesConsumed;

    public PlayerFightTracker(PlayerData data, AbstractPartyData abstractPartyData, StatTracker statTracker, Player player) {
        this.uuid = data.uuid;
        this.partyUid = abstractPartyData != null ? abstractPartyData.uuid : null;
        data.playerFightTracker = this;

        statTracker.handleJoin(this, abstractPartyData, player);
        if (statTracker instanceof FightTracker fightTracker) {
            this.fightTrackerTimer = TAG_LOGOUT_TIMER;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (fightTrackerTimer > 0 || data.playerFightTracker == null)
                        fightTrackerTimer--;

                    if (fightTrackerTimer == 0) {
                        handleFightTrackerExpiration(data, player, fightTracker, abstractPartyData);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 20L, 20L);
        }
    }
    public CompletableFuture<Integer> handleFightTrackerExpiration(PlayerData data, @Nullable Player player, FightTracker fightTracker, AbstractPartyData abstractPartyData) {
        if (data.playerFightTracker == this) {
            if (player != null) {
                player.sendMessage(getConsoleComponent("- fightTracker expired")
                        .appendNewline());
            } // TODO calculate damage dealt + taken + link

            data.playerFightTracker = null;

            fightTracker.players.get(abstractPartyData).remove(player);
            if (fightTracker.players.get(abstractPartyData).isEmpty())
                fightTracker.players.remove(abstractPartyData);
            fightTracker.party1Allies.remove(this);
            fightTracker.party2Allies.remove(this);

            return fetchPgCallNonnullT("{? = call insert_user_fight_return_id(?, ?)}", Types.INTEGER, new Object[]{fightTracker.uuid, POSTGRESQL_SERVER_ID}, Integer.class);
        }
        else
            return CompletableFuture.completedFuture(null);
    }
}
