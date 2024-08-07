package com.memeasaur.potpissersdefault.Commands.Claims;

import com.memeasaur.potpissersdefault.Classes.ClaimCoordinate;
import com.memeasaur.potpissersdefault.Classes.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.Types;
import java.util.ArrayList;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Methods.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Constants.DATA_ARENAS;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.*;

public class ClaimsOpCommands implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof Player p) {
            switch (command.getName().toLowerCase()) {
                case "claimspawn" -> {
                    if (strings.length != 0) {
                        p.sendMessage("?");
                        return true;
                    }
                    PlayerData data = playerDataCache.get(p);
                    handleOpClaimArea(p, data.claimPos1, data.claimPos2, SPAWN_CLAIM);
                    return true;
                }
                case "unclaimarea" -> {
                    if (strings.length != 0)
                        p.sendMessage("?");
                    PlayerData data = playerDataCache.get(p);
                    if (data.claimPos1 == null || data.claimPos2 == null) {
                        p.sendMessage("?");
                        return true;
                    }
                    else if (!data.claimPos1.getWorld().equals(data.claimPos2.getWorld())) {
                        p.sendMessage("invalid (claim worlds)");
                        return true;
                    }
                    int counter = doUnclaimIterationReturnCounter(new ClaimCoordinate(data.claimPos1), new ClaimCoordinate(data.claimPos2), data.currentClaim);
                    p.sendMessage("unclaimed, " + counter + " " + data.currentClaim + " claims");
                    handleClaimsSave();
                    return true;
                }
                case "unclaimall" -> {
                    if (strings.length != 0)
                        p.sendMessage("?");
                    else if (playerDataCache.get(p).currentClaim instanceof Integer postgresId)
                        p.sendMessage("?");
                    else {
                        Object claim = playerDataCache.get(p).currentClaim;
                        if (claim.equals(WILDERNESS_CLAIM)) {
                            p.sendMessage("?");
                            return true;
                        }
                        doObjectUnclaim(claim);
                    }
                    return true;
                }
                case "claimarena" -> {
                    if (strings.length != 2) {
                        p.sendMessage("?");
                        return true;
                    }
                    fetchPgCallNonnullT("{? = call upsert_server_arena_return_id(?, ?, ?)}", Types.INTEGER, new Object[]{strings[0], strings[1], POSTGRESQL_SERVER_ID}, Integer.class)
                            .thenAccept(id -> {
                                if (claims.containsValue(id))
                                    p.sendMessage("invalid (claim exists)");
                                else {
                                    PlayerData data = playerDataCache.get(p);
                                    handleOpClaimArea(p, data.claimPos1, data.claimPos2, id);
                                }
                            });
                    return true;
                }
                case "unclaimarena" -> {
                    if (strings.length != 1)
                        p.sendMessage("?");
                    else
                        fetchPgCallOptionalT("{? = call delete_server_arena_return_id(?, ?)}", Types.INTEGER, new Object[]{strings[0], POSTGRESQL_SERVER_ID}, Integer.class)
                                .thenAccept(optionalId -> {
                                    if (optionalId.isEmpty())
                                        p.sendMessage("?");
                                    else
                                        doObjectUnclaim(optionalId.get());
                                });
                    return true;
                }
                case "addarenawarp" -> {
                    if (strings.length != 1)
                        p.sendMessage("?");
                    else
                        fetchPgCallNonnullT("{? = call get_server_arena_name_exists(?, ?)}", Types.BOOLEAN, new Object[]{strings[0], POSTGRESQL_SERVER_ID}, Boolean.class)
                                .thenAccept(exists -> {
                                    if (!exists)
                                        p.sendMessage("invalid (arena)");
                                    else {
                                        arenaWarps.putIfAbsent(strings[0], new ArrayList<>());
                                        arenaWarps.get(strings[0]).add(p.getLocation());
                                        handleBlockingFileSerialization("arenas", writeBukkitBinaryFile(DATA_ARENAS, arenaWarps));
                                    }
                                });
                    return true;
                }
                case "claimset1" -> {
                    if (strings.length != 0)
                        p.sendMessage("?");
                    doClaimPosSet(p, playerDataCache.get(p).claimPos1);
                    return true;
                }
                case "claimset2" -> {
                    if (strings.length != 0)
                        p.sendMessage("?");
                    doClaimPosSet(p, playerDataCache.get(p).claimPos2);
                    return true;
                }
            }
        }
        return false;
    }
}
