package com.memeasaur.potpissersdefault.Util.Potpissers;

import com.memeasaur.potpissersdefault.Classes.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.messaging.PluginMessageRecipient;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;

import static com.memeasaur.potpissersdefault.Util.Combat.Methods.getLootTableLoot;
import static com.memeasaur.potpissersdefault.Util.Crypto.CURRENT_DEATHBANS_IP_HMAC_KEY;
import static com.memeasaur.potpissersdefault.Util.Crypto.getHmacBytes;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getDangerComponent;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.fetchOptionalDict;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.fetchQueryVoid;

public class Methods1 {
    private static PluginMessageRecipient getPluginMessageRecipient() {
        for (Player player : Bukkit.getOnlinePlayers())
            return player; // velocity is such a piece of shit for requiring this solution. if you use SERVER here it sends one message per player on the fucking server
        return SERVER;
    }
    public static void sendPotpissersPluginMessage(String s, byte[] bytes) {
        getPluginMessageRecipient().sendPluginMessage(plugin, s, bytes);
    }

    public static CompletableFuture<Optional<HashMap<String, Object>>> fetchFarthestUserDeathBan(UUID uuid, int serverId) {
        return fetchOptionalDict(POSTGRES_POOL, "SELECT * FROM get_farthest_user_death_ban(?, ?)", new Object[]{uuid, serverId});
    }
    public static CompletableFuture<Optional<HashMap<String, Object>>> fetchFarthestUserIpDeathban(UUID uuid, String playerAddress, int serverId) {
        return fetchOptionalDict(POSTGRES_POOL, "SELECT * FROM get_farthest_user_ip_deathban(?, ?, ?)", new Object[]{uuid, getHmacBytes(CURRENT_DEATHBANS_IP_HMAC_KEY, playerAddress), serverId});
    }
    public static void executeDeathbanCheck(UUID uuid, Player p, String playerAddress) {
        fetchFarthestUserDeathBan(uuid, POSTGRESQL_SERVER_ID)
                .thenAccept(optionalDIct -> optionalDIct
                        .ifPresent(dict -> // TODO -> just do this entirely in postgres ?
                                p.kick(getDangerComponent("death-banned " + Duration.between(LocalDateTime.now(), ((Timestamp) dict.get("expiration")).toLocalDateTime()).toSeconds() + "s: " + dict.get("death_message")))));
        fetchFarthestUserIpDeathban(uuid, playerAddress, POSTGRESQL_SERVER_ID)
                .thenAccept(optionalResultDict -> optionalResultDict
                        .ifPresent(dict ->
                                p.kick(getDangerComponent("ip-death-banned " + Duration.between(LocalDateTime.now(), ((Timestamp) dict.get("expiration")).toLocalDateTime()).toSeconds() + "s: " + dict.get("death_message")))));
    }
    public static String getPotpissersCommandReason(String[] commandArgs) {
        return String.join(" ", Arrays.copyOfRange(commandArgs, 1, commandArgs.length));
    }
    public static List<String> getOnlinePlayerNamesList() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }
    public static void executeNetworkPlayerMessage(UUID uuid, Component msg) {
        sendPotpissersPluginMessage(PLUGIN_PLAYER_MESSAGER, new JSONObject(Map.of("playerUUID", uuid.toString(), "msg", JSONComponentSerializer.json().serialize(msg))).toJSONString().getBytes(StandardCharsets.UTF_8));
    }
    // LootChests start
    public static void handleLootChestLoot(BlockInventoryHolder chest, int minAmount, int lootVariance, LootTableType lootTable) {
        Inventory chestInventory = chest.getInventory();
        chestInventory.clear(); // I can't think of ever needing to add loot to a non-empty chest
        int inventorySize = chestInventory.getSize();
        for (ItemStack itemStack : getLootTableLoot(minAmount, lootVariance, lootTable)) {
            // TODO these items can replace each other, which is fine for now
            if (chestInventory.firstEmpty() != -1) {
                chestInventory.setItem(RANDOM.nextInt(inventorySize), itemStack);
            }
            // else dropNaturally
        }
    }

    public static void handleLootChestBreak(Block block, BlockInventoryHolder blockInventoryHolder, Map.Entry<Integer, PlayerData> nullableSupplyDropData) {
        ItemStack[] contents = blockInventoryHolder.getInventory().getContents();
        block.setType(Material.AIR); // TODO breakNaturally is so much nicer

        openedLootChests.remove(blockInventoryHolder);

        World world = block.getWorld();
        Location location = block.getLocation();
        for (ItemStack itemStack : contents) {
            if (itemStack != null) {
                lootChestItems.add(world.dropItemNaturally(location, itemStack));
            }
        }
        if (lootChestsCache.get(new LocationCoordinate(block)) instanceof LootChestData lootChestData)
            handleLootChestTask(location, block, lootChestData);
        else if (nullableSupplyDropData != null) {
            PlayerData data = nullableSupplyDropData.getValue();
            currentSupplyDropChestQueries.add(fetchQueryVoid(POSTGRES_POOL, "CALL upsert_supply_drop_round_data(?, ?, ?)", new Object[]{nullableSupplyDropData.getKey(), data.uuid, data.getActiveMutableParty()[0] instanceof AbstractPartyData abstractPartyData ? abstractPartyData.uuid : null}));
        }
    }

    public static void handleLootChestTask(Location location, Block block, LootChestData lootChestData) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!location.getNearbyPlayers(15).isEmpty()) {
                    handleLootChestTask(location, block, lootChestData);
                    cancel();
                    return;
                }
                else if (Math.random() > .98) {
                    block.breakNaturally();
                    block.setType(lootChestData.blockMaterial());
                    if (lootChestData.enumChestDirection() instanceof BlockFace direction) {
                        Directional directional = (Directional) block.getBlockData();
                        directional.setFacing(direction);
                        block.setBlockData(directional);
                    }
                    handleLootChestLoot((Chest) block.getState(), lootChestData.minAmount(), lootChestData.lootVariance(), lootChestData.lootTableType());
                    cancel();
                    return;
                }
            }
        }.runTaskTimer(plugin, lootChestData.restockTime(), 20L);
    }
    // LootChests end

    // Cubecore swords start
    public static void spawnEntityParticles(World world, Particle particleType, Vector boundingBoxCenter, int particleAmount) {
        world.spawnParticle(particleType, boundingBoxCenter.getX(), boundingBoxCenter.getY(), boundingBoxCenter.getZ(), particleAmount);
    }
    // Cubecore swords end
}
