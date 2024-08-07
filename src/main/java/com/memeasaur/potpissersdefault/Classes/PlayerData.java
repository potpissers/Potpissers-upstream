package com.memeasaur.potpissersdefault.Classes;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Types;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.memeasaur.potpissersdefault.Classes.AbstractPartyData.executeNetworkPartyMessage;
import static com.memeasaur.potpissersdefault.Classes.NetworkPartyData.fetchExistingNetworkParty;
import static com.memeasaur.potpissersdefault.Classes.ScoreboardData.SCOREBOARD_STRING;
import static com.memeasaur.potpissersdefault.Commands.PotpissersCommands.handleLogoutTeleport;
import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.SPAWN_CLAIM;
import static com.memeasaur.potpissersdefault.Util.Combat.Methods.handleCombatTag;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants.LogoutTeleport.STRING_TPA;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Methods.getClaim;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getDangerComponent;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Timer.handleTimerCancel;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods1.executeNetworkPlayerMessage;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.handlePotpissersExceptions;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.fetchBukkitObject;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Postgres.Methods.fetchChatRankName;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Postgres.Methods.fetchNullableChatPrefix;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Sqlite.Constants.*;

public class PlayerData extends AbstractData {
    public final Integer sqliteId; // TODO -> refactor ALL_CAPS
    public boolean frozen;

    public static final List<Component> JOIN_MESSAGES = POTPISSERS_JOIN_MESSAGES;
    public transient final HashSet<Component> unViewedTipsCache = new HashSet<>(JOIN_MESSAGES);

    // Tag + logout start
    public int combatTag;

    public int logoutTeleportTimer;
    public Location logoutTeleportLocation;
    public String logoutTeleportString;

    public transient BukkitTask logoutTeleportTask;
    // Tag + logout end

    // Duels/claims start
    public final NetworkPartyData[] mutableNetworkParty = new NetworkPartyData[]{null};

    {
        fetchExistingNetworkParty(uuid)
                .thenAccept(optionalParty -> optionalParty
                        .ifPresent(party -> {
                            mutableNetworkParty[0] = party;
                            executeNetworkPartyMessage(uuid, fetchName(), getNormalComponent(Bukkit.getOfflinePlayer(uuid).getName() + " has logged in to " + GAMEMODE_NAME));
                        }));
    }

    public transient Object currentClaim;
    public final transient Location claimPos1;
    public final transient Location claimPos2;

    private transient final Set<Player> outgoingTpaRequests = Collections.newSetFromMap(new WeakHashMap<>());

    public void handleRequestTpa(Player p, Player pArg, PlayerData dataArg) {
        if (!dataArg.outgoingTpaHereRequests.contains(p)) {
            outgoingTpaRequests.add(pArg);
            p.sendMessage(getNormalComponent(pArg.getName() + " tpa request sent, don't move"));
            pArg.sendMessage(getFocusComponent("tpa requested by " + p.getName() + ". /tpahere (" + p.getName() + ") to accept"));
        } else {
            handleLogoutTeleport(p, this, STRING_TPA, pArg.getLocation());
            dataArg.activeTpaHereTasks.put(this, p);
            pArg.sendMessage(getFocusComponent(p.getName() + " tpa here started, don't move"));
        }
    }

    private transient final Set<Player> outgoingTpaHereRequests = Collections.newSetFromMap(new WeakHashMap<>());

    public void handleRequestTpaHere(Player p, Player pArg, PlayerData dataArg) {
        if (!dataArg.outgoingTpaRequests.contains(p)) {
            outgoingTpaHereRequests.add(pArg);
            p.sendMessage(getNormalComponent(pArg.getName() + " tpa here request sent, don't move"));
            pArg.sendMessage(getFocusComponent("tpa here requested by " + p.getName() + ". /tpa (" + p.getName() + ") to accept"));
        } else {
            handleLogoutTeleport(pArg, dataArg, STRING_TPA, p.getLocation());
            dataArg.activeTpaHereTasks.put(this, p);
            p.sendMessage(getFocusComponent(p.getName() + " tpa here started, don't move"));
        }
    }

    private transient final WeakHashMap<PlayerData, Player> activeTpaHereTasks = new WeakHashMap<>();

    public void handleTpaCancel(LivingEntity playerEntity) {
        for (Player pArg : outgoingTpaRequests) {
            pArg.sendMessage(getDangerComponent("tp request from " + Bukkit.getOfflinePlayer(this.uuid).getName() + " was cancelled"));
            playerEntity.sendMessage(getDangerComponent("tp request cancelled"));
        }
        outgoingTpaRequests.clear();

        for (Map.Entry<PlayerData, Player> entry : activeTpaHereTasks.entrySet()) {
            handleTimerCancel(entry.getKey(), entry.getValue());
            playerEntity.sendMessage(getDangerComponent("tp cancelled by " + playerEntity.getName()));
        }
        activeTpaHereTasks.clear();
    }
    public Set<Location> spawnGlassLocations = Collections.newSetFromMap(new WeakHashMap<>());

