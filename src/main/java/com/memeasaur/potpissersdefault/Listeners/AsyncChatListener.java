package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.*;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.memeasaur.potpissersdefault.Classes.AbstractPartyData.executeNetworkPartyMessage;
import static com.memeasaur.potpissersdefault.Classes.AbstractPartyData.fetchNonnullPartyName;
import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Crypto.CURRENT_PUNISHMENTS_IP_HMAC_KEY;
import static com.memeasaur.potpissersdefault.Util.Crypto.getHmacBytes;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getDangerComponent;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.handlePotpissersExceptions;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Postgres.Methods.*;

public class AsyncChatListener implements Listener {
    @EventHandler
    void onPlayerChat(AsyncChatEvent e) {
        e.setCancelled(true);
        Player p = e.getPlayer();
        PlayerData data = playerDataCache.get(p);
        UUID uuid = data.uuid;

        CompletableFuture<Optional<HashMap<String, Object>>> futureFarthestUUIDMuteDict = fetchOptionalDict(POSTGRES_POOL, "SELECT * FROM get_farthest_user_punishment(?, ?, ?)", new Object[]{"mute", uuid, POSTGRESQL_SERVER_ID});
        CompletableFuture<Optional<HashMap<String, Object>>> futureFarthestIpMuteDict = fetchOptionalDict(POSTGRES_POOL, "SELECT * FROM get_farthest_user_ip_punishment(?, ?, ?, ?)", new Object[]{uuid, getHmacBytes(CURRENT_PUNISHMENTS_IP_HMAC_KEY, data.hostAddress), POSTGRESQL_SERVER_ID, "mute"});
        CompletableFuture<ChatPrefix> futureNullableChatPrefix = fetchNullableChatPrefix(uuid);

//        Component originalComponentMessage = ;

        String tempMessageString = PlainTextComponentSerializer.plainText().serialize(e.originalMessage());
        CompletableFuture<String> futureChatType;
        if (!tempMessageString.isEmpty())
            switch (tempMessageString.charAt(0)) {
                case '#' -> {
                    futureChatType = CompletableFuture.completedFuture(CHAT_ALLY);
                    tempMessageString = tempMessageString.substring(1);
                }
                case '!' -> {
                    futureChatType = CompletableFuture.completedFuture(CHAT_SERVER);
                    tempMessageString = tempMessageString.substring(1);
                }
                case '@' -> {
                    futureChatType = CompletableFuture.completedFuture(CHAT_FACTION);
                    tempMessageString = tempMessageString.substring(1);
                }
                case '$' -> {
                    futureChatType = CompletableFuture.completedFuture(CHAT_LOCAL);
                    tempMessageString = tempMessageString.substring(1);
                }
                default ->
                        futureChatType = fetchPgCallNonnullT("{? = call get_user_chat_type(?)}", Types.VARCHAR, new Object[]{uuid}, String.class);
            }
        else
            futureChatType = fetchPgCallNonnullT("{? = call get_user_chat_type(?)}", Types.VARCHAR, new Object[]{uuid}, String.class);

        String messageString = tempMessageString;

        fetchChatRankColor(uuid)
                .thenAccept(chatRankColor -> {

                    String preColoredName = "<" + p.getName() + "> ";
                    Component coloredName = Component.text(preColoredName, chatRankColor);

                    String finalFullMessageString = preColoredName + messageString;
                    Component finalOriginalMessageComponent = Component.text(messageString)
                            .clickEvent(ClickEvent.suggestCommand(finalFullMessageString));

                    futureChatType
                            .thenAccept(chatType -> {
                                switch (chatType) {
                                    case CHAT_LOCAL -> {
                                        Component localMessageBody = finalOriginalMessageComponent.color(NamedTextColor.YELLOW);
                                        Component message = coloredName.append(localMessageBody);
                                        SCHEDULER.runTask(plugin, () -> { // TODO ?
                                            for (Player player : p.getLocation().getNearbyPlayers(72)) { // TODO magic number
                                                Component localDistance = Component.text(" [" + Math.round(p.getLocation().distance(player.getLocation())) + "]").color(NamedTextColor.YELLOW);
                                                player.sendMessage(message.append(localDistance));
                                            }
                                        });
//                for (Player player : Bukkit.getOnlinePlayers()) {
//                    if (player)
//                } // TODO show ops all local chats
                                    }
                                    case CHAT_PARTY -> {
                                        Component partyMessageBody = finalOriginalMessageComponent.color(NamedTextColor.GREEN);
                                        Component message = coloredName.append(partyMessageBody);
                                        if (data.mutableNetworkParty[0] instanceof AbstractPartyData party)
                                            executeNetworkPartyMessage(party.uuid, fetchNonnullPartyName(party.uuid), message);
                                            // TODO -> faction too
                                        else
                                            p.sendMessage(message);
                                    }
                                    case CHAT_ALLY -> {
                                        Component allyMessageBody = finalOriginalMessageComponent.color(NamedTextColor.AQUA);
                                        Component message = coloredName.append(allyMessageBody);
                                        if (data.getActiveMutableParty()[0] instanceof AbstractPartyData party) {
                                            CompletableFuture<String> futurePartyName = fetchNonnullPartyName(party.uuid);
                                            party.allyCache.forEach(allyUUID ->
                                                    executeNetworkPartyMessage(allyUUID, futurePartyName, message));
                                            executeNetworkPartyMessage(party.uuid, futurePartyName, message);
                                        } else
                                            p.sendMessage(message);
                                    }
                                    case CHAT_SERVER -> futureFarthestUUIDMuteDict
                                            .thenAccept(optionalFarthestUUIDMute -> futureFarthestIpMuteDict
                                                    .thenAccept(optionalFarthestIpMute ->
                                                            futureNullableChatPrefix.thenAccept(nullableChatPrefix -> {
                                                                try {
                                                                    if ((optionalFarthestUUIDMute.orElse(null) instanceof HashMap<String, Object> dict && ((Timestamp)dict.get("expiration")).toLocalDateTime().isAfter(LocalDateTime.now()))) {
                                                                        e.setCancelled(true);
                                                                        p.sendMessage(getDangerComponent("muted " + Duration.between(LocalDateTime.now(), ((Timestamp) dict.get("expiration")).toLocalDateTime()).toSeconds() + "s: " + dict.get("reason")));
                                                                        return;
                                                                    } else if (optionalFarthestIpMute.orElse(null) instanceof HashMap<String, Object> dict && ((Timestamp) dict.get("expiration")).toLocalDateTime().isAfter(LocalDateTime.now())) {
                                                                        e.setCancelled(true);
                                                                        p.sendMessage(getDangerComponent("ip-muted " + Duration.between(LocalDateTime.now(), ((Timestamp) dict.get("expiration")).toLocalDateTime()).toSeconds() + "s: " + dict.get("reason")));
                                                                        return;
                                                                    } else {
                                                                        Component publicMessageBody = finalOriginalMessageComponent.color(NamedTextColor.WHITE);
                                                                        Component prefixedColoredName = nullableChatPrefix == null
                                                                                ? coloredName
                                                                                : nullableChatPrefix.component.appendSpace().append(coloredName);
                                                                        Component message = prefixedColoredName.append(publicMessageBody);
                                                                        for (Player player : Bukkit.getOnlinePlayers())
                                                                            player.sendMessage(message);

                                                                        fetchQueryVoid(POSTGRES_POOL, "CALL handle_insert_web_chat_history(?, ?, ?, ?)", new Object[]{uuid, finalFullMessageString, GAMEMODE_NAME, GAMEMODE_NAME_SUFFIX});
                                                                    }
                                                                } catch (Exception ex) {
                                                                    handlePotpissersExceptions(null, ex);
                                                                }
                                                            })));
                                }
                            });
                });
    }
}
