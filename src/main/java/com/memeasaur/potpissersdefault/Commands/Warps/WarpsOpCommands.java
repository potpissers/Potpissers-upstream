package com.memeasaur.potpissersdefault.Commands.Warps;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Constants.DATA_PRIVATE_WARPS;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Constants.DATA_PUBLIC_WARPS;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.handleBlockingFileSerialization;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.writeBukkitBinaryFile;

public class WarpsOpCommands implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof Player p) {
            switch (command.getName().toLowerCase()) {
                case "addwarp", "setwarp", "createwarp" -> {
                    if (strings.length != 1) {
                        p.sendMessage("?");
                        return true;
                    }
                    publicWarps.put(strings[0], p.getLocation());
                    p.sendMessage(strings[0] + " warp set");
                    handleBlockingFileSerialization("publicWarps", writeBukkitBinaryFile(DATA_PUBLIC_WARPS, publicWarps));
                    return true;
                }
                case "removewarp", "unsetwarp", "deletewarp" -> {
                    if (strings.length != 1) {
                        p.sendMessage("?");
                        return true;
                    }
                    if (!publicWarps.containsKey(strings[0])) {
                        p.sendMessage("?");
                        return true;
                    }
                    publicWarps.remove(strings[0]);
                    p.sendMessage(strings[0] + " warp removed");
                    handleBlockingFileSerialization("publicWarps", writeBukkitBinaryFile(DATA_PUBLIC_WARPS, publicWarps));
                    return true;
                }
                case "addprivatewarp" -> {
                    if (strings.length != 1) {
                        p.sendMessage("?");
                        return true;
                    }
                    if (privateWarps.containsKey(strings[0])) {
                        p.sendMessage("?");
                        return true;
                    }
                    privateWarps.put(strings[0], p.getLocation());
                    p.sendMessage(strings[0] + " warp set");
                    handleBlockingFileSerialization("privateWarps", writeBukkitBinaryFile(DATA_PRIVATE_WARPS, privateWarps));
                    return true;
                }
                case "removeprivatewarp" -> {
                    if (strings.length != 1) {
                        p.sendMessage("?");
                        return true;
                    }
                    if (!privateWarps.containsKey(strings[0])) {
                        p.sendMessage("?");
                        return true;
                    }
                    privateWarps.remove(strings[0]);
                    p.sendMessage(strings[0] + " warp removed");
                    handleBlockingFileSerialization("privateWarps", writeBukkitBinaryFile(DATA_PRIVATE_WARPS, privateWarps));
                    return true;
                }
                case "privatewarp" -> {
                    if (strings.length == 0) {
                        StringBuilder stringBuilder = new StringBuilder().append("privatewarps: ");
                        for (String string : privateWarps.keySet()) {
                            stringBuilder.append(string);
                            stringBuilder.append(" ");
                        }
                        p.sendMessage(stringBuilder.toString());
                        return true;
                    }
                    if (strings.length != 1) {
                        p.sendMessage("invalid (strings)");
                        return true;
                    }
                    if (!privateWarps.containsKey(strings[0])) {
                        p.sendMessage("invalid (warp)");
                        return true;
                    }
                    p.teleport(privateWarps.get(strings[0]));
                    return true;
                }
            }
        }
        return false;
    }
}
