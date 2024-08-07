package com.memeasaur.potpissersdefault.Commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getFocusComponent;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods1.getPotpissersCommandReason;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.fetchQueryVoid;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Postgres.Methods.fetchInsertReviveVoid;

public class DeathbanOpCommands implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        switch (command.getName().toLowerCase()) {
            case "setglobaldeathbanlength" -> {
                if (strings.length == 0)
                    commandSender.sendMessage("?");
                else {
                    int value = Integer.parseInt(strings[0]);
                    fetchQueryVoid(POSTGRES_POOL, "call update_server_death_ban_minutes(?, ?)", new Object[]{value, POSTGRESQL_SERVER_ID})
                            .thenRun(() ->
                                    Bukkit.broadcast(getFocusComponent("the max deathban duration has been updated to " + value + " minutes")));
                }
                return true;
            }
            case "oprevive" -> {
                OfflinePlayer offlinePlayerArg = Bukkit.getOfflinePlayer(strings[0]);
                if (strings.length < 2)
                    commandSender.sendMessage("?");
                else if (offlinePlayerArg.getName() == null)
                    commandSender.sendMessage("?");
                else {
                    String reason = getPotpissersCommandReason(strings);
                    fetchInsertReviveVoid(offlinePlayerArg.getUniqueId(), reason, commandSender instanceof Player player ? player.getUniqueId() : null)
                            .thenRun(() -> Bukkit.broadcast(getFocusComponent( commandSender.getName() + " has revived " + offlinePlayerArg.getName() + ", reason: " + reason)));
                }
                return true;
            }
        }
        return false;
    }
}
