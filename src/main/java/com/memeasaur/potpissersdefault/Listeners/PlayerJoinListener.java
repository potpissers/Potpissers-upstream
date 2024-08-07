package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.PermissionAttachment;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Crypto.CURRENT_PUNISHMENTS_IP_HMAC_KEY;
import static com.memeasaur.potpissersdefault.Util.Crypto.getHmacBytes;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods1.executeDeathbanCheck;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.handlePotpissersExceptions;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Postgres.Methods.*;

public class PlayerJoinListener implements Listener {
    @EventHandler
    void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        p.getAttribute(Attribute.ARMOR).setBaseValue(0);
        final Component JOIN_MSG = e.joinMessage();
        if (!p.isOp()) {
            // Duels start
            if (!p.isVisibleByDefault())
                p.setVisibleByDefault(true);
            // Duels end
            e.joinMessage(null);
            PermissionAttachment permissionAttachment = p.addAttachment(plugin);
            permissionAttachment.setPermission("bukkit.command.plugins", false);
            permissionAttachment.setPermission("minecraft.command.say", true);
        }
        UUID uuid = p.getUniqueId();
        fetchQueryVoid(POSTGRES_POOL, "CALL upsert_user_data(?)", new Object[]{uuid});

        fetchOptionalDict(POSTGRES_POOL, "SELECT * FROM get_farthest_user_punishment(?, ?, ?)", new Object[]{"ban", uuid, POSTGRESQL_SERVER_ID})
                .thenAccept(optional -> optional
                        .ifPresent(dict -> {
                            try {
                                p.kick(getDangerComponent("banned " + Duration.between(LocalDateTime.now(), ((Timestamp)dict.get("expiration")).toLocalDateTime()).toSeconds() + "s: " + dict.get("reason")));
                            }
                            catch (Exception ex) {
                                handlePotpissersExceptions(null, new RuntimeException(ex));
                            }
                        }));

        String playerAddress = p.getAddress().getAddress().getHostAddress();
        fetchOptionalDict(POSTGRES_POOL, "SELECT * FROM get_farthest_user_ip_punishment(?, ?, ?, ?)", new Object[]{uuid, getHmacBytes(CURRENT_PUNISHMENTS_IP_HMAC_KEY, playerAddress), POSTGRESQL_SERVER_ID, "ban"})
                .thenAccept(optional -> optional
                        .ifPresent(dict -> {
                            try {
                                p.kick(getDangerComponent("ip-banned " + Duration.between(LocalDateTime.now(), ((Timestamp)dict.get("expiration")).toLocalDateTime()).toSeconds() + "s: " + dict.get("reason")));
                            }
                            catch (Exception ex) {
                                handlePotpissersExceptions(null, ex);
                            }
                        }));

        // Deathbanz start
        executeDeathbanCheck(uuid, p, playerAddress);
        // Deathbanz end

