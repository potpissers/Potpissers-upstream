package com.memeasaur.potpissersdefault.Classes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.memeasaur.potpissersdefault.Classes.NetworkPartyData.fetchExistingNetworkPartyUUID;
import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods1.sendPotpissersPluginMessage;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.*;

public abstract class AbstractPartyData extends AbstractData { // TODO -> implements commandExecutor (?)
    public static Stream<? extends AbstractPartyData> getOnlinePartyStream() {
        return playerDataCache.values().stream()
                .flatMap(PlayerData::getCurrentParties)
                .distinct();
    }
    public static CompletableFuture<Optional<UUID>> fetchOptionalMainPartyUUID(UUID playerUUID) { // TODO -> this should get the main party, then secondary
        CompletableFuture<Optional<UUID>> futureOptionalNetworkPartyUUID = fetchExistingNetworkPartyUUID(playerUUID);
        return futureOptionalNetworkPartyUUID; // !
    }
    public static CompletableFuture<String> fetchNonnullPartyName(UUID partyUuid) {
        CompletableFuture<Optional<UUID>> futureOptionalPartyLeaderUUID = fetchPgCallOptionalT("{? = call get_party_leader_uuid(?)}", Types.OTHER, new Object[]{partyUuid}, UUID.class);

        return futureOptionalPartyLeaderUUID
                .thenCompose(optionalLeaderUuid -> {
                    if (optionalLeaderUuid.isPresent())
                        return CompletableFuture.completedFuture(Bukkit.getOfflinePlayer(optionalLeaderUuid.get()).getName() + "'s party");
                    else
                        return CompletableFuture.completedFuture("null");
                });
    }
    public static void executeNetworkPartyMessage(UUID partyUUID, CompletableFuture<String> futurePartyName, Component msg) {
        futurePartyName.thenAccept(partyName ->
                sendPotpissersPluginMessage(PLUGIN_PARTY_MESSAGER, new JSONObject(Map.of("partyUuid", partyUUID.toString(), "msg", JSONComponentSerializer.json().serialize(getPartyConsoleComponent("(" + partyName + ") ").append(msg)))).toJSONString().getBytes(StandardCharsets.UTF_8)));
    }

    public abstract Stream<PlayerData> getOnlineRosterPlayerDataStream();
    public abstract Stream<? extends Player> getOnlineRosterPlayerStream();

    public abstract CompletableFuture<String> fetchNonnullPlayerRankName(UUID uuid);;

    public abstract CompletableFuture<Void> fetchKickVoid(UUID kickedUuid);

    public abstract CompletableFuture<Void> fetchJoinVoid(UUID playerUUID, String perms);

    public abstract CompletableFuture<Void> fetchDisbandVoid();

    public abstract void executeLeaderChange(UUID newLeaderUUID);

    public final HashSet<UUID> allyCache;
    public final HashSet<UUID> enemyCache;
    public static CompletableFuture<Optional<Boolean>> fetchOptionalPartyRelation(UUID partyUUID, UUID partyArgUUID) {
        return fetchPgCallOptionalT("{? = call get_party_relation_is_ally_else_enemy(?, ?)}", Types.BOOLEAN, new Object[]{partyUUID, partyArgUUID}, Boolean.class);
    }

    protected AbstractPartyData(UUID partyUuid, HashSet<UUID> allyCache, HashSet<UUID> enemyCache) {
        super(partyUuid);
        this.allyCache = allyCache;
        this.enemyCache = enemyCache;
    }
    private static CompletableFuture<Void> fetchDeleteRelationVoid(UUID partyUUID, UUID partyArgUUID) {
        return fetchQueryVoid(POSTGRES_POOL, "call delete_party_relation(?, ?)", new Object[]{partyUUID, partyArgUUID});
    }
    public CompletableFuture<Void> fetchNetworkUnAllyVoid(UUID partyArgUUID, CompletableFuture<String> futurePartyName, CompletableFuture<String> futurePartyArgName, String pName, String pArgName) {
        CompletableFuture<Void> futureUnAllyVoid = CompletableFuture.allOf(fetchDeleteRelationVoid(this.uuid, partyArgUUID), fetchDeleteRelationVoid(partyArgUUID, this.uuid));

        return futurePartyName
                .thenAccept(partyName -> futurePartyArgName
                        .thenAccept(partyArgName ->
                                futureUnAllyVoid
                                        .thenRun(() -> {
                                            sendPotpissersPluginMessage(PLUGIN_PARTY_UNALLIER, new JSONObject(Map.of("partyUUIDString", this.uuid.toString(), "partyArgUUIDString", partyArgUUID.toString())).toJSONString().getBytes(StandardCharsets.UTF_8));
                                            executeNetworkPartyMessage(this.uuid, futurePartyName, getNormalComponent(pName + " has un-allied " + partyArgName + " (" + pArgName + ")"));
                                            executeNetworkPartyMessage(partyArgUUID, futurePartyName, getFocusComponent(partyName + " has un-allied your party"));
                                        })));
    }
    public static CompletableFuture<Void> fetchNetworkUnEnemyVoid(UUID partyUUID, UUID partyArgUUID, String pName, String pArgName) {
        CompletableFuture<Void> futureUnEnemyVoid = fetchDeleteRelationVoid(partyUUID, partyArgUUID);
        CompletableFuture<String> futurePartyArgName = fetchNonnullPartyName(partyArgUUID);

        CompletableFuture<String> futurePartyName = fetchNonnullPartyName(partyUUID);
        return futurePartyName
                .thenAccept(partyName -> futurePartyArgName
                        .thenAccept(partyArgName -> futureUnEnemyVoid
                                .thenRun(() -> {
                                    sendPotpissersPluginMessage(PLUGIN_PARTY_UNENEMIER, new JSONObject(Map.of("partyUUIDString", partyUUID.toString(), "partyArgUUIDString", partyArgUUID.toString())).toJSONString().getBytes(StandardCharsets.UTF_8));
                                    executeNetworkPartyMessage(partyUUID, futurePartyName, getNormalComponent(pName + " has un-enemied " + partyArgName + " (" + pArgName + ")"));
                                    executeNetworkPartyMessage(partyArgUUID, futurePartyName, getFocusComponent(partyName + " has un-enemied your party"));
                                })));
    }

