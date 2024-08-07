package com.memeasaur.potpissersdefault.Commands.Party;

import com.memeasaur.potpissersdefault.Classes.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.memeasaur.potpissersdefault.Classes.DuelOptions.getNullableDuelOptionsOfArgs;
import static com.memeasaur.potpissersdefault.Classes.NetworkPartyData.*;
import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.SPAWN_CLAIM;
import static com.memeasaur.potpissersdefault.Util.Combat.Methods.getLocationString;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods1.executeNetworkPlayerMessage;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods1.sendPotpissersPluginMessage;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.handlePotpissersExceptions;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.*;

public class PartyCommands implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player p) {
            PlayerData data = playerDataCache.get(p);
            switch (command.getName().toLowerCase()) {
                case "chat" ->
                        handleChatCommand(args.length > 0 ? args[0] : null, p, data);
                case "rally" ->
                        data.getCurrentParties().forEach(party ->
                                handleRallyCommand(args.length > 0 ? args[0] : null, p, party)); // TODO -> first activeMutableParty (?))
                case "caution" ->
                        data.getCurrentParties().forEach(party ->
                                handleCautionCommand(p, party));
                case "focus" ->
                        data.getCurrentParties().forEach(party ->
                                handleFocusCommand(args.length > 0 ? args[0] : null, party, p));
                case "unfocus" ->
                        data.getCurrentParties().forEach(party ->
                                handleUnfocusCommand(args.length >= 1 ? args[0] : null, party, p));
                case "duel" -> {
                    if (!IS_KIT_SERVER)
                        p.sendMessage("invalid (disabled)");
                    else
                        handleDuelCommand(args, p, data);
                }
                case "anon" -> {
                    if (!IS_KIT_SERVER)
                        p.sendMessage("invalid (disabled)");
                    else if (!data.currentClaim.equals(SPAWN_CLAIM))
                        p.sendMessage("invalid (spawn)");
                    else if (!(getNullableDuelOptionsOfArgs(args) instanceof DuelOptions duelOptions))
                        p.sendMessage("invalid (args)");
                    else {
                        for (Map.Entry<PlayerData, DuelOptions> entry : playerDuelQueue.sequencedEntrySet())
                            if (data != entry.getKey() && duelOptions.getNullableFinalDuelOptions(entry.getValue()) instanceof DuelOptions finalDuelOptions) {
                                data.executeHandleDuel(entry.getKey(), finalDuelOptions);
                                return true;
                            }
                        // else
                        playerDuelQueue.putLast(data, duelOptions);
                        Bukkit.broadcast(getNormalComponent("? has used /anon to enter queue with kit: " + duelOptions.kitName() + ", attack speed: " + duelOptions.attackSpeedName() + ", arena: " + duelOptions.arenaName()));
                    }
                }
                case "dequeue" -> {
                    if (args.length > 0)
                        p.sendMessage("?");
                    data.handleQueueCancel();
                }
                case "party" ->
                {
                    if (args.length == 0) // TODO GUI? or just f who
                        p.sendMessage("PARTY_USAGE");
                    else {
                        AbstractPartyData[] mutableAbstractPartyData =
                        switch (command.getName().toLowerCase()) {
                            case "party" -> data.mutableNetworkParty;
                            default -> {
                                RuntimeException e = new RuntimeException();
                                handlePotpissersExceptions(null, e);
                                throw e;
                            }
                        };
                        switch (args[0]) {
                            case "create" -> {
                                if (command.getName().equalsIgnoreCase("party")) {
                                    if (args.length != 1)
                                        p.sendMessage("?");
                                    if (mutableAbstractPartyData[0] != null)
                                        p.sendMessage("invalid (partied)");
                                    else
                                        fetchHandleNewNetworkPartyVoid(data, p);
                                }
                                else {
                                    RuntimeException runtimeException = new RuntimeException();
                                    handlePotpissersExceptions(null, runtimeException);
                                    throw runtimeException;
                                }
                            }
                            case "disband", "delete" -> {
                                if (args.length != 1)
                                    p.sendMessage("PARTY_USAGE");
                                else if (!(mutableAbstractPartyData[0] instanceof AbstractPartyData abstractPartyData))
                                    p.sendMessage(getNormalComponent("invalid (party)"));
                                else {
                                    CompletableFuture<String> futurePartyName = fetchNonnullPartyName(abstractPartyData.uuid);
                                    abstractPartyData.fetchNonnullPlayerRankName(data.uuid)
                                            .thenAccept(playerRankName -> {
                                                if (!playerRankName.equals(PARTY_RANK_LEADER))
                                                    executeNetworkPartyMessage(abstractPartyData.uuid, futurePartyName, getNormalComponent(p.getName() + " has tried to use /disband"));
                                                else
                                                    fetchNonnullPartyName(abstractPartyData.uuid)
                                                            .thenAccept(name -> {
                                                                executeNetworkPartyMessage(abstractPartyData.uuid, futurePartyName, getNormalComponent(name + " disbanded by " + p.getName()));
                                                                abstractPartyData.fetchDisbandVoid()
                                                                        .thenRun(() ->
                                                                                sendPotpissersPluginMessage(PLUGIN_PARTY_DISBANDER, new JSONObject(Map.of("partyUUIDString", abstractPartyData.uuid.toString())).toJSONString().getBytes(StandardCharsets.UTF_8)));
                                                            });
                                            });
                                }
                            }
                            case "invite", "inv" -> {
                                if (args.length < 2) // TODO add multiple invites at once ability + GUI
                                    p.sendMessage("PARTY_USAGE");
                                else {
                                    OfflinePlayer oPArg = Bukkit.getOfflinePlayer(args[1]);
                                    fetchCreatableParty(mutableAbstractPartyData, data, p)
                                            .thenAccept(party ->
                                                    executeForeignOfflinePlayerCommand(oPArg, party, party.uuid, p, () ->
                                                            party.fetchOptionalInviteRankName(oPArg.getUniqueId())
                                                                    .thenAccept(optionalInviteRankName -> {
                                                                        if (optionalInviteRankName.isPresent())
                                                                            p.sendMessage(getDangerComponent("invalid (already invited)"));
                                                                        else {
                                                                            CompletableFuture<String> futurePartyName = fetchNonnullPartyName(party.uuid);
                                                                            party.fetchIsPlayerOfficer(data.uuid)
                                                                                    .thenAccept(isOfficer -> {
                                                                                        if (!isOfficer)
                                                                                            executeNetworkPartyMessage(party.uuid, futurePartyName, getNormalComponent(p.getName() + " has tried to invite " + oPArg.getName()));
                                                                                        else {
                                                                                            CompletableFuture<Void> futureInviteVoid = fetchQueryVoid(POSTGRES_POOL, "call upsert_party_invite(?, ?, ?, ?)", new Object[]{oPArg.getUniqueId(), party.uuid, data.uuid, PARTY_RANK_MEMBER});
                                                                                            fetchNonnullPartyName(party.uuid)
                                                                                                    .thenAccept(partyName -> futureInviteVoid
                                                                                                            .thenRun(() -> {
                                                                                                                executeNetworkPlayerMessage(oPArg.getUniqueId(), getFocusComponent(partyName + " has invited you"));
                                                                                                                executeNetworkPartyMessage(party.uuid, futurePartyName, getNormalComponent(p.getName() + " has invited " + oPArg.getName()));
                                                                                                            }));
                                                                                        }
                                                                                    });
                                                                        }
                                                                    })));
                                }
                            }
                            case "uninvite", "revoke", "disinvite", "deinvite" -> {
                                if (args.length < 2) // TODO add multiple unInvite functionality
                                    p.sendMessage("PARTY_USAGE");
                                if (!(mutableAbstractPartyData[0] instanceof AbstractPartyData abstractPartyData))
                                    p.sendMessage(getNormalComponent("invalid (party)"));
                                else {
                                    OfflinePlayer offlinePlayerArg = Bukkit.getOfflinePlayer(args[1]);
                                    executeForeignOfflinePlayerCommand(offlinePlayerArg, abstractPartyData, abstractPartyData.uuid, p, () ->
                                            abstractPartyData.fetchOptionalInviteRankName(offlinePlayerArg.getUniqueId())
                                                    .thenAccept(optional -> {
                                                        if (optional.isEmpty())
                                                            p.sendMessage(getNormalComponent("invalid (no invite)"));
                                                        else {
                                                            CompletableFuture<String> futurePartyName = fetchNonnullPartyName(abstractPartyData.uuid);
                                                            abstractPartyData.fetchIsPlayerOfficer(data.uuid)
                                                                    .thenAccept(isOfficer -> {
                                                                        if (!isOfficer)
                                                                            executeNetworkPartyMessage(abstractPartyData.uuid, futurePartyName, getNormalComponent(p.getName() + " has tried to un-invite " + offlinePlayerArg.getName()));
                                                                        else {
                                                                            UUID oPArgUUID = offlinePlayerArg.getUniqueId();
                                                                            fetchQueryVoid(POSTGRES_POOL, "call delete_party_invite(?, ?)", new Object[]{oPArgUUID, abstractPartyData.uuid})
                                                                                    .thenRun(() -> {
                                                                                        executeNetworkPartyMessage(abstractPartyData.uuid, futurePartyName, getFocusComponent(p.getName() + " has un-invited " + offlinePlayerArg.getName()));
                                                                                        futurePartyName
                                                                                                .thenAccept(partyName -> executeNetworkPlayerMessage(offlinePlayerArg.getUniqueId(), getFocusComponent(partyName + " has un-invited you")));
                                                                                    });
                                                                        }
                                                                    });
                                                        }
                                            }));
                                }
                            }
                            case "kick" -> {
                                if (args.length == 1)
                                    p.sendMessage("PARTY_USAGE"); // TODO GUI
                                else if (!(mutableAbstractPartyData[0] instanceof AbstractPartyData abstractPartyData))
                                    p.sendMessage(getNormalComponent("invalid (party)"));
                                else {
                                    OfflinePlayer oPArg = Bukkit.getOfflinePlayer(args[1]);
                                    executePartyOfflinePlayerCommand(oPArg, abstractPartyData, abstractPartyData.uuid, p, () ->
                                            abstractPartyData.fetchNonnullPlayerRankName(data.uuid)
                                                    .thenAccept(rankName -> {
                                                        CompletableFuture<String> futurePartyName = fetchNonnullPartyName(abstractPartyData.uuid);
                                                        fetchIsPlayerHigherPerms(abstractPartyData, rankName, oPArg.getUniqueId())
                                                                .thenAccept(isHigherPerms -> {
                                                                    if (!isHigherPerms)
                                                                        executeNetworkPartyMessage(abstractPartyData.uuid, futurePartyName, getNormalComponent(p.getName() + " has tried to kick " + oPArg.getName()));
                                                                    else if (data.uuid.equals(oPArg.getUniqueId()) && rankName.equals(PARTY_RANK_LEADER))
                                                                        p.sendMessage(getDangerComponent("invalid (leader), transfer leadership or use disband"));
                                                                    else
                                                                        abstractPartyData.fetchKickVoid(oPArg.getUniqueId())
                                                                                .thenRun(() -> {
                                                                                    executeNetworkPartyMessage(abstractPartyData.uuid, futurePartyName, getFocusComponent(oPArg.getName() + " has been kicked by " + p.getName()));
                                                                                    fetchNonnullPartyName(abstractPartyData.uuid)
                                                                                            .thenAccept(partyName -> sendPotpissersPluginMessage(PLUGIN_PARTY_KICKER, new JSONObject(Map.of("playerUUIDString", oPArg.getUniqueId().toString(), "partyUUIDString", abstractPartyData.uuid.toString(), "partyName", partyName)).toJSONString().getBytes(StandardCharsets.UTF_8)));
                                                                                });
                                                                });
                                                    }));
                                }
                            }
                            case "accept", "join" -> {
                                if (args.length == 1) {
                                    p.sendMessage("PARTY_USAGE"); // TODO -> gui/etc
                                    return true;
                                }
                                if (args.length > 2)
                                    p.sendMessage("PARTY_USAGE");
                                ((mutableAbstractPartyData[0] instanceof AbstractPartyData abstractPartyData)
                                        ? abstractPartyData.fetchNonnullPlayerRankName(data.uuid).thenCompose(rankName -> {
                                    if (rankName.equals(PARTY_RANK_LEADER)) {
                                        p.sendMessage("invalid (leader), use /f disband or transfer leadership"); // TODO -> use disband code
                                        return CompletableFuture.completedFuture(false);
                                    } else
                                        return abstractPartyData.executeLeaveReturnVoid(rankName, p, data.uuid, data)
                                                .thenCompose(v -> CompletableFuture.completedFuture(true));
                                })
                                        : CompletableFuture.completedFuture(true))
                                        .thenAccept(isValid -> {
                                            OfflinePlayer offlinePlayerArg = Bukkit.getOfflinePlayer(args[1]);
                                            executeOfflinePlayerCommand(offlinePlayerArg, p, () ->
                                                    (mutableAbstractPartyData instanceof NetworkPartyData[]
                                                            ? fetchExistingNetworkParty(offlinePlayerArg.getUniqueId())
                                                            : null) // TODO -> handle faction
                                                            .thenAccept(optionalParty -> {
                                                                if (optionalParty.isEmpty())
                                                                    p.sendMessage("invalid (party). usage: /f join (member name)"); // TODO -> this bad
                                                                else {
                                                                    AbstractPartyData abstractPartyData = optionalParty.get();
                                                                    abstractPartyData.fetchOptionalInviteRankName(data.uuid)
                                                                            .thenAccept(optionalInviteRankName -> {
                                                                                if (optionalInviteRankName.isEmpty())
                                                                                    p.sendMessage("invalid (need invite)"); // TODO -> doRosterMsg
                                                                                else {
                                                                                    CompletableFuture<String> futurePartyName = fetchNonnullPartyName(abstractPartyData.uuid);
                                                                                    abstractPartyData.fetchJoinVoid(data.uuid, optionalInviteRankName.get())
                                                                                            .thenRun(() -> {
                                                                                                executeNetworkPartyMessage(abstractPartyData.uuid, futurePartyName, getNormalComponent(p.getName() + " has joined the party"));
                                                                                                mutableAbstractPartyData[0] = abstractPartyData;
                                                                                            });
                                                                                }
                                                                            });
                                                                }
                                                            }));
                                        });
                            }
                            case "leave", "quit" -> {
                                if (args.length > 1)
                                    p.sendMessage("PARTY_USAGE");

                                if (!(mutableAbstractPartyData[0] instanceof AbstractPartyData abstractPartyData))
                                    p.sendMessage("invalid (party)");
                                else
                                    abstractPartyData.fetchNonnullPlayerRankName(data.uuid)
                                            .thenAccept(rankName -> {
                                                if (rankName.equals(PARTY_RANK_LEADER))
                                                    p.sendMessage(getFocusComponent("invalid (leader), use disband. or switch leader first"));
                                                else
                                                    abstractPartyData.executeLeaveReturnVoid(rankName, p, data.uuid, data);
                                            });
                            }
                            case "officer" -> {
                                if (args.length == 1)
                                    p.sendMessage("PARTY_USAGE");
                                else if (!(mutableAbstractPartyData[0] instanceof AbstractPartyData abstractPartyData))
                                    p.sendMessage("invalid (party)");
                                else {
                                    OfflinePlayer oPArg = Bukkit.getOfflinePlayer(args[1]);
                                    CompletableFuture<Boolean> futureIsOfficer = abstractPartyData.fetchIsPlayerOfficer(data.uuid);

                                    executePartyOfflinePlayerCommand(oPArg, abstractPartyData, abstractPartyData.uuid, p, () ->
                                            abstractPartyData.fetchNonnullPlayerRankName(oPArg.getUniqueId())
                                                    .thenAccept(rankName -> {
                                                        if (PARTY_RANK_LEVELS.get(rankName) >= PARTY_RANK_LEVELS.get(PARTY_RANK_OFFICER))
                                                            p.sendMessage("invalid (officer+)");
                                                        else {
                                                            CompletableFuture<String> futurePartyName = fetchNonnullPartyName(abstractPartyData.uuid);
                                                            futureIsOfficer
                                                                    .thenAccept(isOfficer -> {
                                                                        if (!isOfficer)
                                                                            executeNetworkPartyMessage(abstractPartyData.uuid, futurePartyName, getNormalComponent(p.getName() + " has tried to promote " + oPArg.getName() + " to " + PARTY_RANK_OFFICER));
                                                                        else
                                                                            abstractPartyData.executeNetworkPromotion(oPArg.getUniqueId(), PARTY_RANK_OFFICER, futurePartyName, p.getName(), oPArg.getName());
                                                                    });
                                                        }
                                                    }));
                                }
                            }
                            case "demote" -> {
                                if (args.length == 1)
                                    p.sendMessage("PARTY_USAGE");
                                else if (!(mutableAbstractPartyData[0] instanceof AbstractPartyData abstractPartyData))
                                    p.sendMessage("invalid (party)");
                                else {
                                    OfflinePlayer opArg = Bukkit.getOfflinePlayer(args[1]);
                                    UUID uuidArg = opArg.getUniqueId();

                                    executePartyOfflinePlayerCommand(opArg, abstractPartyData, abstractPartyData.uuid, p, () ->
                                            abstractPartyData.fetchNonnullPlayerRankName(uuidArg)
                                                    .thenAccept(rankArg -> {
                                                        switch (rankArg) {
                                                            case PARTY_RANK_MEMBER ->
                                                                    p.sendMessage("invalid (officer)");
                                                            case PARTY_RANK_LEADER -> p.sendMessage("invalid (leader)");
                                                            default -> {
                                                                CompletableFuture<String> futurePartyName = fetchNonnullPartyName(abstractPartyData.uuid);
                                                                fetchIsPlayerHigherPerms(abstractPartyData, rankArg, data.uuid)
                                                                        .thenAccept(isArgHigherPerms -> {
                                                                            String newPermsName = PARTY_RANK_LEVELS.entrySet().stream().filter(entry -> entry.getValue().equals(PARTY_RANK_LEVELS.get(rankArg) - 1)).findAny().orElseThrow().getKey();
                                                                            if (isArgHigherPerms)
                                                                                executeNetworkPartyMessage(abstractPartyData.uuid, futurePartyName, getNormalComponent(p.getName() + " has tried to demote " + opArg.getName() + " from " + rankArg + " to " + newPermsName));
                                                                            else {
                                                                                abstractPartyData.fetchJoinVoid(uuidArg, newPermsName);
                                                                                executeNetworkPartyMessage(abstractPartyData.uuid, futurePartyName, getNormalComponent(p.getName() + " has demoted " + opArg.getName() + " from " + rankArg + " to " + newPermsName));
                                                                            }
                                                                        });
                                                            }
                                                        }
                                                    }));
                                }
                            }
                            case "ally", "truce", "friend" -> { // TODO add multi ally etc
                                if (args.length == 1) // TODO -> both player-found party and faction-name-found party should resolve
                                    p.sendMessage("PARTY_USAGE");
                                else
                                    fetchCreatableParty(mutableAbstractPartyData, data, p)
                                            .thenAccept(party -> {
                                                OfflinePlayer offlinePlayerArg = Bukkit.getOfflinePlayer(args[1]);
                                                executeForeignOfflinePlayerCommand(offlinePlayerArg, party, party.uuid, p, () ->
                                                        fetchOptionalMainPartyUUID(offlinePlayerArg.getUniqueId())
                                                                .thenAccept(optionalPartyUuid -> {
                                                                    if (party.allyCache.contains(optionalPartyUuid.orElse(null)))
                                                                        p.sendMessage("invalid (allied)");
                                                                    else
                                                                        party.fetchIsPlayerOfficer(data.uuid)
                                                                                .thenAccept(isOfficer -> {
                                                                                    CompletableFuture<String> futurePartyName = fetchNonnullPartyName(party.uuid);
                                                                                    CompletableFuture<String> futurePartyArgName = optionalPartyUuid.isPresent()
                                                                                            ? fetchNonnullPartyName(optionalPartyUuid.get())
                                                                                            : CompletableFuture.completedFuture(null);
                                                                                    if (!isOfficer)
                                                                                        futurePartyArgName
                                                                                                .thenAccept(partyArgName ->
                                                                                                        executeNetworkPartyMessage(party.uuid, futurePartyName, getNormalComponent(p.getName() + " has failed to request an alliance with " + partyArgName + " (" + offlinePlayerArg.getName() + ") (not officer)")));
                                                                                    else {
                                                                                        if (optionalPartyUuid.isEmpty()) {
                                                                                            futurePartyName.thenAccept(partyName -> {
                                                                                                boolean flag = false;
                                                                                                if (offlinePlayerArg.getPlayer() instanceof Player player) {
                                                                                                    player.sendMessage(getFocusComponent(partyName + " has failed to request an alliance with you (party)"));
                                                                                                    flag = true;
                                                                                                }
                                                                                                p.sendMessage(getNormalComponent("invalid (party)" + (flag ? ", they've been notified" : "")));
                                                                                            });
                                                                                        } else {
                                                                                            UUID partyArgUUID = optionalPartyUuid.get();
                                                                                            futurePartyName
                                                                                                    .thenAccept(partyName ->
                                                                                                            futurePartyArgName.thenAccept(partyArgName ->
                                                                                                                    fetchPgCallNonnullT("{? = call get_is_ally_invited(?, ?)}", Types.BOOLEAN, new Object[]{partyArgUUID, party.uuid}, Boolean.class)
                                                                                                                            .thenAccept(isRequested -> {
                                                                                                                                if (!isRequested) {
                                                                                                                                    fetchPgCallNonnullT("{? = call upsert_ally_invite_return_existed(?, ?, ?)}", Types.BOOLEAN, new Object[]{party.uuid, partyArgUUID, data.uuid}, Boolean.class)
                                                                                                                                            .thenAccept(exists -> {
                                                                                                                                                if (exists)
                                                                                                                                                    p.sendMessage(getNormalComponent("invalid (request pending)"));
                                                                                                                                                else {
                                                                                                                                                    executeNetworkPartyMessage(party.uuid, futurePartyName, getNormalComponent(p.getName() + " has requested an alliance with " + partyArgName + "(" + offlinePlayerArg.getName() + ")"));
                                                                                                                                                    executeNetworkPartyMessage(partyArgUUID, futurePartyArgName, getFocusComponent(partyName + " has requested an alliance"));
                                                                                                                                                }
                                                                                                                                            });
                                                                                                                                } else {
                                                                                                                                    String pName = p.getName();
                                                                                                                                    String pArgName = offlinePlayerArg.getName();

                                                                                                                                    CompletableFuture<Void> futureIsPartyEnemied = fetchOptionalPartyRelation(partyArgUUID, party.uuid)
                                                                                                                                            .thenCompose(optional -> {
                                                                                                                                                if (optional.isPresent())
                                                                                                                                                    return fetchNetworkUnEnemyVoid(partyArgUUID, party.uuid, pArgName, pName);
                                                                                                                                                else
                                                                                                                                                    return CompletableFuture.completedFuture(null);
                                                                                                                                            });
                                                                                                                                    CompletableFuture.allOf(new CompletableFuture[]
                                                                                                                                                    {
                                                                                                                                                            (party.enemyCache.contains(partyArgUUID)
                                                                                                                                                                    ? fetchNetworkUnEnemyVoid(party.uuid, partyArgUUID, pName, pArgName)
                                                                                                                                                                    : CompletableFuture.completedFuture(null)),
                                                                                                                                                            futureIsPartyEnemied, // todo -> I have no clue why this piece of fucking shit language needs me to make this variable for this to work
                                                                                                                                                            fetchQueryVoid(POSTGRES_POOL, "CALL delete_ally_invite(?, ?)", new Object[]{partyArgUUID, party.uuid})
                                                                                                                                                    })
                                                                                                                                            .thenRun(() -> {
                                                                                                                                                UUID partyUUID = party.uuid;
                                                                                                                                                CompletableFuture.allOf(new CompletableFuture[]{
                                                                                                                                                        fetchCreateRelationVoid(partyUUID, partyArgUUID, Boolean.TRUE),
                                                                                                                                                                fetchCreateRelationVoid(partyArgUUID, partyUUID, Boolean.TRUE)
                                                                                                                                                })
                                                                                                                                                        .thenRun(() -> {
                                                                                                                                                            sendPotpissersPluginMessage(PLUGIN_PARTY_ALLIER, new JSONObject(Map.of("partyUUIDString", partyUUID.toString(), "partyArgUUIDString", partyArgUUID.toString())).toJSONString().getBytes(StandardCharsets.UTF_8));
                                                                                                                                                            executeNetworkPartyMessage(partyUUID, futurePartyName, getNormalComponent(pName + " has allied " + partyArgName + " (" + pArgName + ")"));
                                                                                                                                                            executeNetworkPartyMessage(partyArgUUID, futurePartyArgName, getFocusComponent(partyName + " has allied your party")); // TODO -> get the teammate who request it
                                                                                                                                                        });
                                                                                                                                            });
                                                                                                                                }
                                                                                                                            })));
                                                                                        }
                                                                                    }
                                                                                });
                                                                }));

                                            });
                            } // TODO both player names and party name
                            case "enemy" -> {
                                if (args.length == 1)
                                    p.sendMessage("PARTY_USAGE");
                                else {
                                    OfflinePlayer offlinePlayerArg = Bukkit.getOfflinePlayer(args[1]);
                                    fetchOptionalMainPartyUUID(offlinePlayerArg.getUniqueId())
                                            .thenAccept(optionalPartyArgUUID -> {
                                                if (optionalPartyArgUUID.isEmpty())
                                                    p.sendMessage(getNormalComponent("invalid (party)"));
                                                else {
                                                    UUID partyArgUUID = optionalPartyArgUUID.get();
                                                    fetchCreatableParty(mutableAbstractPartyData, data, p)
                                                            .thenAccept(party -> executeForeignOfflinePlayerCommand(offlinePlayerArg, party, party.uuid, p, () -> {
                                                                if (party.enemyCache.contains(partyArgUUID))
                                                                    p.sendMessage(getNormalComponent("invalid (enemied)"));
                                                                else {
                                                                    CompletableFuture<String> futurePartyName = fetchNonnullPartyName(party.uuid);
                                                                    party.fetchIsPlayerOfficer(data.uuid)
                                                                            .thenAccept(isOfficer -> {
                                                                                CompletableFuture<String> futurePartyArgName = fetchNonnullPartyName(partyArgUUID);
                                                                                if (!isOfficer)
                                                                                    futurePartyArgName
                                                                                            .thenAccept(partyArgName ->
                                                                                                    executeNetworkPartyMessage(party.uuid, futurePartyName, getNormalComponent(p.getName() + " has failed to mark " + partyArgName + " as an enemy (not officer)")));
                                                                                else {
                                                                                    String pName = p.getName();
                                                                                    String pArgName = offlinePlayerArg.getName();
                                                                                    (party.allyCache.contains(partyArgUUID)
                                                                                            ? party.fetchNetworkUnAllyVoid(partyArgUUID, futurePartyName, futurePartyArgName, pName, pArgName)
                                                                                            : CompletableFuture.completedFuture(null))
                                                                                            .thenRun(() -> {
                                                                                                CompletableFuture<Void> futureEnemyVoid = fetchCreateRelationVoid(party.uuid, partyArgUUID, Boolean.FALSE);

                                                                                                fetchNonnullPartyName(party.uuid)
                                                                                                        .thenAccept(partyName -> futurePartyArgName
                                                                                                                .thenAccept(partyArgName -> futureEnemyVoid
                                                                                                                        .thenRun(() -> {
                                                                                                                            sendPotpissersPluginMessage(PLUGIN_PARTY_ENEMIER, new JSONObject(Map.of("partyUUIDString", party.uuid.toString(), "partyArgUUIDString", partyArgUUID.toString())).toJSONString().getBytes(StandardCharsets.UTF_8));
                                                                                                                            executeNetworkPartyMessage(party.uuid, futurePartyName, getNormalComponent(pName + " has enemied " + partyArgName + " (" + pArgName + ")"));
                                                                                                                            executeNetworkPartyMessage(partyArgUUID, futurePartyArgName, getFocusComponent(partyName + " has enemied your party"));
                                                                                                                        })));
                                                                                            });
                                                                                }
                                                                            });
                                                                }
                                                            }));
                                                }
                                            });
                                }
                            }
                            case "neutral" -> {
                                if (args.length == 1)
                                    p.sendMessage("PARTY_USAGE");
                                else if (!(mutableAbstractPartyData[0] instanceof AbstractPartyData abstractPartyData))
                                    p.sendMessage("invalid (party)");
                                else {
                                    OfflinePlayer offlinePlayerArg = Bukkit.getOfflinePlayer(args[1]);
                                    executeForeignOfflinePlayerCommand(offlinePlayerArg, abstractPartyData, abstractPartyData.uuid, p, () ->
                                            fetchOptionalMainPartyUUID(offlinePlayerArg.getUniqueId())
                                                    .thenAccept(optionalPartyArgUUID -> {
                                                        if (optionalPartyArgUUID.isEmpty())
                                                            p.sendMessage("invalid (party)");
                                                        else {
                                                            UUID partyArgUUID = optionalPartyArgUUID.get();
                                                            CompletableFuture<String> futurePartyName = fetchNonnullPartyName(abstractPartyData.uuid);
                                                            CompletableFuture<String> futurePartyArgName = fetchNonnullPartyName(partyArgUUID);
                                                            abstractPartyData.fetchIsPlayerOfficer(data.uuid)
                                                                    .thenAccept(isOfficer -> {
                                                                        if (!isOfficer)
                                                                            futurePartyArgName
                                                                                    .thenAccept(partyArgName ->
                                                                                            executeNetworkPartyMessage(abstractPartyData.uuid, futurePartyName, getNormalComponent(p.getName() + " has failed to mark " + partyArgName + " as neutral (not officer)")));
                                                                        else if (abstractPartyData.allyCache.contains(partyArgUUID))
                                                                            abstractPartyData.fetchNetworkUnAllyVoid(partyArgUUID, futurePartyName, futurePartyArgName, p.getName(), offlinePlayerArg.getName());
                                                                        else if (abstractPartyData.enemyCache.contains(partyArgUUID))
                                                                            fetchNetworkUnEnemyVoid(abstractPartyData.uuid, partyArgUUID, p.getName(), offlinePlayerArg.getName());
                                                                        else
                                                                            p.sendMessage(getNormalComponent("invalid (neutral)"));
                                                                        // TODO -> make this reset focuses to their correct color in cubeCore
                                                                    });
                                                        }
                                                    }));
                                }
                            }
                            case "focus", "target" -> // TODO -> run for each arg // TODO -> faction name matching
                                    handleFocusCommand(args.length > 1 ? args[1] : null, mutableAbstractPartyData[0], p);
                            case "unfocus", "untarget", "removefocus" ->
                                    handleUnfocusCommand(args.length >= 2 ? args[1] : null, mutableAbstractPartyData[0], p);
                            case "chat", "c", "ch" ->
                                    handleChatCommand(args.length > 1 ? args[1] : null, p, data);
                            case "rally", "r" ->
                                    handleRallyCommand(args.length > 1 ? args[1] : null, p, mutableAbstractPartyData[0]);
                            case "caution" ->
                                    handleCautionCommand(p, mutableAbstractPartyData[0]);
                            case "decline", "deny" -> {}
                            case "invites" -> {}
                            case "promote" -> {}
                            case "leader", "transfer" -> {
                                if (args.length == 1)
                                    p.sendMessage(getNormalComponent("usage: /f leader (target)"));
                                else if (!(mutableAbstractPartyData[0] instanceof AbstractPartyData abstractPartyData))
                                    p.sendMessage("invalid (party)");
                                else {
                                    if (args.length > 2)
                                        p.sendMessage("?");
                                    OfflinePlayer offlinePlayerArg = Bukkit.getOfflinePlayer(args[1]);
                                    CompletableFuture<String> futurePartyName = fetchNonnullPartyName(abstractPartyData.uuid);
                                    executePartyOfflinePlayerCommand(offlinePlayerArg, abstractPartyData, abstractPartyData.uuid, p, () ->
                                            abstractPartyData.fetchNonnullPlayerRankName(data.uuid)
                                                    .thenAccept(rankName -> {
                                                        if (!rankName.equals(PARTY_RANK_LEADER))
                                                            executeNetworkPartyMessage(abstractPartyData.uuid, futurePartyName, getNormalComponent(p.getName() + " has tried to transfer leadership to " + offlinePlayerArg.getName())); // TODO -> partyName ?
                                                        else {
                                                            abstractPartyData.executeLeaderChange(offlinePlayerArg.getUniqueId());
                                                            executeNetworkPartyMessage(abstractPartyData.uuid, futurePartyName, getNormalComponent(p.getName() + " has transferred their leadership to " + offlinePlayerArg.getName()));
                                                        }
                                                    }));
                                }
                            }
                            case "warp", "tp", "teleport", "rallywarp", "rw" -> {}
                            case "warpset", "setwarp", "settp", "tpset", "teleportset", "setteleport" ->
                            {
                                if (args.length > 1)
                                    p.sendMessage("?");
                                if (!(mutableAbstractPartyData[0] instanceof AbstractPartyData abstractPartyData))
                                    p.sendMessage("invalid (party)");
                                else {
                                    executeNetworkPartyMessage(abstractPartyData.uuid, fetchNonnullPartyName(abstractPartyData.uuid), getFocusComponent(p.getName() + " has used /setwarp"));
                                    abstractPartyData.getOnlineRosterPlayerStream()
                                            .forEach(playerIteration ->
                                                    data.handleRequestTpaHere(p, playerIteration, playerDataCache.get(playerIteration)));
                                }
                            }
                            case "duel" -> {
                                if (!IS_KIT_SERVER)
                                    p.sendMessage("invalid (disabled)");
                                else
                                    handleDuelCommand(Arrays.copyOfRange(args, 1, args.length), p, mutableAbstractPartyData[0] instanceof AbstractPartyData abstractPartyData ? abstractPartyData : data);
                            }
                            case "anon" -> {
                                if (!IS_KIT_SERVER)
                                    p.sendMessage("invalid (disabled)");
                                else if (!(mutableAbstractPartyData[0] instanceof AbstractPartyData abstractPartyData))
                                    p.sendMessage("invalid (party)");
                                else if (!abstractPartyData.isInSpawn())
                                    p.sendMessage("invalid (spawn)");
                                else if (!(getNullableDuelOptionsOfArgs(args) instanceof DuelOptions duelOptions))
                                    p.sendMessage("invalid (args)");
                                else {
                                    for (Map.Entry<AbstractPartyData, DuelOptions> entry : partyDuelQueue.sequencedEntrySet())
                                        if (duelOptions.getNullableFinalDuelOptions(entry.getValue()) instanceof DuelOptions finalDuelOptions) {
                                            abstractPartyData.executeHandleDuel(entry.getKey(), finalDuelOptions);
                                            return true;
                                        }
                                    // else
                                    partyDuelQueue.putLast(abstractPartyData, duelOptions);
                                    Bukkit.broadcast(getNormalComponent("?'s party (" + abstractPartyData.getOnlineRosterPlayerStream().count() + ") has used /p anon to enter team-fight queue with kit: " + duelOptions.kitName() + ", attack speed: " + duelOptions.attackSpeedName() + ", arena: " + duelOptions.arenaName()));
                                }
                            }
                            case "dequeue" -> {
                                if (args.length > 1)
                                    p.sendMessage("?");
                                if (!(mutableAbstractPartyData[0] instanceof AbstractPartyData abstractPartyData))
                                    p.sendMessage("invalid (party)");
                                else
                                    abstractPartyData.handleQueueCancel();
                            }
                            case "who", "info" -> {
                                if (mutableAbstractPartyData instanceof NetworkPartyData[]) {
                                    if (args.length > 1)
                                        p.sendMessage("?");
                                    if (!(mutableAbstractPartyData[0] instanceof NetworkPartyData networkPartyData))
                                        p.sendMessage("invalid (party)");
                                    else {
                                        UUID partyUUID = networkPartyData.uuid;
                                        CompletableFuture<ArrayList<HashMap<String, Object>>> futureRelations = fetchDictList(POSTGRES_POOL, "SELECT * FROM get_party_relations(?)", new Object[]{partyUUID});
                                        CompletableFuture<ArrayList<HashMap<String, Object>>> futureMembers = fetchDictList(POSTGRES_POOL, "SELECT * FROM get_party_members(?)", new Object[]{partyUUID});

                                        fetchNonnullPartyName(partyUUID)
                                                .thenAccept(name ->
                                                        handleAbstractPartyWhoMessageSuffix(futureRelations, futureMembers, p, getFWhoConsolePlainComponent("- party: ").append(getFWhoGreenPlainComponent(name)), partyUUID));
                                    }
                                }
                            }
                            default -> {
                                p.sendMessage(getNormalComponent("invalid (args)"));
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    void handleDuelCommand(String[] args, Player p, AbstractData dueler) {
        if (args.length > 3)
            p.sendMessage(getConsoleComponent("?"));
        if (args.length == 0) { // TODO doQueue or GUI
            boolean flag = false;
            StringBuilder stringBuilder = new StringBuilder().append("requests: ");
            for (PlayerData data : playerDataCache.values())
                if (data.outgoingDuelRequestsContains(dueler)) {
                    stringBuilder
                            .append(Bukkit.getOfflinePlayer(data.uuid).getName())
                            .append(" ");
                    flag = true;
                }
            for (AbstractPartyData party : getOnlinePartyStream().toList())
                if (party.outgoingDuelRequestsContains(dueler) && party.getOnlineRosterPlayerDataStream().findFirst().orElse(null) instanceof PlayerData data) {
                    stringBuilder
                            .append(Bukkit.getOfflinePlayer(data.uuid).getName())
                            .append("'s party")
                            .append("(")
                            .append(party.getOnlineRosterPlayerStream().count())
                            .append(") ");
                    flag = true;
                }
            if (!flag)
                p.sendMessage(getNormalComponent(String.valueOf(stringBuilder.append(" none"))));
            else
                p.sendMessage(getNormalComponent(String.valueOf(stringBuilder)));
        }
        else if (!dueler.isInSpawn())
            p.sendMessage("invalid (spawn)");
        else if (!(Bukkit.getPlayer(args[0]) instanceof Player pArg))
            p.sendMessage("invalid (player)"); // TODO add support for teamName
        else if (!(getNullableDuelOptionsOfArgs(Arrays.copyOfRange(args, 1, args.length)) instanceof DuelOptions duelOptions))
            p.sendMessage("invalid (args)");
        else
            dueler.executeDuelRequest(playerDataCache.get(pArg), p, duelOptions, pArg);
    }

    void handleFocusCommand(String arg, AbstractPartyData nullableAbstractPartyData, Player p) {
        handleAbstractFocusCommand(arg, nullableAbstractPartyData, p, "focus");
    }
    void handleUnfocusCommand(String arg, AbstractPartyData nullableAbstractPartyData, Player p) {
        handleAbstractFocusCommand(arg, nullableAbstractPartyData, p, "unfocus");
    }
    void handleAbstractFocusCommand(String arg, AbstractPartyData nullableAbstractPartyData, Player p, String commandName) {
        if (arg == null)
            p.sendMessage(getNormalComponent("usage: /p focus (target)"));
        else {
            OfflinePlayer offlinePlayerArg = Bukkit.getOfflinePlayer(arg);
            if (!(nullableAbstractPartyData instanceof AbstractPartyData abstractPartyData)) {
                p.sendMessage(getFocusComponent(p.getName() + " has /" + commandName + "'d " + offlinePlayerArg.getName()));
            }
            else {
                executeNetworkPartyMessage(abstractPartyData.uuid, fetchNonnullPartyName(abstractPartyData.uuid), getFocusComponent(p.getName() + " has /" + commandName + "'d " + offlinePlayerArg.getName()));
            }
        }
    }

    void handleRallyCommand(String arg, Player p, AbstractPartyData nullableAbstractPartyData) {
        Player rally = arg != null
                ? Bukkit.getPlayer(arg) instanceof Player player && playerDataCache.get(player).getCurrentParties().anyMatch(party -> party.equals(nullableAbstractPartyData))
                ? player
                : p
                : p;
        Component rallyMsg = getFocusComponent(p.getName() + " has used /rally on " + rally.getName() + ": " + getLocationString(rally.getLocation()));
        Firework firework = (Firework) rally.getWorld().spawnEntity(rally.getLocation(), EntityType.FIREWORK_ROCKET);
        firework.setVisibleByDefault(false);
        if (!(nullableAbstractPartyData instanceof AbstractPartyData abstractPartyData)) {
            p.sendMessage(rallyMsg);
            p.showEntity(plugin, firework);
            // TODO -> sendBlockChanges
        }
        else {
            executeNetworkPartyMessage(abstractPartyData.uuid, fetchNonnullPartyName(abstractPartyData.uuid), rallyMsg);
            abstractPartyData.getOnlineRosterPlayerStream()
                    .forEach(player -> player.showEntity(plugin, firework));
//            if (mutableParty instanceof NetworkParty[])
//                playerDataCache.values().stream().map(data -> data.mutableNetworkParty[0]); // TODO -> sendBlockChanges
        }
    }
    void handleCautionCommand(Player p, AbstractPartyData nullableAbstractPartyData) {
        Block caution = p.getTargetBlockExact(64);
        Component rallyMsg = getFocusComponent(p.getName() + " has used /caution on " + getLocationString(caution.getLocation()));
        if (!(nullableAbstractPartyData instanceof AbstractPartyData abstractPartyData)) {
            p.sendMessage(rallyMsg);
            // TODO -> sendBlockChanges
        }
        else {
            executeNetworkPartyMessage(abstractPartyData.uuid, fetchNonnullPartyName(abstractPartyData.uuid), rallyMsg);
//            if (mutableParty instanceof NetworkParty[])
//                playerDataCache.values().stream().map(data -> data.mutableNetworkParty[0]); // TODO -> sendBlockChanges
        }
    }

    void handleChatCommand(String arg, Player p, PlayerData data) {
        CompletableFuture<String> futureChatType = new CompletableFuture<>();
        if (arg != null)
            switch (arg) {
                case "all", "server", "p", "!", "public" ->
                        futureChatType.complete(CHAT_SERVER);
                case "faction", "@", "f" -> // TODO -> f c f toggles between party and faction ?
                        futureChatType.complete(CHAT_FACTION);
                case "ally", "a", "#", "alliance" ->
                        futureChatType.complete(CHAT_ALLY);
                case "local", "l", "$" ->
                        futureChatType.complete(CHAT_LOCAL);
                case "party", "%" ->
                        futureChatType.complete(CHAT_PARTY);
                default ->
                        p.sendMessage(getNormalComponent("?"));
            }
        if (!futureChatType.isDone())
            fetchPgCallNonnullT("{? = call get_user_chat_type(?)}", Types.VARCHAR, new Object[]{data.uuid}, String.class)
                    .thenAccept(chatTypeName -> {
                        switch (chatTypeName) {
                            case CHAT_SERVER -> {
                                if (true)
                                    futureChatType.complete(CHAT_ALLY);
                                else
                                    futureChatType.complete(CHAT_FACTION);
                            } // TODO -> only cycle to chats that make sense
                            case CHAT_FACTION -> futureChatType.complete(CHAT_ALLY);
                            case CHAT_ALLY -> futureChatType.complete(CHAT_LOCAL);
                            case CHAT_LOCAL -> futureChatType.complete(CHAT_PARTY);
                            case CHAT_PARTY -> futureChatType.complete(CHAT_SERVER);
                        }
                    });
        futureChatType
                .thenAccept(chatType ->
                        fetchQueryVoid(POSTGRES_POOL, "call update_user_chat_type(?, ?)", new Object[]{chatType, data.uuid})
                                .thenRun(() ->
                                        p.sendMessage(getNormalComponent("chat type changed to: " + chatType))));
    }
    CompletableFuture<Boolean> fetchIsPlayerHigherPerms(AbstractPartyData abstractPartyData, String playerRankName, UUID uuidArg) {
        CompletableFuture<Boolean> futureBoolean = new CompletableFuture<>();

        abstractPartyData.fetchNonnullPlayerRankName(uuidArg)
                .thenAccept(pArgRankName ->
                        futureBoolean.complete(PARTY_RANK_LEVELS.get(playerRankName) > PARTY_RANK_LEVELS.get(pArgRankName) || pArgRankName.equals(PARTY_RANK_OFFICER) || pArgRankName.equals(PARTY_RANK_MEMBER)));
        return futureBoolean;
    }
    void executeOfflinePlayerCommand(OfflinePlayer offlinePlayer, Player p, Runnable runnable) {
        if (offlinePlayer.getName() == null)
            p.sendMessage("invalid (player)");
        else
            runnable.run();
    }
    CompletableFuture<Boolean> fetchIsUserInParty(UUID uuid, UUID partyUUID) {
        return fetchPgCallNonnullT("{? = call get_user_is_in_party(?, ?)}", Types.BOOLEAN, new Object[]{uuid, partyUUID}, Boolean.class);
    }
    void executeForeignOfflinePlayerCommand(OfflinePlayer offlinePlayerArg, AbstractPartyData partyData, UUID partyUuid, Player p, Runnable runnable) {
        executeOfflinePlayerCommand(offlinePlayerArg, p, () ->
                (partyData instanceof NetworkPartyData ? fetchIsUserInParty(offlinePlayerArg.getUniqueId(), partyUuid) : null)
                        .thenAccept(isInParty -> {
                            if (isInParty)
                                p.sendMessage("invalid (roster contains)");
                            else
                                runnable.run();
                        }));
    }
    void executePartyOfflinePlayerCommand(OfflinePlayer offlinePlayerArg, AbstractPartyData partyData, UUID partyUuid, Player p, Runnable runnable) {
        executeOfflinePlayerCommand(offlinePlayerArg, p, () ->
                (partyData instanceof NetworkPartyData ? fetchIsUserInParty(offlinePlayerArg.getUniqueId(), partyUuid) : null)
                        .thenAccept(isInParty -> {
                            if (!isInParty)
                                p.sendMessage("invalid (not in party)");
                            else
                                runnable.run();
                        }));
    }
    CompletableFuture<AbstractPartyData> fetchCreatableParty(AbstractPartyData[] mutableAbstractPartyData, PlayerData data, Player p) {
        CompletableFuture<AbstractPartyData> futureParty = new CompletableFuture<>();

        if (!(mutableAbstractPartyData[0] instanceof AbstractPartyData abstractPartyData)) {
            if (mutableAbstractPartyData instanceof NetworkPartyData[])
                fetchHandleNewNetworkPartyVoid(data, p)
                        .thenAccept(futureParty::complete);
            else {
                RuntimeException runtimeException = new RuntimeException();
                handlePotpissersExceptions(futureParty, runtimeException);
                throw runtimeException;
            }
        }
        else
            futureParty.complete(abstractPartyData);
        return futureParty;
    }
    Component getFactionMemberDataComponent(UUID factionUUID, ArrayList<HashMap<String, Object>> memberData) { // TODO -> red for dead members, yellow for in-network offline members
        List<Map.Entry<OfflinePlayer, Integer>> memberDataList = new ArrayList<>();
        for (HashMap<String, Object> dict : memberData)
            memberDataList.add(Map.entry(Bukkit.getOfflinePlayer((UUID)dict.get("user_uuid")), PARTY_RANK_LEVELS.get((String) dict.get("name"))));

        memberDataList.sort(Comparator
                .comparing(Map.Entry<OfflinePlayer, Integer>::getValue, Comparator.reverseOrder())
                .thenComparing(entry -> entry.getKey().getName()));

        Component memberComponent = getFWhoConsolePlainComponent("members: ");

        boolean isFirst = true;
        for (Map.Entry<OfflinePlayer, Integer> entry : memberDataList) {
            if (entry.getKey().isOnline()) {
                if (!isFirst)
                    memberComponent = memberComponent.append(getFWhoConsolePlainComponent(", "));

                memberComponent = memberComponent.append(Component.text("*".repeat(Math.max(0, entry.getValue())), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                memberComponent = memberComponent.append(Component.text(entry.getKey().getName(), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                isFirst = false;
            }
        }
        // TODO -> iterate once
        for (Map.Entry<OfflinePlayer, Integer> entry : memberDataList) {
            if (!entry.getKey().isOnline()) {
                if (!isFirst)
                    memberComponent = memberComponent.append(getFWhoConsolePlainComponent(", "));

                memberComponent = memberComponent.append(Component.text("*".repeat(Math.max(0, entry.getValue())), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                memberComponent = memberComponent.append(Component.text(entry.getKey().getName(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                isFirst = false;
                // TODO if player is death-banned meme
            }
        }

        long onlineCount = getOnlinePartyStream().filter(faction -> faction.uuid.equals(factionUUID))
                .findAny() // TODO -> do this in one iteration
                .map(faction -> faction.getOnlineRosterPlayerStream()
                        .count())
                .orElse(0L);

        return getFWhoConsolePlainComponent("player count: ")
                .append(getFWhoGreenPlainComponent(onlineCount + " / " + memberData.size()))
                .appendNewline()
                .append(memberComponent);
    }
    Component getOtherPartiesData(ArrayList<HashMap<String, Object>> dictList) {
        Component allies = getFWhoConsolePlainComponent("allies: ");
        boolean flagAllies = false;
        Component enemies = getFWhoConsolePlainComponent("enemies: ");
        boolean flagEnemies = false; // TODO -> get allies/enemies total members too
        for (HashMap<String, Object> dict : dictList) {
            UUID partyUUID = (UUID) dict.get("party_arg_uuid");
            AbstractPartyData party = getOnlinePartyStream().filter(party1 -> party1.uuid.equals(partyUUID))
                    .findAny()
                    .orElse(null);
            if (party != null || dict.get("name") != null) {
                if ((Boolean) dict.get("is_ally_else_enemy")) {
                    if (!flagAllies)
                        allies = allies.append(Component.text(", "));
                    allies = allies.append(Component.text((dict.get("name") instanceof String name ? name : "?'s party") + " (" + (party != null ? party.getOnlineRosterPlayerStream().count() : 0) + ")", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
                    flagAllies = true;
                } else {
                    if (!flagEnemies)
                        enemies = enemies.append(Component.text(", "));
                    enemies = enemies.append(Component.text((dict.get("name") instanceof String name ? name : "?'s party") + " (" + (party != null ? party.getOnlineRosterPlayerStream().count() : 0) + ")", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
                    flagEnemies = true;
                }
            }
        }
        return allies.appendNewline().append(enemies);
    }
    Component getFWhoConsolePlainComponent(String string) {
        return Component.text(string, NamedTextColor.GRAY, TextDecoration.ITALIC);
    }
    Component getFWhoGreenPlainComponent(String string) {
        return Component.text(string, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false);
    }
    void handleAbstractPartyWhoMessageSuffix(CompletableFuture<ArrayList<HashMap<String, Object>>> futureRelations, CompletableFuture<ArrayList<HashMap<String, Object>>> futureMembers, Player p, Component messagePrefix, UUID partyUUID) {
        futureRelations
                .thenAccept(factionRelations -> futureMembers
                        .thenAccept(factionMembers ->
                                p.sendMessage(messagePrefix
                                        .appendNewline()
                                        .append(getFactionMemberDataComponent(partyUUID, factionMembers))
                                        .appendNewline()
                                        .append(getOtherPartiesData(factionRelations)))));
    }
}
