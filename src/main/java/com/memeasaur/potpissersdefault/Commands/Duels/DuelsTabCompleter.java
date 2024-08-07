package com.memeasaur.potpissersdefault.Commands.Duels;

import com.memeasaur.potpissersdefault.Classes.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.memeasaur.potpissersdefault.PotpissersDefault.immutableDefaultKitNamesCache;
import static com.memeasaur.potpissersdefault.PotpissersDefault.playerDataCache;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.ATTACK_SPEED_VALUES;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods1.getOnlinePlayerNamesList;

public class DuelsTabCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof Player p) {
            switch (command.getName().toLowerCase()) {
                case "duel" -> {
                    switch (strings.length) {
                        case 1 -> {
                            return getOnlinePlayerNamesList();
                        }
                        case 2 -> {
                            return Stream.concat(immutableDefaultKitNamesCache.stream(), ATTACK_SPEED_VALUES.keySet().stream()).toList();
                        }
                        case 3 -> { // TODO -> impl arenas auto-complete
                            if (immutableDefaultKitNamesCache.contains(strings[1]))
                                return new ArrayList<>(ATTACK_SPEED_VALUES.keySet());
                            else if (ATTACK_SPEED_VALUES.containsKey(strings[1]))
                                return immutableDefaultKitNamesCache;
                        }
                    }
                }
                case "accept" -> {
                    if (strings.length == 1) {
                        PlayerData data = playerDataCache.get(p);
                        return playerDataCache.values().stream()
                                .filter(dataIteration -> dataIteration.outgoingDuelRequestsContains(data))
                                .map(dataIteration -> Bukkit.getOfflinePlayer(dataIteration.uuid).getName())
                                .toList();
                    }
                }
                case "anon" -> {}
            }
        }
        return List.of();
    }
}
