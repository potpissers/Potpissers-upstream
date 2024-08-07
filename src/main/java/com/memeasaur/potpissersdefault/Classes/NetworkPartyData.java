package com.memeasaur.potpissersdefault.Classes;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Types;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.memeasaur.potpissersdefault.PotpissersDefault.playerDataCache;
import static com.memeasaur.potpissersdefault.PotpissersDefault.POSTGRES_POOL;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.PARTY_RANK_LEADER;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getNormalComponent;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.*;

public class NetworkPartyData extends AbstractPartyData {
    private NetworkPartyData(UUID partyUuid, HashSet<UUID> allyCache, HashSet<UUID> enemyCache) {
        super(partyUuid, allyCache, enemyCache);
    }
    public static CompletableFuture<Optional<UUID>> fetchExistingNetworkPartyUUID(UUID playerUUID) {
        return fetchPgCallOptionalT("{? = call get_user_party_uuid(?)}", Types.OTHER, new Object[]{playerUUID}, UUID.class);
    }
    public static CompletableFuture<NetworkPartyData> fetchHandleNewNetworkPartyVoid(PlayerData data, Player p) {
        UUID partyUUID = UUID.randomUUID();
        return fetchQueryVoid(POSTGRES_POOL, "call handle_insert_party_member(?, ?, ?)", new Object[]{data.uuid, partyUUID, PARTY_RANK_LEADER})
                .thenCompose(v -> {
                    data.mutableNetworkParty[0] = new NetworkPartyData(partyUUID, new HashSet<>(), new HashSet<>());
                    p.sendMessage(getNormalComponent("party created"));
                    return CompletableFuture.completedFuture(data.mutableNetworkParty[0]);
                });
    }
    public static CompletableFuture<Optional<NetworkPartyData>> fetchExistingNetworkParty(UUID playerUuid) {
        return fetchExistingNetworkPartyUUID(playerUuid)
                .thenCompose(optionalPartyUUID -> {
                    if (optionalPartyUUID.isEmpty()) // TODO -> method-ize this for factionData
                        return CompletableFuture.completedFuture(Optional.empty());
                    else {
                        UUID partyUUID = optionalPartyUUID.get();
                        if (getOnlinePartyStream().filter(party -> party.uuid.equals(partyUUID)).findAny().orElse(null) instanceof NetworkPartyData partyData)
                            return CompletableFuture.completedFuture(Optional.of(partyData));
                        else
                            return fetchDictList(POSTGRES_POOL, "SELECT * FROM get_party_relations(?)", new Object[]{partyUUID})
                                .thenCompose(dictList -> {
                                    HashSet<UUID> allies = new HashSet<>();
                                    HashSet<UUID> enemies = new HashSet<>();
                                    for (HashMap<String, Object> dict : dictList) {
                                        if ((Boolean) dict.get("is_ally_else_enemy"))
                                            allies.add((UUID) dict.get("party_arg_uuid"));
                                        else
                                            enemies.add((UUID) dict.get("party_arg_uuid"));
                                    }
                                    return CompletableFuture.completedFuture(Optional.of(new NetworkPartyData(partyUUID, allies, enemies)));
                                });
                    }
                });
    }
    @Override
    public CompletableFuture<Void> fetchDisbandVoid() {
        return fetchQueryVoid(POSTGRES_POOL, "call handle_network_party_delete(?)", new Object[]{this.uuid});
    }
    @Override
    public CompletableFuture<Void> fetchKickVoid(UUID kickedPlayerUuid) {
        return fetchQueryVoid(POSTGRES_POOL, "call delete_user_party(?, ?)", new Object[]{kickedPlayerUuid, this.uuid});
    }

    @Override
    public CompletableFuture<Void> fetchJoinVoid(UUID playerUUID, String perms) {
        return fetchQueryVoid(POSTGRES_POOL, "call delete_party_invite(?, ?)", new Object[]{playerUUID, uuid})
                .thenCompose(v -> fetchQueryVoid(POSTGRES_POOL, "call insert_current_parties_members_user(?, ?, ?)", new Object[]{playerUUID, uuid, perms}));
    }

    @Override
    public CompletableFuture<String> fetchNonnullPlayerRankName(UUID uuid) {
        return fetchPgCallNonnullT("{? = call get_user_network_party_rank_name(?)}", Types.VARCHAR, new Object[]{uuid}, String.class);
    }

    @Override
    public void executeLeaderChange(UUID newLeaderUUID) {
        fetchQueryVoid(POSTGRES_POOL, "call handle_update_party_leader(?, ?)", new Object[]{this.uuid, newLeaderUUID});
    }

    @Override
    public Stream<PlayerData> getOnlineRosterPlayerDataStream() {
        return playerDataCache.values().stream()
                .filter(data -> data.mutableNetworkParty[0] == this);
    }

    @Override
    public Stream<? extends Player> getOnlineRosterPlayerStream() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> playerDataCache.get(player).mutableNetworkParty[0] == this);
    }
}
