package com.memeasaur.potpissersdefault.Util.Potpissers;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import java.util.*;

import com.google.gson.Gson;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.json.simple.parser.JSONParser;

public class Constants1 {
    public static final NamespacedKey KILL_LORE_COUNT_KEY = new NamespacedKey("killlorecount", "killlorecount");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss");
    public static final Gson GSON = new Gson();
    public static final JSONParser JSON_PARSER = new JSONParser();
    public static final String CHAT_RANK_DEFAULT = "default";
    public static final String CHAT_RANK_ADMIN = "admin";
    public static final String CHAT_RANK_MOD = "mod";
    public static final String CHAT_RANK_WATCHER = "watcher";
    public static final Map<String, NamedTextColor> CHAT_RANK_COLORS = Map.of(
            CHAT_RANK_ADMIN, NamedTextColor.DARK_RED,
            CHAT_RANK_MOD, NamedTextColor.DARK_AQUA,
            CHAT_RANK_WATCHER, NamedTextColor.DARK_GREEN,

            "unverified", NamedTextColor.GRAY,
            CHAT_RANK_DEFAULT, NamedTextColor.WHITE,
            "basic", NamedTextColor.GREEN,
            "gold", NamedTextColor.YELLOW,
            "diamond", NamedTextColor.AQUA,
            "ruby", NamedTextColor.RED,
            "big dog", NamedTextColor.LIGHT_PURPLE);

    public static final String PARTY_RANK_MEMBER = "member";
    public static final String PARTY_RANK_OFFICER = "officer";
    public static final String PARTY_RANK_LEADER = "leader";
    public static final Map<String, Integer> PARTY_RANK_LEVELS = Map.of(
            PARTY_RANK_MEMBER, 0,
            PARTY_RANK_OFFICER, 1,
            "co-leader", 2,
            PARTY_RANK_LEADER, 3);

    private static final String VANILLA = "vanilla";
    public static final String REVERTED_VANILLA = "reverted vanilla";
    public static final Set<String> ATTACK_SPEED_VANILLA_NAMES = Set.of(VANILLA, REVERTED_VANILLA);
    public static final Map<String, Double> ATTACK_SPEED_VALUES = Map.of(
            VANILLA, 4D,
            REVERTED_VANILLA, 1.5999999046325684,
            "7cps", 6.25,
            "12cps", 9.25,
            "uncapped", Double.MAX_VALUE);
    public static final Set<Double> ATTACK_SPEED_VANILLA_VALUES = Set.of(ATTACK_SPEED_VALUES.get(VANILLA), ATTACK_SPEED_VALUES.get(REVERTED_VANILLA));

    public static final String CHAT_SERVER = "server";
    public static final String CHAT_LOCAL = "local";
    public static final String CHAT_PARTY = "party";
    public static final String CHAT_FACTION = "faction";
    public static final String CHAT_ALLY = "ally";

    // Claims start
    public static final String PLUGIN_PLAYER_MESSAGER = "potpissers:messager";
    public static final String PLUGIN_PARTY_MESSAGER = "potpissers:partymessager";
    public static final String PLUGIN_PARTY_UNALLIER = "potpissers:partyunallier";
    public static final String PLUGIN_PARTY_UNENEMIER = "potpissers:partyunenemier";
    public static final String PLUGIN_PARTY_ALLIER = "potpissers:partyallier";
    public static final String PLUGIN_PARTY_ENEMIER = "potpissers:partyenemier";
    public static final String PLUGIN_PARTY_DISBANDER = "potpissers:partydisbander";
    public static final String PLUGIN_PARTY_KICKER = "potpissers:partykicker";
    // Claims end

    // Cubecore sign start
    public static final NamespacedKey KEY_CUBECORE_SIGN = new NamespacedKey("cubecoresign", "cubecoresign");
    // Cubecore sign end

    // LootChests start
    public static final Random RANDOM = new Random(); // TODO -> im sure i'm using this in a thread somewhere like a fucking idiot
    // LootChests end
    // ServerWarping start
    public static final NamespacedKey KEY_POTPISSERS_GATEWAY = new NamespacedKey("potpissersgateway", "potpissersgateway");
    // ServerWarping end

    // FightTracker start
    public enum AnticleanLevel {
        OFF(0),
        ALLIES(1),
        ALLIES_STRICT(2),
        ON(3),
        ON_STRICT(4); // TODO - impl

        public final int level;

        AnticleanLevel(int level) {
            this.level = level;
        }
    }
    // FightTracker end

    // UnclickableItems start
    public static final NamespacedKey KEY_UNCLICKABLE_ITEM = new NamespacedKey("unclickableitem", "unclickableitem");
    // UnclickableItems end

    // SpawnCannon start
    public static final PotionEffect SPAWN_CANNON_LEVITATION = new PotionEffect(PotionEffectType.LEVITATION, 10, 255);
    public static final PotionEffect SPAWN_CANNON_GLOWING = new PotionEffect(PotionEffectType.GLOWING, 10, 0);
    public static final List<PotionEffect> SPAWN_CANNON_PHASE_ONE_EFFECTS = List.of(SPAWN_CANNON_GLOWING, SPAWN_CANNON_LEVITATION);
    // SpawnCannon end

    // Combat tag start
    public static final int COMBAT_TAG = 120; // TODO make command
    // Combat tag end

    // Grapple start
    public static final int GRAPPLE_CD = 12;
    public static final int KNOCKBACK_CD = 4;
    // Grapple end

    // Movement cd start
    public static PotionEffect getCombatGlow(int duration) {
        return new PotionEffect(PotionEffectType.GLOWING, duration, 0);
    }
    // Movement cd end

    // Cubecore swords start
    public static final int PHANTOM_BLADE_CD = Math.round(GRAPPLE_CD * .75F);
    // Cubecore swords end

    // Reverted opple start
    public static final int OPPLE_CD = 0; public static final int TICK_OPPLE_CD = OPPLE_CD * 20;
    public static final int TOTEM_CD = 0; public static final int TICK_TOTEM_CD = TOTEM_CD * 20;
    public static final int CONSUMABLE_BUFFER = 4;
    public static final NamespacedKey KEY_OPPLE_TOTEM = new NamespacedKey("oppletotem", "oppletotem");
    // Reverted opple end
    public static final ZoneId EST = ZoneId.of("America/New_York");
}
