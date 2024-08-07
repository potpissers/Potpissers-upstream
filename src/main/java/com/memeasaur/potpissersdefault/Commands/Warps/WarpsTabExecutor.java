package com.memeasaur.potpissersdefault.Commands.Warps;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.memeasaur.potpissersdefault.PotpissersDefault.publicWarps;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods1.getOnlinePlayerNamesList;

public class WarpsTabExecutor implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof Player p) {
            switch (command.getName().toLowerCase()) {
                case "warp" -> {
                    if (strings.length == 1)
                        return List.of(publicWarps.keySet().toArray(new String[0]));
                }
                case "tpa, tpahere" -> {
                    if (strings.length == 1) {
                        return getOnlinePlayerNamesList();
                    }
                }
            }
        }
        return List.of();
    }
}
