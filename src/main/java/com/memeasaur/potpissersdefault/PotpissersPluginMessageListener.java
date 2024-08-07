package com.memeasaur.potpissersdefault;

import com.memeasaur.potpissersdefault.Classes.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

import static com.memeasaur.potpissersdefault.Classes.AbstractPartyData.getOnlinePartyStream;
import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getFocusComponent;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.handlePotpissersExceptions;

public class PotpissersPluginMessageListener implements PluginMessageListener {
    @Override
    public void onPluginMessageReceived(@NotNull String s, @NotNull Player player, @NotNull byte[] bytes) {
        final JSONObject jsonObject; {
            try { // TODO -> use gson
                jsonObject = ((JSONObject) JSON_PARSER.parse(new String(bytes, StandardCharsets.UTF_8)));
            } catch (ParseException e) {
                handlePotpissersExceptions(null, e);
                throw new RuntimeException(e);
            }
        }
        switch (s) {
            case PLUGIN_PLAYER_MESSAGER -> {
                if (Bukkit.getPlayer(UUID.fromString((String) jsonObject.get("playerUUID"))) instanceof Player p)
                    p.sendMessage(JSONComponentSerializer.json().deserialize((String) jsonObject.get("msg")));
            }
            case PLUGIN_PARTY_MESSAGER -> {
                UUID partyUuid = UUID.fromString((String) jsonObject.get("partyUuid"));
                Component message = JSONComponentSerializer.json().deserialize((String) jsonObject.get("msg"));

                playerDataCache.values().stream()
                        .filter(data -> data.getCurrentParties()
                                .anyMatch(party -> party.uuid.equals(partyUuid)))
                        .map(data -> Bukkit.getPlayer(data.uuid))
                        .filter(Objects::nonNull)
                        .forEach(partyPlayer -> partyPlayer.sendMessage(message));
            }
            case PLUGIN_PARTY_UNALLIER -> {
                UUID partyUUID = UUID.fromString((String) jsonObject.get("partyUUIDString"));
                UUID partyArgUUID = UUID.fromString((String) jsonObject.get("partyArgUUIDString"));

                getOnlinePartyStream()
                        .forEach(party -> {
                            if (party.uuid.equals(partyUUID)) {
                                party.allyCache.remove(partyArgUUID);
                            }
                            else if (party.uuid.equals(partyArgUUID)) {
                                party.allyCache.remove(partyUUID);
                            }
                        });
            }
            case PLUGIN_PARTY_UNENEMIER -> {
                UUID partyUUID = UUID.fromString((String) jsonObject.get("partyUUIDString"));
                UUID partyArgUUID = UUID.fromString((String) jsonObject.get("partyArgUUIDString"));

                getOnlinePartyStream().filter(party -> party.uuid.equals(partyUUID))
                        .findAny()
                        .ifPresent(party -> {
                            party.enemyCache.remove(partyArgUUID);
                        });
            }
            case PLUGIN_PARTY_ALLIER -> {
                UUID partyUUID = UUID.fromString((String) jsonObject.get("partyUUIDString"));
                UUID partyArgUUID = UUID.fromString((String) jsonObject.get("partyArgUUIDString"));

                getOnlinePartyStream()
                        .forEach(party -> {
                            if (party.uuid.equals(partyUUID)) {
                                party.allyCache.add(partyArgUUID);
                            }
                            else if (party.uuid.equals(partyArgUUID)) {
                                party.allyCache.add(partyUUID);
                            }
                        });
            }
            case PLUGIN_PARTY_ENEMIER -> {
                UUID partyUUID = UUID.fromString((String) jsonObject.get("partyUUIDString"));
                UUID partyArgUUID = UUID.fromString((String) jsonObject.get("partyArgUUIDString"));

                getOnlinePartyStream().filter(party -> party.uuid.equals(partyUUID))
                        .findAny()
                        .ifPresent(party -> {
                            party.enemyCache.add(partyArgUUID);
                        });
            }
            case PLUGIN_PARTY_DISBANDER -> {
                UUID partyUUID = UUID.fromString((String) jsonObject.get("partyUUIDString"));

                for (PlayerData playerData : playerDataCache.values())
                    playerData.handleAbstractPartyLeave(partyUUID);
            }
            case PLUGIN_PARTY_KICKER -> {
                UUID playerUUID = UUID.fromString((String) jsonObject.get("playerUUIDString"));

                playerDataCache.values().stream().filter(data -> data.uuid.equals(playerUUID)).findAny()
                        .ifPresent(data -> {
                            data.handleAbstractPartyLeave(UUID.fromString((String) jsonObject.get("partyUUIDString")));
                            if (Bukkit.getPlayer(playerUUID) instanceof Player p)
                                p.sendMessage(getFocusComponent(jsonObject.get("partyName") + " has kicked you"));
                        });
            }
        }
    }
}
