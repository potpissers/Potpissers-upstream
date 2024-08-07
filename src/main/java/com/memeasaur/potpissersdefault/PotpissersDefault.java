package com.memeasaur.potpissersdefault;

import com.memeasaur.potpissersdefault.Classes.LoggerData;
import com.memeasaur.potpissersdefault.Classes.LoggerUpdate;
import com.memeasaur.potpissersdefault.Classes.PlayerData;
import com.memeasaur.potpissersdefault.Classes.*;
import com.memeasaur.potpissersdefault.Commands.*;
import com.memeasaur.potpissersdefault.Commands.Claims.ClaimsOpCommands;
import com.memeasaur.potpissersdefault.Commands.Duels.DuelsTabCompleter;
import com.memeasaur.potpissersdefault.Commands.CustomItemOpCommands;
import com.memeasaur.potpissersdefault.Commands.Kits.KitsCommands;
import com.memeasaur.potpissersdefault.Commands.Kits.KitsOpCommands;
import com.memeasaur.potpissersdefault.Commands.Kits.KitsTabCompleter;
import com.memeasaur.potpissersdefault.Commands.Party.PartyCommands;
import com.memeasaur.potpissersdefault.Commands.Party.PartyTabCompleter;
import com.memeasaur.potpissersdefault.Listeners.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Location;
import com.memeasaur.potpissersdefault.Commands.Warps.WarpsCommands;
import com.memeasaur.potpissersdefault.Commands.Warps.WarpsOpCommands;
import com.memeasaur.potpissersdefault.Commands.Warps.WarpsTabExecutor;
import com.memeasaur.potpissersdefault.Listeners.EntityDamageListener;
import com.memeasaur.potpissersdefault.Listeners.PlayerLaunchProjectileListener;
import org.bukkit.entity.*;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import static com.memeasaur.potpissersdefault.Classes.PlayerData.JOIN_MESSAGES;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getFocusComponent;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods1.handleLootChestTask;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Constants.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Postgres.Methods.*;

public final class PotpissersDefault extends JavaPlugin {
    public static final String GAMEMODE_NAME = "hub";
    public static final String GAMEMODE_NAME_SUFFIX = "";
    public static final Server SERVER = Bukkit.getServer();
    public static final BukkitScheduler SCHEDULER = SERVER.getScheduler();
    public static final Messenger MESSENGER = SERVER.getMessenger();

    public static Plugin plugin;
    public static FileConfiguration config;

    public static boolean isExceptionShutdownable = true;

    public static final HikariDataSource SQLITE_POOL;
    static {
        HikariConfig sqliteConfig = new HikariConfig();
        sqliteConfig.setJdbcUrl("jdbc:sqlite:" + DATA_SQLITE);
        SQLITE_POOL = new HikariDataSource(sqliteConfig);
        try (Connection connection = SQLITE_POOL.getConnection(); Statement statement = connection.createStatement()) {
            for (String string : new String(Files.readAllBytes(Paths.get(System.getProperty("user.dir"), "semicolon-separated-sqlite.sql"))).split(";"))
                statement.execute(string);
        } catch (SQLException | IOException e) {
            handlePotpissersExceptions(null, e);
        }
    }
    public static final HikariDataSource POSTGRES_POOL;
    public static final List<Component> POTPISSERS_JOIN_MESSAGES;
    private static Component getJoinMessageComponent(ResultSet resultSet, String endpoint) throws SQLException {
        Component bold = getFocusComponent(resultSet.getString("tip_title")).decorate(TextDecoration.BOLD).decorate(TextDecoration.UNDERLINED).clickEvent(ClickEvent.openUrl("potpissers.com/" + endpoint));
        Component rest = getFocusComponent(":").decoration(TextDecoration.BOLD, false).appendNewline().append(Component.text(resultSet.getString("tip_message")));
        return bold.append(rest);
    }
    public static final int POSTGRESQL_SERVER_ID;
    public static Integer worldBorderRadius;
    public static String defaultKitName;
    public static String defaultAttackSpeedName;
    public static Integer sharpnessLimit;
    public static Integer powerLimit;
    public static Integer protectionLimit;
    static {
        try {
            Class.forName("org.postgresql.Driver"); // I'm not supposed to need this, but I do. probably because paper includes sqlite etc

            HikariConfig postgresConfig = new HikariConfig();
            // TODO -> this can't be good
            postgresConfig.setJdbcUrl(System.getenv("JAVA_POSTGRES_CONNECTION_STRING"));
            POSTGRES_POOL = new HikariDataSource(postgresConfig);

            try (Connection connection = POSTGRES_POOL.getConnection(); Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("SELECT * FROM get_tips()")) {
                    ArrayList<Component> potpissersJoinMessages = new ArrayList<>();
                    System.out.println(resultSet.getMetaData());
                    while (resultSet.next()) {
                        switch (resultSet.getString("game_mode_name")) {
                            case "potpissers" ->
                                    potpissersJoinMessages.add(getJoinMessageComponent(resultSet, ""));
                        }
                    }
                    POTPISSERS_JOIN_MESSAGES = List.copyOf(potpissersJoinMessages);
                }

                try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM upsert_server_return_data(?, ?)")) {
                    preparedStatement.setString(1, GAMEMODE_NAME);
                    preparedStatement.setString(2, GAMEMODE_NAME_SUFFIX);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        resultSet.next();
                        POSTGRESQL_SERVER_ID = resultSet.getInt("server_id");
                        worldBorderRadius = resultSet.getInt("world_border_radius");
                        defaultKitName = resultSet.getString("default_kit_name");
                        defaultAttackSpeedName = resultSet.getString("attack_speed_name");
                        sharpnessLimit = resultSet.getInt("sharpness_limit");
                        powerLimit = resultSet.getInt("power_limit");
                        protectionLimit = resultSet.getInt("protection_limit");
                    }
                }

                catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            handlePotpissersExceptions(null, e);
            throw new RuntimeException(e); // static initializer needs this even though above method does it (?)
        }
    }

    public static boolean isMobDamageImmunityToggled = false;

    public static final Set<CompletableFuture<?>> blockingTasks = Collections.newSetFromMap(new WeakHashMap<>());

    public static final WeakHashMap<Player, PlayerData> playerDataCache = new WeakHashMap<>();
    public static final WeakHashMap<Piglin, LoggerData> loggerDataCache = new WeakHashMap<>();
    public static final WeakHashMap<UUID, Piglin> playerLoggerCache = new WeakHashMap<>();
    public static final WeakHashMap<UUID, LoggerUpdate> loggerUpdateCache = new WeakHashMap<>();

    // Potion start
    public static final Set<ThrownPotion> headSplashPotions = Collections.newSetFromMap(new WeakHashMap<>());
    // Potion end
    public static final Set<Player> loggerUpdateDeadPlayers = Collections.newSetFromMap(new WeakHashMap<>());

    // Claims start

    public static final ConcurrentHashMap<ClaimCoordinate, Object> claims = new ConcurrentHashMap<>();
    public static CompletableFuture<Void> saveClaimsTask = CompletableFuture.completedFuture(null);
    public static CompletableFuture<Void> queueSaveClaimsTaskTask = CompletableFuture.completedFuture(null);

    public static final HashMap<String, CompletableFuture<Void>> serializationTasks = new HashMap<>(Map.ofEntries(
            Map.entry("arenas", CompletableFuture.completedFuture(null)),
            Map.entry("claimsBlocks", CompletableFuture.completedFuture(null)), // TODO impl
            Map.entry("publicWarps", CompletableFuture.completedFuture(null)),
            Map.entry("privateWarps", CompletableFuture.completedFuture(null)),
            Map.entry("locationWarps", CompletableFuture.completedFuture(null)),
            Map.entry("serverWarps", CompletableFuture.completedFuture(null))
    ));

    public static final HashMap<LocationCoordinate, int[]> claimsBlocks = new HashMap<>(); // TODO: serialize this so restarts doing cause damage
    public static final HashMap<String, ArrayList<Location>> arenaWarps = new HashMap<>();
    public static final HashMap<String, Location> publicWarps = new HashMap<>();
    public static final HashMap<String, Location> privateWarps = new HashMap<>();

    public static List<String> immutableDefaultKitNamesCache;
    public static List<String> consumableKitNamesCache;
    public static List<String> arenaNamesCache;

    public static final LinkedHashMap<PlayerData, DuelOptions> playerDuelQueue = new LinkedHashMap<>(); // auto-remove when logout TODO
    public static final LinkedHashMap<AbstractPartyData, DuelOptions> partyDuelQueue = new LinkedHashMap<>();
    public static final WeakHashMap<Entity, DuelTracker> duelEntities = new WeakHashMap<>(); // TODO -> make serialize these and remove them onenable, especially duelitems
    public static final WeakHashMap<ItemStack, DuelTracker> duelDeathItemStacks = new WeakHashMap<>();

    public static final HashMap<LocationCoordinate, String> serverWarps = new HashMap<>();
    public static final HashMap<LocationCoordinate, Location> locationWarps = new HashMap<>();
    // Claims end
    // LootChests start
    public static final HashMap<LocationCoordinate, LootChestData> lootChestsCache = new HashMap<>();
    public static final Set<Entity> lootChestItems = Collections.newSetFromMap(new WeakHashMap<>());
    public static final Set<BlockInventoryHolder> openedLootChests = Collections.newSetFromMap(new WeakHashMap<>());

    public static final Set<CompletableFuture<Void>> currentSupplyDropChestQueries = Collections.newSetFromMap(new WeakHashMap<>()); // TODO -> this doesn't differ between different supply drops, which is fine for now
    // LootChests end
    // Grapple start
    public static final Set<FishHook> grappleHooks = Collections.newSetFromMap(new WeakHashMap<>());
    // Grapple end
    // FightTracker start
    public static final Set<ItemStack> fightTrackerItemStacks = Collections.newSetFromMap(new WeakHashMap<>());
    // FightTracker end
    // SpawnCannon start
    public static final HashMap<UUID, ItemStack> spawnCannonChestplates = new HashMap<>(); // TODO same as claimsBlockMap
    // SpawnCannon end
    // Strength/weakness start
    public static final boolean IS_REVERTED_STRENGTH = false;
    // Strength/weakness end
    // Sharpness/mace/trident start
    public static final boolean IS_NETHERITE_SWORD_NERFED = false;
    // Sharpness/mace/trident end
    // Spawn kit start
    public static ItemStack[] hubKit;
    public static final boolean IS_JOIN_SPAWN_TELEPORT = true;
    // Spawn kit end
    public static final boolean IS_KIT_SERVER = true;
    @Override
    public void onEnable() {
        // Default start
        Bukkit.setWhitelist(true);

        plugin = this;
        config = this.getConfig();

        // Default start
        CompletableFuture.allOf(new CompletableFuture[]{
                        // Claims start
                        fetchBinaryFileObject(DATA_CLAIMS, ConcurrentHashMap.class)
                                .thenAccept(optionalClaims -> optionalClaims
                                .ifPresent(claims::putAll)),
                        fetchBinaryFileObject(DATA_CLAIMS_BLOCKS, HashMap.class) // TODO -> serialize this
                                .thenAccept(optionalClaims -> optionalClaims
                                .ifPresent(claimsBlocks::putAll)),
                        fetchBukkitBinaryFileObject(DATA_PUBLIC_WARPS, HashMap.class)
                                .thenAccept(optionalPublicWarps -> optionalPublicWarps
                                .ifPresent(publicWarps::putAll)),
                        fetchBukkitBinaryFileObject(DATA_PRIVATE_WARPS, HashMap.class)
                                .thenAccept(optionalPrivateWarps -> optionalPrivateWarps
                                .ifPresent(privateWarps::putAll)),
                        fetchBukkitBinaryFileObject(DATA_ARENAS, HashMap.class)
                                .thenAccept(optionalArenas -> optionalArenas.ifPresent(arenaWarps::putAll)),
                        // TODO re-add arenas serialization
                        // Claims end
                        // ServerWarping start
                        fetchBukkitBinaryFileObject(DATA_SERVER_WARPS, HashMap.class)
                                .thenAccept(optionalArenas -> optionalArenas.ifPresent(serverWarps::putAll)),
                        // ServerWarping end
                        // LocationWarping start
                        fetchBukkitBinaryFileObject(DATA_LOCATION_WARPS, HashMap.class)
                                .thenAccept(optionalArenas -> optionalArenas.ifPresent(locationWarps::putAll)),
                        // LocationWarping end
                        // LootChests start
                        fetchDictList(POSTGRES_POOL, "SELECT * FROM get_server_loot_chests(?)", new Object[]{POSTGRESQL_SERVER_ID})
                                .thenAccept(this::handleLootChestsDictList),
                        // LootChests end
                        // Spawn kit start
                        fetchBukkitBinaryFileObject(DATA_SPAWN_KIT, ItemStack[].class)
                                .thenAccept(optionalClaims -> optionalClaims
                                .ifPresent(kit -> hubKit = kit))
                        // Spawn kit end
                })
                .thenRun(() ->
                        fetchPgCallNonnullT("{? = call get_server_is_initially_whitelisted(?)}", Types.BOOLEAN, new Object[]{POSTGRESQL_SERVER_ID}, Boolean.class)
                                .thenAccept(isInitiallyWhitelisted -> {
                                    if (!isInitiallyWhitelisted)
                                        Bukkit.setWhitelist(false);
                                }));
        // Default end

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new EntityDamageByEntityListener(), this);
        pm.registerEvents(new EntityDamageListener(), this);
        pm.registerEvents(new EntityDeathListener(), this);
        pm.registerEvents(new PlayerJoinListener(), this);
        pm.registerEvents(new PlayerMoveListener(), this);
        pm.registerEvents(new PlayerQuitListener(), this);
        pm.registerEvents(new EntityPotionEffectListener(), this);

        PotpissersOpCommands potpissersOpCommands = new PotpissersOpCommands();
        for (String commandName : List.of("heal", "feed", "freeze", "unfreeze", "setdefaultattackspeed", "setworldborder", "setdefaultkitname", "invsee", "togglemobimmunity", "vanish", "unvanish", "repair", "fly", "spectator", "survival", "visit", "godmode", "opmute", "opunmute", "mute", "unmute", "opban", "opunban", "tempban", "untempban", "toggleipexempt", "tpall", "stash", "setsharpnesslimit", "setpowerlimit", "setprotectionlimit", "toggleuserunlockedchatprefix"))
            getCommand(commandName).setExecutor(potpissersOpCommands);
        // Default end

        // Potion start
        pm.registerEvents(new PlayerLaunchProjectileListener(), this);
        pm.registerEvents(new EntityShootBowListener(), this);
        pm.registerEvents(new PotionSplashListener(), this);
        // Potion end

        // Logout start
        PotpissersCommands potpissersCommands = new PotpissersCommands();
        for (String commandName : List.of("logout", "prefix", "killme", "helpop", "hcfrevive", "hcflives", "mzrevive", "mzlives", "referral"))
            getCommand(commandName).setExecutor(potpissersCommands);
        pm.registerEvents(new LoggerListeners(), this);
        pm.registerEvents(new PlayerDeathListener(), this);
        pm.registerEvents(new AreaEffectCloudApplyListener(), this);
        getCommand("getcombattags").setExecutor(potpissersOpCommands);
        // Logout end

        // Claims start
        // PlayerRanks start
        for (String commandName : List.of("addplayertransaction", "setplayerstaffrank", "chatmod"))
            getCommand(commandName).setExecutor(potpissersOpCommands);
        // PlayerRanks end
        ClaimsOpCommands claimsOpCommands = new ClaimsOpCommands();
        for (String commandName : List.of("claimspawn", "unclaimarea", "unclaimall", "claimarena", "unclaimarena", "claimset1", "claimset2", "addarenawarp"))
            getCommand(commandName).setExecutor(claimsOpCommands);

        KitsCommands kitsCommands = new KitsCommands();
        for (String commandName : List.of("save", "removeall"))
            getCommand(commandName).setExecutor(kitsCommands);
        KitsTabCompleter kitsTabExecutor = new KitsTabCompleter();
        for (String commandName : List.of("load", "remove", "loaddefault", "kit")) {
            getCommand(commandName).setExecutor(kitsCommands);
            getCommand(commandName).setTabCompleter(kitsTabExecutor);
        }
        KitsOpCommands kitsOpCommands = new KitsOpCommands();
        for (String commandName : List.of("savedefault", "removedefault", "adddefaultpersonalkit", "cubecorechest"))
            getCommand(commandName).setExecutor(kitsOpCommands);

        PartyCommands partyCommands = new PartyCommands();
        PartyTabCompleter partyTabCompleter = new PartyTabCompleter();
        for (String commandName : List.of("party", "chat", "rally", "caution", "focus", "unfocus", "dequeue")) {
            getCommand(commandName).setExecutor(partyCommands);
            getCommand(commandName).setTabCompleter(partyTabCompleter);
        }

        DuelsTabCompleter duelsTabCompleter = new DuelsTabCompleter();
        for (String commandName : List.of("duel", "anon")) {
            getCommand(commandName).setExecutor(partyCommands);
            getCommand(commandName).setTabCompleter(duelsTabCompleter);
        }

        WarpsCommands warpsCommands = new WarpsCommands();
        getCommand("spawn").setExecutor(warpsCommands);
        WarpsTabExecutor warpsTabExecutor = new WarpsTabExecutor();
        for (String commandName : List.of("warp", "tpa", "tpahere")) {
            getCommand(commandName).setExecutor(warpsCommands);
            getCommand(commandName).setTabCompleter(warpsTabExecutor);
        }
        WarpsOpCommands warpsOpCommands = new WarpsOpCommands();
        for (String commandName : List.of("addwarp", "removewarp", "addprivatewarp", "removeprivatewarp", "privatewarp"))
            getCommand(commandName).setExecutor(warpsOpCommands);

        pm.registerEvents(new ClaimListeners(), this);
        pm.registerEvents(new InventoryClickListener(), this);
        pm.registerEvents(new BlockPlaceListener(), this);
        pm.registerEvents(new BlockBreakListener(), this);
        pm.registerEvents(new BlockDamageListener(), this);
        pm.registerEvents(new PlayerOpenSignListener(), this);
        pm.registerEvents(new PlayerInteractListener(), this);
        pm.registerEvents(new PlayerAttemptPickupItemListener(), this);
        pm.registerEvents(new AsyncChatListener(), this);
        pm.registerEvents(new CreatureSpawnListener(), this);
        pm.registerEvents(new PlayerRespawnListener(), this);
        pm.registerEvents(new EntityExhaustionListener(), this);

        PotpissersPluginMessageListener potpissersPluginMessageListener = new PotpissersPluginMessageListener();

        MESSENGER.registerOutgoingPluginChannel(this, PLUGIN_PLAYER_MESSAGER);
        MESSENGER.registerIncomingPluginChannel(this, PLUGIN_PLAYER_MESSAGER, potpissersPluginMessageListener);

        MESSENGER.registerOutgoingPluginChannel(this, PLUGIN_PARTY_MESSAGER);
        MESSENGER.registerIncomingPluginChannel(this, PLUGIN_PARTY_MESSAGER, potpissersPluginMessageListener);

        MESSENGER.registerOutgoingPluginChannel(this, PLUGIN_PARTY_UNALLIER);
        MESSENGER.registerIncomingPluginChannel(this, PLUGIN_PARTY_UNALLIER, potpissersPluginMessageListener);

        MESSENGER.registerOutgoingPluginChannel(this, PLUGIN_PARTY_UNENEMIER);
        MESSENGER.registerIncomingPluginChannel(this, PLUGIN_PARTY_UNENEMIER, potpissersPluginMessageListener);

        MESSENGER.registerOutgoingPluginChannel(this, PLUGIN_PARTY_ALLIER);
        MESSENGER.registerIncomingPluginChannel(this, PLUGIN_PARTY_ALLIER, potpissersPluginMessageListener);

        MESSENGER.registerOutgoingPluginChannel(this, PLUGIN_PARTY_ENEMIER);
        MESSENGER.registerIncomingPluginChannel(this, PLUGIN_PARTY_ENEMIER, potpissersPluginMessageListener);

        MESSENGER.registerOutgoingPluginChannel(this, PLUGIN_PARTY_DISBANDER);
        MESSENGER.registerIncomingPluginChannel(this, PLUGIN_PARTY_DISBANDER, potpissersPluginMessageListener);

        MESSENGER.registerOutgoingPluginChannel(this, PLUGIN_PARTY_KICKER);
        MESSENGER.registerIncomingPluginChannel(this, PLUGIN_PARTY_KICKER, potpissersPluginMessageListener);

        executeDefaultKitNamesCacheUpdate();
        executeDefaultKitNamesCacheUpdate();
        executeArenaNamesCacheUpdate();
        // Claims end
        // LootChest start
        pm.registerEvents(new PotpissersListeners(), this);
        for (String commandName : List.of("setlootchest", "unsetlootchest"))
            getCommand(commandName).setExecutor(potpissersOpCommands);
        // LootChest end

        // Grapple start
        pm.registerEvents(new ProjectileHitListener(), this);
        pm.registerEvents(new PlayerFishListener(), this);
        CustomItemOpCommands customItemOpCommands = new CustomItemOpCommands();
        getCommand("getgrapple").setExecutor(customItemOpCommands);
        // Grapple end

        // Server switcher start
        getCommand("getservercompass").setExecutor(customItemOpCommands);
        // Server switcher end

        // Cubecore swords start
        getCommand("getcubecoreswords").setExecutor(customItemOpCommands);
        // Cubecore swords end
        // Grapple repair start
        pm.registerEvents(new PrepareItemCraftListener(), this);
        // Grapple repair end

        // Cubecore sign start
        getCommand("cubecoresign").setExecutor(potpissersOpCommands);
        // Cubecore sign end

        // ServerWarping start
        for (String commandName : List.of("toggleenforcedwhitelist", "addservergateway", "removeservergateway"))
            getCommand(commandName).setExecutor(potpissersOpCommands);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "potpissers:serverswitcher");
        // ServerWarping end
        // LocationWarping start
        for (String commandName : List.of("addlocationgateway", "removelocationgateway"))
            getCommand(commandName).setExecutor(potpissersOpCommands);
        // LocationWarping end
        // SpawnCannon start
        getCommand("spawncannon").setExecutor(potpissersOpCommands);
        // SpawnCannon end
        // Supply drops start
        getCommand("activatesupplydrop").setExecutor(potpissersOpCommands);
        // Supply drops end

        // Deathbanz start
        DeathbanOpCommands deathbanOpCommands = new DeathbanOpCommands();
        for (String commandName : List.of("setglobaldeathbanlength", "oprevive"))
            getCommand(commandName).setExecutor(deathbanOpCommands);
        // Deathbanz end

        // Spawn kit start
        getCommand("setspawnkit").setExecutor(potpissersOpCommands);
        // Spawn kit end
        pm.registerEvents(new PlayerItemConsumeListener(), this);

        new BukkitRunnable() {
            @Override
            public void run() {
                Component tip = JOIN_MESSAGES.get(RANDOM.nextInt(JOIN_MESSAGES.size()));
                for (Map.Entry<Player, PlayerData> entry : playerDataCache.entrySet())
                    if (entry.getValue().unViewedTipsCache.remove(tip))
                        entry.getKey().sendMessage(tip);
            }
        }.runTaskTimer(plugin, 0L, 2400L);
    }

    @Override
    public void onDisable() {
        // Default start
        CompletableFuture.allOf(blockingTasks.toArray(new CompletableFuture[0]))
                .join();
        // Default end
    }

    void handleLootChestsDictList(ArrayList<HashMap<String, Object>> dictList) {
        try {
            for (HashMap<String, Object> dict : dictList) {
                LocationCoordinate locationCoordinate = new LocationCoordinate((String) dict.get("world_name"), (Integer) dict.get("x"), (Integer) dict.get("y"), (Integer) dict.get("z"));
                LootChestData lootChestData = new LootChestData(LootTableType.valueOf((String) dict.get("loot_table_name")), (Integer) dict.get("min_amount"), (Integer) dict.get("loot_variance"), (Integer) dict.get("restock_time"), dict.get("direction") instanceof String string ? BlockFace.valueOf(string) : null, Material.valueOf((String) dict.get("block_type")));
                lootChestsCache.put(locationCoordinate, lootChestData);

                Location location = locationCoordinate.toLocation();
                Block block = location.getBlock();
                if (!block.getType().equals(lootChestData.blockMaterial()))
                    handleLootChestTask(location, block, lootChestData);
            }
        }
        catch (Exception e) {
            handlePotpissersExceptions(null, e);
        }
    }
}