    // Duels/claims end
    // Movement cd start
    public final ScoreboardTimer movementTimer;
    // Movement cd end
    // FightTracker start
    public PlayerFightTracker playerFightTracker;
    // FightTracker end
    // Cubecore swords start
    public final transient Set<String> currentAttackBuffs = Collections.newSetFromMap(new WeakHashMap<>());
    // Cubecore swords end
    // Harming cd start
    public transient int harmingDamageCd;
    // Harming cd end
    // Shulker cd start
    public int shulkerCd;
    public transient BukkitTask shulkerTimerTask;
    public LocationCoordinate shulkerLocation;
    // Shulker cd end
    // Deathbans start
    public transient String hostAddress;
    // Deathbans end
    // Reverted opple start
    public final ScoreboardTimer oppleTimer;
    public final ScoreboardTimer totemTimer;
    // Reverted opple end

    public static CompletableFuture<PlayerData> handlePlayerLogin(UUID uuid, Player p, String playerAddress) {
        CompletableFuture<PlayerData> futurePlayerData = new CompletableFuture<>();

        fetchOptionalDict(SQLITE_POOL, RETURN_PLAYER_DATA_LOGGER_UPDATE, new Object[]{uuid})
                .thenAccept(optionalResultDict -> {
                    try {
                        if (optionalResultDict.orElse(null) instanceof HashMap<String, Object> resultDict) {
                            handleLoggerUpdate(resultDict, p, (Integer) resultDict.get("user_id"));
                            futurePlayerData.complete(new PlayerData(uuid, resultDict, p, playerAddress));
                        } else
                            handleSqliteDataCreation(uuid, futurePlayerData, p, playerAddress);
                    } catch (Exception ex) {
                        handlePotpissersExceptions(null, ex);
                    }
                });

        return futurePlayerData;
    }

    private PlayerData(UUID uuid, HashMap<String, Object> resultDict, Player p, String playerAddress) {
        super(uuid);

        sqliteId = (Integer) resultDict.get("user_id");
        frozen = (int) resultDict.get("frozen") != 0;

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        p.setScoreboard(scoreboard);
        Objective o = scoreboard.registerNewObjective(SCOREBOARD_STRING, Criteria.DUMMY, getConsoleComponent(SCOREBOARD_STRING));
        o.setDisplaySlot(DisplaySlot.SIDEBAR);
        o.numberFormat(NumberFormat.blank());

        for (ScoreboardData scoreboardData : ScoreboardData.values()) {
            Team team = scoreboard.registerNewTeam(scoreboardData.string);
            team.addEntry(scoreboardData.string);
            team.color(scoreboardData.color);
        }
        // Tag + logout start
        this.combatTag = (int) resultDict.get("combat_tag");
        this.logoutTeleportTimer = (int) resultDict.get("logout_teleport_timer");
        // Tag + logout end


        // Claims start
        this.claimPos1 = new Location(p.getWorld(), 0, 64, 0);
        this.claimPos2 = new Location(p.getWorld(), 0, 64, 0);
        this.currentClaim = getClaim(new ClaimCoordinate(p.getLocation()));
        // Claims end
        // Movement cd start
        this.movementTimer = new ScoreboardTimer(ScoreboardData.MOVEMENT, (int) resultDict.get("movement_cd"), 0, this, uuid, sqliteId);
        // Movement cd end
        // Shulker cd start
        this.shulkerCd = (int) resultDict.get("shulker_cd");
        // Shulker cd end
        this.hostAddress = playerAddress;

        SCHEDULER.runTaskLater(plugin, () ->
                p.sendMessage(getFocusComponent("discord: ").append(Component.text("https://discord.gg/Cqnvktf7EF").clickEvent(ClickEvent.openUrl("https://discord.gg/Cqnvktf7EF")).decorate(TextDecoration.UNDERLINED))
                ), RANDOM.nextLong(600, 1800)); // TODO -> make method + make youtube/reddit etc work

        // Logout + tag start

        if (combatTag != 0)
            handleCombatTag(this, combatTag);
        // Logout + tag end
        // Reverted opple start
        this.oppleTimer = new ScoreboardTimer(ScoreboardData.OPPLE, (int) resultDict.get("opple_cd"), 0, this, uuid, sqliteId);
        this.totemTimer = new ScoreboardTimer(ScoreboardData.TOTEM, (int) resultDict.get("totem_cd"), 0, this, uuid, sqliteId);
        // Reverted opple end
        // Nameplates start
        for (Map.Entry<String, NamedTextColor> entry : CHAT_RANK_COLORS.entrySet()) {
            String chatRankName = entry.getKey();
            NamedTextColor chatRankColor = entry.getValue();

            scoreboard.registerNewTeam(chatRankName).color(chatRankColor);

            // Hub nameplates start
            for (ChatPrefix chatPrefix : ChatPrefix.values()) {
                Team team = scoreboard.registerNewTeam(chatPrefix.name() + chatRankName);
                team.color(chatRankColor);
                team.prefix(Component.text("<", NamedTextColor.WHITE).append(chatPrefix.component).append(Component.text("> ", NamedTextColor.WHITE)));
            }
            // Hub nameplates end
        }
        fetchChatRankName(uuid)
                .thenAccept(chatRankName -> {
                    try {

                        // Hub nameplates start
                        fetchNullableChatPrefix(uuid)
                                .thenAccept(nullableChatPrefix -> {
                                    String teamName = nullableChatPrefix != null
                                            ? nullableChatPrefix + chatRankName
                                            : chatRankName;
                                    for (Player playerIteration : Bukkit.getOnlinePlayers()) {
                                        playerIteration.getScoreboard().getTeam(teamName).addPlayer(p);
                                        scoreboard.getTeam(playerIteration.getScoreboard().getEntityTeam(playerIteration).getName()).addPlayer(playerIteration);
                                    }
                                });
                        // Hub nameplates end
                    } catch (Exception ex) {
                        handlePotpissersExceptions(null, ex);
                    }
                });
        // Nameplates end

        fetchQueryVoid(POSTGRES_POOL, "call handle_upsert_online_player(?, ?, ?, ?, ?)", new Object[]{uuid, p.getName(), GAMEMODE_NAME, GAMEMODE_NAME_SUFFIX, null}); // TODO -> faction
    }

