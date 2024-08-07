package com.memeasaur.potpissersdefault.Classes;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getConsoleComponent;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getNormalComponent;

public class FightTracker extends StatTracker {
    // TODO -> disabling fight-protection publishes fight coords in chat (?)
    public AbstractPartyData mainAbstractPartyData1;
    public final Set<PlayerFightTracker> party1Allies = Collections.newSetFromMap(new WeakHashMap<>());
    public AbstractPartyData mainAbstractPartyData2;
    public final Set<PlayerFightTracker> party2Allies = Collections.newSetFromMap(new WeakHashMap<>());

    final WeakHashMap<AbstractPartyData, WeakHashMap<Player, PlayerFightTracker>> players = new WeakHashMap<>();

    public FightTracker(Player p1, PlayerData data1, Player p2, PlayerData data2) {
        new PlayerFightTracker(data1, data1.getActiveMutableParty()[0], this, p1);
        new PlayerFightTracker(data2, data2.getActiveMutableParty()[0], this, p2);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (players.isEmpty() ||
                        (players.size() == 1 &&
                                (!(players.get(null) instanceof WeakHashMap weakHashMap) || weakHashMap.size() == 1))) {
                    players.values().stream().findAny()
                            .ifPresent(lastMap -> lastMap.keySet()
                                    .forEach(playerIteration ->
                                            playerDataCache.get(playerIteration).playerFightTracker = null));

                    StringBuilder fightTitle = new StringBuilder("- fight ended: ");
                    // TODO -> construction

                    ArrayList<PlayerFightTracker> damageDealtLeaderboard = (ArrayList<PlayerFightTracker>) fightData.clone();
                    damageDealtLeaderboard.sort((a, b) -> Double.compare(
                            b.victimStats
                                    .values()
                                    .stream()
                                    .flatMap(stats -> stats.values().stream())
                                    .mapToDouble(entry -> entry.damageDealt)
                                    .sum(),
                            a.victimStats
                                    .values()
                                    .stream()
                                    .flatMap(stats -> stats.values().stream())
                                    .mapToDouble(entry -> entry.damageDealt)
                                    .sum()));

                    Component fightEndData = getNormalComponent(fightTitle.toString())
                            .appendNewline() // TODO -> more data (?) + this doesn't combine entries like it should
                            .append(getConsoleComponent("damage dealt: "))
                            .append(getConsoleComponent("1. " + Bukkit.getOfflinePlayer(damageDealtLeaderboard.get(0).uuid).getName()))
                            .append(getConsoleComponent("2. " + Bukkit.getOfflinePlayer(damageDealtLeaderboard.get(1).uuid).getName()));

                    for (UUID uuid : fightData.stream().map(playerFightTracker -> playerFightTracker.uuid).collect(Collectors.toSet())) {
                        if (Bukkit.getPlayer(uuid) instanceof Player player)
                            player.sendMessage(fightEndData);
                    } // TODO -> web-link + chat link to public the results

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        p1.sendMessage("fight started");
        p2.sendMessage("fight started");
    }
    public void handleMerge(FightTracker fightTracker) {
        this.fightData.addAll(fightTracker.fightData);

        fightTracker.players.forEach((party, map) -> players.merge(party, map, (v1, v2) -> {
            v2.forEach((key, value) ->
                    value.statTracker = this);

            v1.putAll(v2);
            return v1;
        }));

        handleMainTeams();
    }
    void handleMainTeams() {
        for (Map.Entry<AbstractPartyData, WeakHashMap<Player, PlayerFightTracker>> entry : players.entrySet())
            if (entry.getKey() instanceof AbstractPartyData abstractPartyData) {
                if (mainAbstractPartyData1 == null) {
                    mainAbstractPartyData1 = abstractPartyData;
                    party1Allies.clear();
                }
                else if (entry.getValue().size() > players.get(mainAbstractPartyData1).size()) {
                    if (!mainAbstractPartyData1.allyCache.contains(abstractPartyData.uuid)) {
                        mainAbstractPartyData2 = mainAbstractPartyData1;
                        party2Allies.clear();
                    }
                    mainAbstractPartyData1 = abstractPartyData;
                    party1Allies.clear();
                }
                else if (mainAbstractPartyData2 == null ||
                        (!abstractPartyData.allyCache.contains(mainAbstractPartyData1.uuid) && entry.getValue().size() > players.get(mainAbstractPartyData2).size())) {
                    mainAbstractPartyData2 = abstractPartyData;
                    party2Allies.clear();
                }
            }
    }
    public int getTeamSize(AbstractPartyData abstractPartyData) {
        if (abstractPartyData == null)
            return 1;
        else
            return players.get(abstractPartyData).size();
    }
    public boolean isOutnumbered(AbstractPartyData abstractPartyData, PlayerFightTracker playerFightTracker) {
        if (mainAbstractPartyData1 == abstractPartyData || party1Allies.contains(playerFightTracker))
            return players.get(mainAbstractPartyData1).size() + party1Allies.size() < players.get(mainAbstractPartyData2).size() + party2Allies.size();
        else if (mainAbstractPartyData2 == abstractPartyData || party2Allies.contains(playerFightTracker))
            return players.get(mainAbstractPartyData2).size() + party2Allies.size() < players.get(mainAbstractPartyData1).size() + party1Allies.size();
        else // TODO -> this results in only the highest player count fight being protected (?)
            return false; // ?
    }
    public boolean handleIsAllyForceable(PlayerData data1, PlayerData data) {
        if (data1.getActiveMutableParty()[0] == mainAbstractPartyData1 || party1Allies.contains(data1.playerFightTracker))
            return mainAbstractPartyData2.allyCache.contains(data.playerFightTracker.partyUid) && party2Allies.stream().anyMatch(allyFightTracker -> {
                if (!mainAbstractPartyData2.allyCache.contains(allyFightTracker.partyUid)) {
                    party2Allies.remove(allyFightTracker);
                    party2Allies.add(data.playerFightTracker);
                    return true;
                }
                else
                    return false;
            });
        else // TODO -> methodize
            return mainAbstractPartyData1.allyCache.contains(data.playerFightTracker.partyUid) && party1Allies.stream().anyMatch(allyFightTracker -> {
                if (!mainAbstractPartyData1.allyCache.contains(allyFightTracker.partyUid)) {
                    party1Allies.remove(allyFightTracker);
                    party1Allies.add(data.playerFightTracker);
                    return true;
                }
                else
                    return false;
            });
    }
    public boolean handleIsFighterProtected(PlayerData data1, PlayerData data) {
        if (data1.getActiveMutableParty()[0] == mainAbstractPartyData1 || party1Allies.contains(data1.playerFightTracker)) {
            if (data.getActiveMutableParty()[0] == mainAbstractPartyData2 || data.getActiveMutableParty()[0] == mainAbstractPartyData1 || party2Allies.contains(data.playerFightTracker))
                return false;
            else if (party1Allies.contains(data.playerFightTracker)) {
                if (data.playerFightTracker.partyDamage.getOrDefault(mainAbstractPartyData1, 0D) > data.playerFightTracker.partyDamage.get(mainAbstractPartyData2)) {
                    party1Allies.remove(data.playerFightTracker);
                    return true;
                }
                else
                    return false;
            }
            else
                return true;
        } // TODO -> methodize
        else if (data1.getActiveMutableParty()[0] == mainAbstractPartyData2 || party2Allies.contains(data1.playerFightTracker)) {
            if (data.getActiveMutableParty()[0] == mainAbstractPartyData1 || data.getActiveMutableParty()[0] == mainAbstractPartyData2 || party1Allies.contains(data.playerFightTracker))
                return false;
            else if (party2Allies.contains(data.playerFightTracker)) {
                if (data.playerFightTracker.partyDamage.getOrDefault(mainAbstractPartyData2, 0D) > data.playerFightTracker.partyDamage.get(mainAbstractPartyData1)) {
                    party1Allies.remove(data.playerFightTracker);
                    return true;
                }
                else
                    return false;
            }
            else
                return true;
        }
        else
            return false;
    }

    @Override
    public void handleJoin(PlayerFightTracker playerFightTracker, AbstractPartyData abstractPartyData, Player player) {
        playerFightTracker.statTracker = this;
        fightData.add(playerFightTracker);

        if (players.get(abstractPartyData) instanceof WeakHashMap<Player, PlayerFightTracker> weakHashMap)
            weakHashMap.put(player, playerFightTracker);
        else
            players.put(abstractPartyData, new WeakHashMap<>(Collections.singletonMap(player, playerFightTracker)));

        handleMainTeams();
    }
}