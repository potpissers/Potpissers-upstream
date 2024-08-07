package com.memeasaur.potpissersdefault.Classes;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.ATTACK_SPEED_VALUES;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.RANDOM;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getNormalComponent;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Postgres.Methods.fetchDefaultKitContents;

public class DuelTracker extends StatTracker {
    public final DuelOptions duelOptions;
    public int crappleCd;
    public int oppleCd;
    public int sharpnessLimit;
    public int protectionLimit;
    public int powerLimit;
    public int strengthLimit;
    public int regenLimit;

    public final HashMap<UUID, String> alivePlayers = new HashMap<>();
    public final Map<UUID, String> players; // TODO IMPL SPECTATOR

    public DuelTracker(DuelOptions finalDuelOptions, Map<UUID, String> players) {
        this.players = players;
        this.duelOptions = finalDuelOptions;

        Component vsComponent = getNormalComponent("duel started with ");

        ArrayList<Location> possibleArenaWarps = arenaWarps.get(finalDuelOptions.arenaName());
        ArrayList<Location> unusedLocations = (ArrayList<Location>) possibleArenaWarps.clone(); // TODO -> improve

        Set<String> teamNames = Set.copyOf(players.values());
        Map<String, Location> teamWarpLocations = teamNames.stream().collect(Collectors.toMap(teamName -> teamName,teamName -> {
            Location location = !unusedLocations.isEmpty() ?
                    unusedLocations.get(RANDOM.nextInt(unusedLocations.size())) :
                    possibleArenaWarps.get(RANDOM.nextInt(possibleArenaWarps.size()));

            unusedLocations.remove(location);
            return location;
        }));
        Map<String, Component> teamMsgComponents = teamNames.stream()
                .collect(Collectors.toMap(teamName -> teamName,teamName -> vsComponent
                        .append(teamNames.stream()
                        .filter(name -> !name.equals(teamName))
                        .map(Component::text)
                        .reduce((current, next) -> current.equals(Component.empty()) ? next : current.append(Component.text(", ")).append(next))
                        .orElse(Component.empty()))));

        for (Map.Entry<UUID, String> entry : players.entrySet()) {
            UUID uuid = entry.getKey();
            String team = entry.getValue();
            Location teamWarpLocation = teamWarpLocations.get(team);
            if (Bukkit.getPlayer(uuid) instanceof Player player) {
                PlayerData playerData = playerDataCache.get(player);
                playerData.handleQueueCancel();
                alivePlayers.put(uuid, team);
                player.setHealth(20);
                player.clearActivePotionEffects();
                player.setFoodLevel(19);
                player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(ATTACK_SPEED_VALUES.get(finalDuelOptions.attackSpeedName()));

                player.setVisibleByDefault(false);
                for (UUID uuidIteration : players.keySet())
                    Bukkit.getPlayer(uuidIteration).showPlayer(plugin, player);

                player.teleport(teamWarpLocation); // TODO -> get random location. make the warp always be facing each other

                fetchDefaultKitContents(finalDuelOptions.kitName())
                        .thenAccept(kitContents -> player.getInventory().setContents(kitContents));
                player.sendMessage(teamMsgComponents.get(team));

                new PlayerFightTracker(playerData, null, this, player); // TODO -> store team as party (?)
            }
        }
    }

    @Override
    public void handleJoin(PlayerFightTracker playerFightTracker, AbstractPartyData abstractPartyData, Player player) {
        playerFightTracker.statTracker = this;
        fightData.add(playerFightTracker);
    }
}
