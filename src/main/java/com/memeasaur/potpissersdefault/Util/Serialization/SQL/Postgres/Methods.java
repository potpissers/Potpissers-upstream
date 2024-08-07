package com.memeasaur.potpissersdefault.Util.Serialization.SQL.Postgres;

import com.memeasaur.potpissersdefault.Classes.ChatPrefix;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.memeasaur.potpissersdefault.Classes.ChatPrefix.CHAT_PREFIX_ID_MAP;
import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Crypto.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.CHAT_RANK_COLORS;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.fetchBukkitObject;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.*;

public class Methods {
    public static void executePgNotify(String channel, Object gsonPojo) {
        fetchQueryVoid(POSTGRES_POOL, "SELECT pg_notify(?, ?)", new Object[]{channel, gsonPojo});
    }

    public static CompletableFuture<String> fetchChatRankName(UUID uuid) {
        CompletableFuture<String> futureDonationRankName = fetchDonationRankName(uuid);
        return fetchStaffRankName(uuid)
                .thenCompose(optionalStaffRankName ->
                        optionalStaffRankName.<java.util.concurrent.CompletionStage<String>>map(CompletableFuture::completedFuture).orElse(futureDonationRankName));
    }
    public static CompletableFuture<ChatPrefix> fetchNullableChatPrefix(UUID uuid) {
        return fetchPgCallOptionalT("{? = CALL get_nullable_user_chat_prefix_id(?)}", Types.INTEGER, new Object[]{uuid}, Integer.class)
                .thenCompose(nullableId -> CompletableFuture.completedFuture(CHAT_PREFIX_ID_MAP.get(nullableId.orElse(null))));
    }
    public static CompletableFuture<NamedTextColor> fetchChatRankColor(UUID uuid) {
        return fetchChatRankName(uuid).thenCompose(name -> CompletableFuture.completedFuture(CHAT_RANK_COLORS.get(name)));
    }
    public static CompletableFuture<Void> fetchInsertUserReferralVoid(String ip, UUID uuid, @Nullable String string) {
        return fetchQueryVoid(POSTGRES_POOL, "call insert_user_referral(?, ?, ?, ?)", new Object[]{getHmacBytes(IP_REFERRAL_IP_HMAC_KEY, ip), string != null ? getAesGcmFixedIvBytes(IP_REFERRAL_REFERRER_KEY, string) : null, uuid, string});
    }
    public static CompletableFuture<Boolean> handleUserReferralDataFetchExists(UUID uuid, String ip) {
        CompletableFuture<Boolean> futureExists = new CompletableFuture<>();
        fetchNonnullDict(POSTGRES_POOL, "SELECT * FROM get_user_referral_data(?, ?)", new Object[]{uuid, getHmacBytes(IP_REFERRAL_IP_HMAC_KEY, ip)})
                .thenAccept(dict -> {
                    boolean exists = (boolean)dict.get("exists");
                    if (!exists && dict.get("nullable_refferer_bytes") instanceof byte[] bytes)
                        fetchInsertUserReferralVoid(ip, uuid, getAesGcmFixedIvString(IP_REFERRAL_REFERRER_KEY, bytes))
                                .thenRun(() -> futureExists.complete(Boolean.TRUE));
                    else {
                        if (exists && !(boolean)dict.get("ip_exists"))
                            fetchInsertUserReferralVoid(ip, uuid, dict.get("nullable_referrer") instanceof String referrer ? referrer : null);
                        futureExists.complete(exists);
                    }
                });
        return futureExists;
    }
    public static CompletableFuture<Optional<String>> fetchStaffRankName(UUID uuid) {
        return fetchPgCallOptionalT("{? = call get_user_staff_rank_name(?, ?)}", Types.VARCHAR, new Object[]{uuid, POSTGRESQL_SERVER_ID}, String.class);
    }
    public static CompletableFuture<String> fetchDonationRankName(UUID uuid) {
        return fetchPgCallNonnullT("{? = call get_donation_rank(?, ?)}", Types.VARCHAR, new Object[]{uuid, GAMEMODE_NAME}, String.class);
    }
    public static CompletableFuture<Void> fetchInsertReviveVoid(UUID revivedUuid, String reason, UUID reviverUuid) {
        return fetchQueryVoid(POSTGRES_POOL, "call insert_revive(?, ?, ?, ?)", new Object[]{revivedUuid, POSTGRESQL_SERVER_ID, reason, reviverUuid});
    }
    public static CompletableFuture<ItemStack[]> fetchDefaultKitContents(String kitName) {
        return fetchPgCallNonnullT("{? = call get_kit_bukkit_default_contents(?)}", Types.BINARY, new Object[]{kitName}, byte[].class)
                .thenCompose(bytes -> fetchBukkitObject(bytes, ItemStack[].class));
//        CompletableFuture<ItemStack[]> futureKitBytes = new CompletableFuture<>(); // bro wtf was this
//        SCHEDULER.runTaskAsynchronously(plugin, () -> {
//            try (Connection connection = POSTGRES_POOL.getConnection(); PreparedStatement preparedStatement = getOpenPreparedStatement(connection, RETURN_BUKKIT_DEFAULT_KIT_CONTENTS, new Object[]{kitName});
//                 ResultSet resultSet = preparedStatement.executeQuery()) {
//                if (resultSet.next()) {
//                    ItemStack[] kitContents = getBukkitObjectBlocking(resultSet.getBytes("bukkit_default_loadout"), ItemStack[].class);
//                    SCHEDULER.runTask(plugin, () ->
//                            futureKitBytes.complete(kitContents));
//                }
//                else
//                    throw new RuntimeException("kit erorr :)");
//            } catch (SQLException | IOException | ClassNotFoundException e) {
//                throw new RuntimeException(e);
//            }
//        });
//        return futureKitBytes;
    }
    public static void executeDefaultKitNamesCacheUpdate() {
        fetchPgCallNonnullT("{? = call get_kit_names()}", Types.ARRAY, null, String[].class)
                .thenAccept(array -> immutableDefaultKitNamesCache = List.of(array));
    }
    public static void executeDefaultConsumableKitNamesCacheUpdate() {
        fetchPgCallNonnullT("{? = call get_server_consumable_kit_names(?)}", Types.ARRAY, new Object[]{POSTGRESQL_SERVER_ID}, String[].class)
                .thenAccept(list -> immutableDefaultKitNamesCache = List.of(list));
    }
    public static void executeArenaNamesCacheUpdate() {
        fetchPgCallNonnullT("{? = call get_arena_names(?)}", Types.ARRAY, new Object[]{POSTGRESQL_SERVER_ID}, String[].class)
                .thenAccept(list -> arenaNamesCache = List.of(list));
    }
}
