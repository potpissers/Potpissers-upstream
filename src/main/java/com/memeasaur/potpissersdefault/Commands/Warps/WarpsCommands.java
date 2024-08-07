package com.memeasaur.potpissersdefault.Commands.Warps;

import com.memeasaur.potpissersdefault.Classes.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static com.memeasaur.potpissersdefault.Commands.PotpissersCommands.handleLogoutTeleport;
import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants.LogoutTeleport.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.*;

public class WarpsCommands implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof Player p) {
            if (!IS_KIT_SERVER)
                p.sendMessage("invalid (disabled)");
            else
                switch (command.getName().toLowerCase()) {
                    case "spawn" -> {
                        if (strings.length != 0)
                            p.sendMessage(getConsoleComponent("?"));
                        PlayerData data = playerDataCache.get(p);
                        data.logoutTeleportString = "Spawn teleport in ";
                        handleLogoutTeleport(p, data, STRING_SPAWN, Bukkit.getWorld("world").getSpawnLocation());
                    }
                    case "warp" -> {
                        if (strings.length == 0) {
                            StringBuilder stringBuilder = new StringBuilder().append("warps: ");
                            for (String string : publicWarps.keySet())
                                stringBuilder.append(string).append(" ");
                            p.sendMessage(getNormalComponent(stringBuilder.toString()));
                            return true;
                        }
                        if (strings.length > 1)
                            p.sendMessage("?");
                        if (!(publicWarps.getOrDefault(strings[0], null) instanceof Location location))
                            p.sendMessage("invalid (strings)");
                        else
                            handleLogoutTeleport(p, playerDataCache.get(p), STRING_WARP, location);
                        return true;
                    }
                    case "tpa" -> {
                        if (strings.length == 0)
                            p.sendMessage(getNormalComponent("warps you to player arg. usage: /tpa (player)"));
                        else if (!(Bukkit.getPlayer(strings[0]) instanceof Player pArg))
                            p.sendMessage(getNormalComponent("invalid (player)"));
                        else
                            playerDataCache.get(p).handleRequestTpa(p, pArg, playerDataCache.get(pArg));
                    }
                    case "tpahere" -> {
                        if (strings.length == 0)
                            p.sendMessage(getNormalComponent("warps player arg to you. usage: /tpahere (player)"));
                        else if (!(Bukkit.getPlayer(strings[0]) instanceof Player pArg))
                            p.sendMessage(getNormalComponent("invalid (player)"));
                        else
                            playerDataCache.get(p).handleRequestTpaHere(p, pArg, playerDataCache.get(pArg));
                    }
                }
        }
        return false;
    }
}