        if (playerLoggerCache.containsKey(uuid)) {
            Piglin piglin = playerLoggerCache.get(uuid);
            LoggerData loggerData = loggerDataCache.get(piglin);

            p.setHealth(piglin.getHealth());
            p.setAbsorptionAmount(piglin.getAbsorptionAmount());
            p.addPotionEffects(piglin.getActivePotionEffects());
            p.getInventory().setContents(loggerData.playerInventory);
            p.setFireTicks(piglin.getFireTicks());
            p.setFreezeTicks(piglin.getFreezeTicks());
            p.setArrowsInBody(piglin.getArrowsInBody());
            p.setFallDistance(piglin.getFallDistance());
            p.teleport(piglin);
            p.setVelocity(piglin.getVelocity());

            loggerDataCache.remove(piglin);
            playerLoggerCache.remove(uuid);
            piglin.remove();

            loggerData.playerData.hostAddress = playerAddress;
            p.setScoreboard(loggerData.scoreboard); // playerData creation also handles this
            handlePlayerDataLogin(uuid, p, loggerData.playerData);
            return;
        }
//        else if (playerDataCache.get(p) instanceof PlayerData data) {
//            TODO dunno, this would be nice but logger update probably fuck-y
//        }
        else
            PlayerData.handlePlayerLogin(uuid, p, playerAddress)
                    .thenAccept(data -> {
                        try {
                            handlePlayerDataLogin(uuid, p, data);
                        }
                        catch (Exception ex) {
                            handlePotpissersExceptions(null, ex);
                        }
                    });
    }

    void handlePlayerDataLogin(UUID uuid, Player p, PlayerData data) {
        fetchStaffRankName(uuid)
                .thenAccept(optionalStaffRank -> optionalStaffRank
                        .ifPresent(staffRank -> {
                            try {
                                switch (staffRank) {
                                    case CHAT_RANK_WATCHER -> {
                                        PermissionAttachment perms = p.addAttachment(plugin);
                                        perms.setPermission("potpissers.helper", true);
                                        perms.setPermission("minecraft.command.weather", true);
                                        perms.setPermission("minecraft.command.time", true);

                                        perms.setPermission("potpissers.watcher", true);
                                        perms.setPermission("worldedit.navigation.jumpto.tool", true);
                                        perms.setPermission("worldedit.navigation.thru.tool", true);
                                        perms.setPermission("minecraft.command.playsound", true);
                                        perms.setPermission("minecraft.command.ride", true);
                                    }
                                    case CHAT_RANK_MOD -> {
                                        PermissionAttachment perms = p.addAttachment(plugin);
                                        perms.setPermission("potpissers.helper", true);
                                        perms.setPermission("minecraft.command.weather", true);
                                        perms.setPermission("minecraft.command.time", true);

                                        perms.setPermission("potpissers.watcher", true);
                                        perms.setPermission("worldedit.navigation.jumpto.tool", true);
                                        perms.setPermission("worldedit.navigation.thru.tool", true);
                                        perms.setPermission("minecraft.command.playsound", true);
                                        perms.setPermission("minecraft.command.ride", true);

                                        perms.setPermission("potpissers.mod", true);
                                        perms.setPermission("minecraft.command.whitelist", true);
                                        perms.setPermission("minecraft.command.stop", true);
                                        perms.setPermission("minecraft.command.clear", true);
                                        perms.setPermission("minecraft.command.damage", true);
                                        perms.setPermission("minecraft.command.kick", true);
                                        perms.setPermission("minecraft.command.kill", true);

                                    }
                                    case CHAT_RANK_ADMIN -> {
                                        PermissionAttachment perms = p.addAttachment(plugin);
                                        perms.setPermission("potpissers.helper", true);
                                        perms.setPermission("potpissers.watcher", true);
                                        perms.setPermission("potpissers.mod", true);
                                    }
                                }
                            }
                            catch (Exception e) {
                                handlePotpissersExceptions(null, e);
                            }
                        }));
        fetchPgCallNonnullT("{? = call get_user_id_chat_mod(?)}", Types.BOOLEAN, new Object[]{uuid}, Boolean.class)
                .thenAccept(isChatMod -> {
                    try {
                        if (isChatMod) {
                            PermissionAttachment perms = p.addAttachment(plugin);
                            perms.setPermission("potpissers.helper", true);
                            perms.setPermission("minecraft.command.weather", true);
                            perms.setPermission("minecraft.command.time", true);
                        }
                    }
                    catch (Exception e) {
                        handlePotpissersExceptions(null, e);
                    }
                });

        playerDataCache.put(p, data);

        String ip = p.getAddress().getAddress().getHostAddress();
        handleUserReferralDataFetchExists(uuid, ip)
                .thenAccept(isNotNew -> {
                    if (!isNotNew) {
                        switch (data.hostAddress.toLowerCase()) {
                            case "camwen.potpissers.com" ->
                                fetchInsertUserReferralVoid(ip, uuid, "camwen");
                            default -> {
                                p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BANJO, 1f, 1f);
                                p.sendMessage(getFocusComponent("hey, new player: any time during this first connection, please type /ref (name) to give referral credit to who/whatever sent you here").decorate(TextDecoration.BOLD));
                            }
                        }
                    }
                });

        if (IS_JOIN_SPAWN_TELEPORT && hubKit != null) {
            p.teleport(p.getWorld().getSpawnLocation());
            p.getInventory().setContents(hubKit);
        }
    }
}
