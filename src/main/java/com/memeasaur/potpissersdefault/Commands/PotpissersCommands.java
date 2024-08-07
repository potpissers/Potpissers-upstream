package com.memeasaur.potpissersdefault.Commands;

import com.memeasaur.potpissersdefault.Classes.ChatPrefix;
import com.memeasaur.potpissersdefault.Classes.ClaimCoordinate;
import com.memeasaur.potpissersdefault.Classes.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.sql.Types;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.PotpissersDefault.plugin;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.SPAWN_CLAIM;
import static com.memeasaur.potpissersdefault.Util.Claim.Methods.getClaim;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants.LogoutTeleport.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Timer.handleTimerCancel;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods1.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Postgres.Methods.fetchInsertUserReferralVoid;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.serializeBukkitBinary;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Postgres.Methods.handleUserReferralDataFetchExists;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Sqlite.Methods.fetchUpdatePlayerDataIntVoid;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Sqlite.Methods.fetchUpdatePlayerDataVoid;

public class PotpissersCommands implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof Player p)
            switch (command.getName().toLowerCase()) {
                case "logout" -> {
                    if (strings.length != 0)
                        p.sendMessage(getConsoleComponent("?"));
                    handleLogoutTeleport(p, playerDataCache.get(p), STRING_LOGOUT, null);
                }
                case "referral" -> {
                    UUID uuid = p.getUniqueId();
                    if (strings.length == 0)
                        p.sendMessage("invalid (args)");
                    else {
                        String ip = p.getAddress().getAddress().getHostAddress();
                        handleUserReferralDataFetchExists(uuid, ip)
                                .thenAccept(exists -> {
                                    if (exists)
                                        p.sendMessage("invalid (no longer eligible)");
                                    else {
                                        String string = String.join(" ", Arrays.copyOfRange(strings, 0, strings.length));
                                        fetchInsertUserReferralVoid(ip, uuid, string)
                                                .thenRun(() -> p.sendMessage("referral set: " + string));
                                    }
                                });
                    }
                }
                case "prefix" -> {
                    if (strings.length == 0)
                        fetchPgCallNonnullT("{? = CALL get_user_unlocked_chat_prefixes(?)}", Types.ARRAY, new Object[]{p.getUniqueId()}, Integer[].class)
                                .thenAccept(ids -> {
                                    StringBuilder stringBuilder = new StringBuilder("unlocked prefixes: ");
                                    Set<Integer> idsSet = Set.of(ids);
                                    boolean flag = false;
                                    for (ChatPrefix chatPrefix : ChatPrefix.values())
                                        if (idsSet.contains(chatPrefix.postgresId)) {
                                            if (flag)
                                                stringBuilder.append(", ");
                                            stringBuilder.append(chatPrefix.name());
                                            flag = true;
                                        }
                                    p.sendMessage(stringBuilder.toString());
                                });
                    else {
                        if (strings.length > 1)
                            p.sendMessage("?");
                        if (Arrays.stream(ChatPrefix.values()).noneMatch(chatPrefix -> chatPrefix.name().equals(strings[0])))
                            p.sendMessage("invalid: chatPrefix");
                        else
                            fetchPgCallOptionalT("{? = CALL toggle_chat_prefix_returning_is_null_if_successful(?, ?)}", Types.BOOLEAN, new Object[]{p.getUniqueId(), ChatPrefix.valueOf(strings[0]).postgresId}, Boolean.class)
                                    .thenAccept(optional -> {
                                        if (optional.isEmpty())
                                            p.sendMessage("invalid (locked)");
                                        else
                                            p.sendMessage("prefix set: " + (!optional.get() ? strings[0] : null));
                                    });
                    }
                }
                case "killme" -> {
                    if (!IS_KIT_SERVER)
                        p.sendMessage("invalid (disabled)");
                    else
                        p.damage(Double.MAX_VALUE);
                }
                case "helpop" -> {
                    if (strings.length == 0)
                        p.sendMessage("usage: /helpop (...)");
                    else { // TODO -> network-wide
                        String reason = getPotpissersCommandReason(strings);
                        for (Player player : Bukkit.getOnlinePlayers())
                            if (player.hasPermission("potpissers.helper"))
                                player.sendMessage(p.getName() + ": " + reason);
                    }
                }
                case "hcfrevive" -> {
                    handleAbstractReviveCommand(strings, p, "hcf");
                    return true;
                }
                case "hcflives" -> {
                    handleAbstractLivesCommand(strings, p, "hcf");
                    return true;
                }
                case "mzrevive" -> {
                    handleAbstractReviveCommand(strings, p, "mz");
                    return true;
                }
                case "mzlives" -> {
                    handleAbstractLivesCommand(strings, p, "mz");
                    return true;
                }
            }
        return false;
    }

    public static void handleLogoutTeleport(Player p, PlayerData data, String logoutTeleportString, Location logoutTeleportLocation) {
        final boolean flag;
        if (data.logoutTeleportTask instanceof BukkitTask task && !task.isCancelled()) {
            if (Objects.equals(data.logoutTeleportLocation, logoutTeleportLocation)) {
                p.sendMessage(Component.text("invalid (active)"));
                return;
            } else if (data.logoutTeleportLocation == null)
                p.sendMessage(Component.text("logout swapped to teleport"));
            else if (logoutTeleportLocation == null)
                p.sendMessage(Component.text("teleport swapped to logout"));
            else
                p.sendMessage(Component.text("teleport location swapped"));
            flag = true;
        }
        else
            flag = false;
        data.logoutTeleportString = logoutTeleportString;
        data.logoutTeleportLocation = logoutTeleportLocation;
        (data.logoutTeleportLocation != null ? serializeBukkitBinary(data.logoutTeleportLocation.clone()) : CompletableFuture.completedFuture(null))
                .thenAccept(locationBytes ->
                        fetchUpdatePlayerDataVoid(new String[]{"logout_teleport_location", "logout_teleport_string"}, new Object[]{locationBytes, data.logoutTeleportString, data.sqliteId}));

        if (!flag) {
            p.sendActionBar(Component.text(data.logoutTeleportString + (getLogoutTeleportTimer(data) - data.logoutTeleportTimer)));
            data.logoutTeleportTask = new BukkitRunnable() {
                CompletableFuture<Void> saveLogoutTimerQuery = fetchUpdatePlayerDataIntVoid("logout_teleport_timer", data.logoutTeleportTimer, data.sqliteId);
                final UUID uuid = data.uuid;
                @Override
                public void run() {
                    LivingEntity playerEntity = Bukkit.getPlayer(uuid) instanceof Player player ? player : playerLoggerCache.get(uuid);
                    data.logoutTeleportTimer++;
                    int currentLogoutTeleportTimer = getLogoutTeleportTimer(data);
                    if (data.logoutTeleportTimer >= currentLogoutTeleportTimer) {
                        if (!(data.logoutTeleportLocation instanceof Location location)) {
                            if (playerEntity instanceof Player p) {
                                p.kick(getNormalComponent("logout successful"));
                                handleTimerCancel(data, playerEntity);
                            }
                        } else {
                            if (PVP_WARPS.contains(data.logoutTeleportString)) {
                                location.setPitch(playerEntity.getPitch());
                                location.setYaw(playerEntity.getYaw());
                                location.setDirection(playerEntity.getLocation().getDirection());
                            }
                            if (playerEntity != null)
                                playerEntity.teleport(location);
//                            else
                                // TODO -> make the logger teleport before it gets away

                            // Claims start
                            if (getClaim(new ClaimCoordinate(location)).equals(SPAWN_CLAIM))
                                data.combatTag = 0;
                            // Claims end
                            handleTimerCancel(data, playerEntity);
                        }
                        playerEntity.sendActionBar(Component.text(""));
                        saveLogoutTimerQuery
                                .thenRun(() -> fetchUpdatePlayerDataIntVoid("logout_teleport_timer", data.logoutTeleportTimer, data.sqliteId));
                        cancel();
                    } else {
                        playerEntity.sendActionBar(Component.text(data.logoutTeleportString + (currentLogoutTeleportTimer - data.logoutTeleportTimer)));
                        if (saveLogoutTimerQuery.isDone())
                            saveLogoutTimerQuery = fetchUpdatePlayerDataIntVoid("logout_teleport_timer", data.logoutTeleportTimer, data.sqliteId);
                    }
                }
            }.runTaskTimer(plugin, 20L, 20L);
        }
    }

    public static int getLogoutTeleportTimer(PlayerData data) {
        if (data.currentClaim.equals(SPAWN_CLAIM))
            return 0;
        else if (data.combatTag == 0)
            return SAFE_LOGOUT_TIMER;
        else
            return TAG_LOGOUT_TIMER;
    }
    public static int getShulkerTimer(PlayerData data) {
        if (data.currentClaim.equals(SPAWN_CLAIM))
            return 0;
        else if (data.combatTag == 0)
            return TAG_LOGOUT_TIMER;
        else
            return LONG_LOGOUT_TIMER;
    }
    private void handleAbstractRevive(String gameModeName, Player p, UUID revivedOfflinePlayerUuid, int serverId, String commandName, String revivedPlayerName) {
        fetchPgCallOptionalT("{? = CALL handle_insert_revive_return_result_in_cents_if_successful(?, ?, ?, ?, ?)}", Types.INTEGER, new Object[]{gameModeName, p.getUniqueId(), revivedOfflinePlayerUuid, serverId, commandName}, Integer.class)
                .thenAccept(optionalNewLivesInCents -> {
                    if (optionalNewLivesInCents.isEmpty())
                        p.sendMessage("invalid (lives)");
                    else {
                        p.sendMessage(revivedPlayerName + " revived. remaining " + gameModeName + " lives: " + (float) optionalNewLivesInCents.get() / 100);
                        executeNetworkPlayerMessage(revivedOfflinePlayerUuid, getFocusComponent("you've been " + commandName + "'d by " + p.getName())); // TODO -> anon option
                    }
                });
    }
    private void handleAbstractReviveCommand(String[] strings, Player p, String gameModeName) {
        if (strings.length < 1)
            p.sendMessage(Component.text("invalid (args)"));
        else {
            if (strings.length > 1)
                p.sendMessage(Component.text("?"));
            OfflinePlayer revivedOfflinePlayer = Bukkit.getOfflinePlayer(strings[0]);
            fetchPgCallOptionalT("{? = CALL get_nullable_game_mode_name_current_server_id(?)}", Types.INTEGER, new Object[]{gameModeName}, Integer.class)
                    .thenAccept(optionalHcfServerId -> {
                        if (optionalHcfServerId.isEmpty())
                            p.sendMessage("invalid (active " + gameModeName + " server)");
                        else {
                            int currentHcfServerId = optionalHcfServerId.get();
                            UUID revivedOfflinePlayerUuid = revivedOfflinePlayer.getUniqueId();
                            fetchFarthestUserDeathBan(revivedOfflinePlayerUuid, currentHcfServerId)
                                    .thenAccept(optionalUuidDeathBanDict -> {
                                        if (optionalUuidDeathBanDict.isEmpty()) {
                                            if (!(revivedOfflinePlayer.getPlayer() instanceof Player revivedPlayer)) {
                                                // TODO -> check network for an ip for this player
                                                p.sendMessage("invalid (online player), give them the life to use themselves - or join their server and revive them, until I handle this better");
                                            }
                                            else
                                                fetchFarthestUserIpDeathban(revivedOfflinePlayerUuid, revivedPlayer.getAddress().getAddress().getHostAddress(), currentHcfServerId)
                                                        .thenAccept(optionalIpDeathBanDict -> {
                                                            if (optionalIpDeathBanDict.isEmpty())
                                                                p.sendMessage("invalid (deathban)");
                                                            else
                                                                handleAbstractRevive(gameModeName, p, revivedOfflinePlayerUuid, currentHcfServerId, "/" + gameModeName + "revive", revivedOfflinePlayer.getName());
                                                        });
                                        }
                                        else
                                            handleAbstractRevive(gameModeName, p, revivedOfflinePlayerUuid, currentHcfServerId, "/" + gameModeName + "revive", revivedOfflinePlayer.getName());
                                    });
                        }
                    });
        }
    }
    private void handleAbstractLivesCommand(String[] strings, Player p, String gameModeName) {
        if (strings.length != 0)
            p.sendMessage(getConsoleComponent("?"));
        fetchPgCallNonnullT("{? = CALL get_user_game_mode_lives_as_cents(?, ?)}", Types.INTEGER, new Object[]{p.getUniqueId(), gameModeName}, Integer.class)
                .thenAccept(livesAsCents ->
                        p.sendMessage(gameModeName + " lives (" + p.getName() + "): " + (float) livesAsCents / 100));
    }
}