    public static CompletableFuture<Void> fetchCreateRelationVoid(UUID partyUUID, UUID partyArgUUID, Boolean isAllyElseEnemy) {
        return fetchQueryVoid(POSTGRES_POOL, "call insert_party_relation(?, ?, ?)", new Object[]{partyUUID, partyArgUUID, isAllyElseEnemy});
    }
    public CompletableFuture<Optional<String>> fetchOptionalInviteRankName(UUID playerUuid) {
        return fetchPgCallOptionalT("{? = call get_user_party_invite_rank_name(?, ?)}", Types.VARCHAR, new Object[]{playerUuid, this.uuid}, String.class);
    }

    public CompletableFuture<Void> executeLeaveReturnVoid(String rankName, Player p, UUID playerUUID, PlayerData data) {
        CompletableFuture<String> futurePartyName = fetchName();
        futurePartyName
                .thenAccept(partyName -> {
                    if (PARTY_RANK_LEVELS.get(rankName) > PARTY_RANK_LEVELS.get(PARTY_RANK_MEMBER))
                        executeNetworkPartyMessage(this.uuid, futurePartyName, getNormalComponent(p.getName() + " has demoted themselves from " + rankName));
                    executeNetworkPartyMessage(this.uuid, futurePartyName, getNormalComponent(p.getName() + " has left the party"));
                });

        return this.fetchKickVoid(playerUUID)
                .thenRun(() -> data.handleAbstractPartyLeave(uuid));
    }

    public void executeNetworkPromotion(UUID promotedUUID, String newRank, CompletableFuture<String> futurePartyName, String promoterName, String promotedName) {
        fetchJoinVoid(promotedUUID, newRank);
        executeNetworkPartyMessage(this.uuid, futurePartyName, getNormalComponent(promoterName + " has promoted " + promotedName + " to " + newRank));
    }

    @Override
    public boolean isInSpawn() {
        StringBuilder stringBuilder = new StringBuilder().append("out of spawn:");
        boolean flag = false;
        for (PlayerData data : getOnlineRosterPlayerDataStream().toList())
            if (!data.isInSpawn()) {
                stringBuilder.append(" ").append(Bukkit.getOfflinePlayer(data.uuid).getName());
                flag = true;
            }
        if (flag)
            executeNetworkPartyMessage(this.uuid, fetchName(), getDangerComponent(String.valueOf(stringBuilder)));
        return flag;
    }

    @Override
    protected boolean isSubclassQueued() {
        return partyDuelQueue.containsKey(this);
    }
    @Override
    protected void handleSubclassQueueCancel() {
        partyDuelQueue.remove(this);
    }

    @Override
    protected CompletableFuture<Void> fetchHandleDuelMapVoid(ConcurrentHashMap<UUID, String> map) {
        return fetchName()
                .thenAccept(name -> getOnlineRosterPlayerStream()
                        .forEach(player -> map.put(player.getUniqueId(), name)));
    }
    @Override
    protected void executeNetworkMessage(Component msg) {
        executeNetworkPartyMessage(this.uuid, fetchName(), msg);
    }
    @Override
    protected CompletableFuture<String> fetchName() {
        return fetchNonnullPartyName(this.uuid);
    }
    public CompletableFuture<Boolean> fetchIsPlayerOfficer(UUID uuid) {
        CompletableFuture<Boolean> futureBoolean = new CompletableFuture<>();
        this.fetchNonnullPlayerRankName(uuid)
                .thenAccept(rankName ->
                        futureBoolean.complete(!rankName.equals(PARTY_RANK_MEMBER)));
        return futureBoolean;
    }
}

