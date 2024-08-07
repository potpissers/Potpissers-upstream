package com.memeasaur.potpissersdefault.Commands.Party;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods1.getOnlinePlayerNamesList;

public class PartyTabCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof Player) {
            switch (command.getName().toLowerCase()) {
                case "party" -> {
                    return handlePartyCompletes(strings);
                }
            }
        }
        return List.of();
    }
    List<String> handlePartyCompletes(String[] strings) {
        switch (strings.length) {
            case 1 -> {
                return List.of("create"
                        , "disband"
                        , "invite"
                        , "uninvite"
                        , "kick"
                        , "join"
                        , "leave"
                        , "officer"
                        , "demote"
                        , "ally"
                        , "enemy"
                        , "neutral"
                        , "focus"
                        , "leader"
                        , "rally"
                        , "warp"
                        , "setwarp");
            }
            case 2 -> {
                switch (strings[0]) {
                    case "invite" -> {
                        return getOnlinePlayerNamesList();
                        // TODO subtract current members
                    }
                    case "kick", "officer", "demote", "leader" -> {
                    }
                    case "uninvite" -> {
                    }
                }
            }
        }
        return List.of();
    }
}