    static void handleSqliteDataCreation(UUID uuid, CompletableFuture<PlayerData> futurePlayerData, Player p, String playerAddress) {
        fetchSqliteNonnullT(INSERT_USER_UUID_RETURN_ID, new Object[]{uuid}, "id", Integer.class)
                .thenAccept(sqliteId ->
                        CompletableFuture.allOf(fetchQueryVoid(SQLITE_POOL, INSERT_PLAYER_DATA, new Object[]{sqliteId}), fetchQueryVoid(SQLITE_POOL, INSERT_LOGGER_UPDATE, new Object[]{sqliteId, null, null, null}))
                                .thenRun(() ->
                                        fetchNonnullDict(SQLITE_POOL, RETURN_PLAYER_DATA_LOGGER_UPDATE, new Object[]{uuid})
                                                .thenAccept(resultDict -> {
                                                    try {
                                                        handleLoggerUpdate(resultDict, p, sqliteId);
                                                        futurePlayerData.complete(new PlayerData(uuid, resultDict, p, playerAddress));
                                                    } catch (Exception ex) {
                                                        handlePotpissersExceptions(null, ex);
                                                    }
                                                })));
    }

    private static void handleLoggerUpdate(HashMap<String, Object> resultDict, Player p, Integer sqliteId) {
        if (resultDict.get("health") instanceof Double health) {
            if (health == 0)
                loggerUpdateDeadPlayers.add(p);
            p.setHealth(health);
        }
        if (resultDict.get("bukkit_location") instanceof byte[] bytes) // TODO health != null && bukkit_location == null would be a problem, shouldn't be possible
            fetchBukkitObject(bytes, Location.class)
                    .thenAccept(location -> {
                        p.teleport(location);
                        fetchQueryVoid(SQLITE_POOL, UPDATE_LOGGER_UPDATE_NULL, new Object[]{sqliteId});
                    });
        if (resultDict.get("bukkit_inventory") instanceof byte[] bytes) {
            CompletableFuture<ItemStack[]> futureInventory = fetchBukkitObject(bytes, ItemStack[].class);
            fetchQueryVoid(SQLITE_POOL, UPDATE_LOGGER_UPDATE_NULL_INVENTORY, new Object[]{sqliteId})
                    .thenRun(() -> futureInventory
                            .thenAccept(inventory -> p.getInventory().setContents(inventory)));
        }
    }

    public AbstractPartyData[] getActiveMutableParty() {
        return mutableNetworkParty; // TODO -> faction
    }

    public Stream<? extends AbstractPartyData> getCurrentParties() {
        return Stream.of(mutableNetworkParty[0]).filter(Objects::nonNull);
    }

    public void handleAbstractPartyLeave(UUID partyUUID) {
        if (mutableNetworkParty[0] instanceof AbstractPartyData party && party.uuid.equals(partyUUID))
            mutableNetworkParty[0] = null;
        // TODO -> faction
    }

    public CompletableFuture<Integer> fetchCurrentKillStreak() {
        return fetchPgCallNonnullT("{? = call get_user_kill_streak(?, ?)}", Types.INTEGER, new Object[]{uuid, POSTGRESQL_SERVER_ID}, Integer.class);
    }

    private String getName() {
        return Bukkit.getOfflinePlayer(uuid).getName();
    }

    @Override
    protected void executeNetworkMessage(Component msg) {
        executeNetworkPlayerMessage(this.uuid, msg);
    }

    @Override
    public boolean isInSpawn() {
        return currentClaim.equals(SPAWN_CLAIM);
    }

    @Override
    protected CompletableFuture<String> fetchName() {
        return CompletableFuture.completedFuture(getName());
    }

    @Override
    protected boolean isSubclassQueued() {
        return playerDuelQueue.containsKey(this);
    }

    @Override
    protected void handleSubclassQueueCancel() {
        playerDuelQueue.remove(this);
    }

    @Override
    protected CompletableFuture<Void> fetchHandleDuelMapVoid(ConcurrentHashMap<UUID, String> map) {
        map.put(uuid, getName());
        return CompletableFuture.completedFuture(null);
    }
}
