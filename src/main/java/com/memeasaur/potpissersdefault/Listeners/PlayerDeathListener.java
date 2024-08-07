package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.attribute.Attribute;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static com.memeasaur.potpissersdefault.Classes.LootTableType.KILL_STREAK_REWARDS;
import static com.memeasaur.potpissersdefault.Classes.LootTableType.SUPPLY_DROP;
import static com.memeasaur.potpissersdefault.PotpissersDefault.playerDataCache;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.PotpissersDefault.fightTrackerItemStacks;
import static com.memeasaur.potpissersdefault.Util.Combat.Methods.*;
import static com.memeasaur.potpissersdefault.Util.Crypto.CURRENT_DEATHBANS_IP_HMAC_KEY;
import static com.memeasaur.potpissersdefault.Util.Crypto.getHmacBytes;
import static com.memeasaur.potpissersdefault.Util.CubecoreSwords.Constants.CUBECORE_SWORD_DAMAGE_TYPES;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.serializeBukkitBinary;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.*;

public class PlayerDeathListener implements Listener {
    @EventHandler
    void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        p.getAttribute(Attribute.ARMOR).setBaseValue(0);
        if (!loggerUpdateDeadPlayers.contains(p)) {
            handlePlayerDeath(e, playerDataCache.get(p), null, p.getInventory().getContents());
            loggerUpdateDeadPlayers.remove(p);
        }
    }
    public static void handlePlayerDeath(EntityDeathEvent e, PlayerData data, Component nullablePiglinLoggerUnsentDeathMessage, ItemStack[] playerInventory) {
        LivingEntity playerEntity = e.getEntity();
        data.combatTag = 0;

        // Lightning start
        playerEntity.getWorld().strikeLightningEffect(playerEntity.getLocation());
        // Lightning end
        // Dueling start
        if (data.playerFightTracker instanceof PlayerFightTracker playerFightTracker && playerFightTracker.statTracker instanceof DuelTracker duelTracker && duelTracker.alivePlayers.containsKey(data.uuid)) {

            for (ItemStack is : e.getDrops())
                duelDeathItemStacks.put(is, duelTracker);

            if (playerEntity instanceof Player player) {
                player.setGameMode(GameMode.SPECTATOR); // TODO -> make this survival w/ flying w/ inventories
                player.setRespawnLocation(player.getLocation());
            }

            data.playerFightTracker = null;
            duelTracker.alivePlayers.remove(data.uuid);
            HashSet<String> aliveTeams = new HashSet<>(duelTracker.alivePlayers.values());

            if (aliveTeams.size() <= 1) {
                SCHEDULER.runTaskLater(plugin, () -> {
                    Location spawn = Bukkit.getWorld("world").getSpawnLocation();

                    Component msg = Component.text("winner: ").decorate(TextDecoration.ITALIC);
                    for (String teamName : aliveTeams)
                        msg = msg.append(Component.text(teamName));

                    msg = msg.appendNewline().append(Component.text("loser: "));
                    for (String teamName : Set.copyOf(duelTracker.players.values()))
                        if (!aliveTeams.contains(teamName))
                            msg = msg.append(Component.text(teamName));

                    Component loserMsg = msg.color(NamedTextColor.RED);
                    Component winnerMsg = msg.color(NamedTextColor.GREEN);
                    for (PlayerFightTracker playerFightTrackerIteration : duelTracker.fightData)
                        if (Bukkit.getPlayer(playerFightTrackerIteration.uuid) instanceof Player player) {
                            player.setVisibleByDefault(true);
                            player.setGameMode(GameMode.SURVIVAL);
                            player.setRespawnLocation(Bukkit.getWorld("world").getSpawnLocation());
                            if (player.getHealth() > 0)
                                player.teleport(spawn);
                            playerDataCache.get(player).combatTag = 0;
                            if (aliveTeams.contains(duelTracker.players.get(playerFightTrackerIteration.uuid)))
                                player.sendMessage(winnerMsg);
                            else
                                player.sendMessage(loserMsg);

                            for (Map.Entry<Entity, DuelTracker> entry : duelEntities.entrySet())
                                if (entry.getValue() == duelTracker)
                                    entry.getKey().remove();
                        }
                }, 60L);
            }
        }

        else {
//            data.killStreakCache = 0;

            // FightTracker start
            CompletableFuture<Integer> futureUserFightId; // TODO -> do this for duelTracker too with same code
            if (data.playerFightTracker instanceof PlayerFightTracker playerFightTracker && playerFightTracker.statTracker instanceof FightTracker fightTracker) {
                fightTrackerItemStacks.addAll(e.getDrops());
                data.playerFightTracker.fightTrackerTimer = 0;
                futureUserFightId = data.playerFightTracker.handleFightTrackerExpiration(data, playerEntity instanceof Player player ? player : null, fightTracker, data.getActiveMutableParty()[0]);
            } else
                futureUserFightId = CompletableFuture.completedFuture(null);
            // FightTracker end

            // KillLore start
            final ItemStack killWeapon;
            final DamageSource damageSource = e.getDamageSource();
            final DamageType damageType = damageSource.getDamageType();

            // removed lored item component from death message because it hits the string limit and crashes everyone who sees the message
            // TODO -> add that message to the item, then generate that new item's component for the chat message
            if (damageSource.getCausingEntity() instanceof LivingEntity livingEntity) {
                if (CUBECORE_SWORD_DAMAGE_TYPES.contains(damageType) && livingEntity.getEquipment() instanceof EntityEquipment entityEquipment) {
                    killWeapon = entityEquipment.getItemInMainHand();
                    Component component = Component.text(" using ").append(killWeapon.displayName());
                    if (e instanceof PlayerDeathEvent playerDeathEvent && playerDeathEvent.deathMessage() instanceof Component deathMessage)
                        if (killWeapon.getItemMeta() instanceof ItemMeta itemMeta && itemMeta.hasCustomName())
                            playerDeathEvent.deathMessage(deathMessage.append(component)); // default behaviour does this with custom names WTF!
                    else if (nullablePiglinLoggerUnsentDeathMessage != null)
                        nullablePiglinLoggerUnsentDeathMessage = nullablePiglinLoggerUnsentDeathMessage.append(component);
                } else if (damageSource.getDirectEntity() instanceof AbstractArrow abstractArrow && abstractArrow.getWeapon() instanceof ItemStack weapon) {
                    killWeapon = weapon;
                    Component component = Component.text(" using ").append(killWeapon.displayName());
                    Component component2 = Component.text(" from a distance of " + (int) playerEntity.getLocation().distance(livingEntity.getLocation()));
                    if (e instanceof PlayerDeathEvent playerDeathEvent && playerDeathEvent.deathMessage() instanceof Component deathMessage) {
                        if (killWeapon.getItemMeta() instanceof ItemMeta itemMeta && itemMeta.hasCustomName())
                            playerDeathEvent.deathMessage(deathMessage.append(component).append(component2));
                        else
                            playerDeathEvent.deathMessage(deathMessage.append(component2));
                    } // TODO -> method-ize
                    else if (nullablePiglinLoggerUnsentDeathMessage != null)
                        nullablePiglinLoggerUnsentDeathMessage = nullablePiglinLoggerUnsentDeathMessage.append(component).append(component2);
                }
                else
                    killWeapon = null;
            }
            else
                killWeapon = null; // TODO -> fuck doing this rn, but make it so that the death message kill weapon shows the death message's kill

            final String plainDeathMessage = PlainTextComponentSerializer.plainText().serialize(e instanceof PlayerDeathEvent playerDeathEvent && playerDeathEvent.deathMessage() instanceof Component deathMessage ? deathMessage : nullablePiglinLoggerUnsentDeathMessage != null ? nullablePiglinLoggerUnsentDeathMessage : Component.text("null")); // TODO do this in the above cluster-fuck
            final Component plainDeathMessageLore = Component.text(OffsetDateTime.now().format(DATE_TIME_FORMATTER) + " " + plainDeathMessage);
            if (playerEntity.getEquipment() instanceof EntityEquipment entityEquipment)
                for (ItemStack is : entityEquipment.getArmorContents())
                    handleKillLoreItem(is, "deaths", plainDeathMessageLore);
            handleKillLoreItem(killWeapon, "kills", plainDeathMessageLore);
            // KillLore end

            // KillTracker start
            Location deathLocation = playerEntity.getLocation();

            Player killer = playerEntity.getKiller();

            UUID killerUuid = killer != null ? killer.getUniqueId() : null;
            CompletableFuture<byte[]> futureKillerInventory = killer != null ? serializeBukkitBinary(killer.getInventory().getContents().clone()) : CompletableFuture.completedFuture(null);
            CompletableFuture<byte[]> futureKillWeaponBytes = killWeapon != null ? serializeBukkitBinary(killWeapon) : CompletableFuture.completedFuture(null);

            CompletableFuture<Integer> futureDeathId = new CompletableFuture<>();
            serializeBukkitBinary(playerInventory).thenAccept(bytes ->
                    futureKillWeaponBytes.thenAccept(killWeaponBytes ->
                            futureUserFightId.thenAccept(userFightId ->
                                    futureKillerInventory.thenAccept(killerInventoryBytes ->
                                            fetchPgCallNonnullT("{? = call insert_user_death_return_id(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}", Types.INTEGER, new Object[]{POSTGRESQL_SERVER_ID, userFightId, data.uuid, bytes, deathLocation.getWorld().getName(), deathLocation.getBlockX(), deathLocation.getBlockY(), deathLocation.getBlockZ(), plainDeathMessage, killerUuid, killWeaponBytes, killerInventoryBytes}, Integer.class)
                                                    .thenAccept(futureDeathId::complete)))));
            // KillTracker end

            if (playerEntity.getKiller() instanceof Player p1)
                futureDeathId
                        .thenRun(() -> playerDataCache.get(p1).fetchCurrentKillStreak()
                                .thenAccept(killStreak -> {
                                    if (killStreak % 5 == 0 || (!IS_KIT_SERVER && killStreak > 1))
                                        Bukkit.broadcast(getFocusComponent(p1.getName() + " has reached a killstreak of " + killStreak));
                                    else
                                        p1.sendMessage(getFocusComponent("killstreak: " + killStreak));

                                    // KillRewards start
                                    if (IS_KIT_SERVER) {
                                        for (ItemStack is : getLootTableLoot(1, 0, killStreak % 10 == 0
                                                ? SUPPLY_DROP
                                                : KILL_STREAK_REWARDS))
                                            handleGivePlayerItem(p1.getInventory(), p1, is);
                                    }
                                    // KillRewards end
                                }));

            // Deathbanz start
            CompletableFuture<Boolean> futureIsDeathIpExempt = fetchUserIsIpExempt(data.uuid);
            futureDeathId
                    .thenAccept(deathId ->
                            fetchOptionalDict(POSTGRES_POOL, "SELECT * FROM handle_insert_deathban_return_duration_data_if_inserted(?, ?, ?, ?)", new Object[]{POSTGRESQL_SERVER_ID, Bukkit.getOfflinePlayer(data.uuid).getStatistic(Statistic.PLAY_ONE_MINUTE) / 20, deathId, getHmacBytes(CURRENT_DEATHBANS_IP_HMAC_KEY, data.hostAddress)}) // TODO -> use player if avail ?
                                    .thenAccept(optional -> optional
                                            .ifPresent(dict -> {
                                                String deathbanMsg = "deathbanned for " + (float) dict.get("death_ban_seconds") / 60 + " minutes: " + plainDeathMessage;
                                                Component ipBanMessage = getNormalComponent("ip-" + deathbanMsg);
                                                playerEntity.sendMessage((Boolean)dict.get("is_ip_only") ? ipBanMessage : getNormalComponent(deathbanMsg));
                                                for (Player player : Bukkit.getOnlinePlayers()) {
                                                    if ((player != playerEntity || player.getHealth() > 0) && playerDataCache.get(player).hostAddress.equals(data.hostAddress)) {
                                                        CompletableFuture<Boolean> futureIsPlayerIterationIpExempt = fetchUserIsIpExempt(player.getUniqueId());
                                                        futureIsDeathIpExempt.thenAccept(isDeathIpExempt -> {
                                                            if (!isDeathIpExempt)
                                                                futureIsPlayerIterationIpExempt.thenAccept(isPlayerIpExempt -> {
                                                                    if (!isPlayerIpExempt)
                                                                        player.kick(ipBanMessage);
                                                                });
                                                        });
                                                    }
                                                    }
                                            })));
            // Deathbanz end
        }
        // Dueling end
    }
    static CompletableFuture<Boolean> fetchUserIsIpExempt(UUID uuid) {
        return fetchPgCallNonnullT("{? = call get_user_is_ip_exempt(?, ?)}", Types.BOOLEAN, new Object[]{uuid, POSTGRESQL_SERVER_ID}, Boolean.class);
    }

    static void handleKillLoreItem(ItemStack killWeapon, String string, Component killLoreComponent) {
        if (killWeapon != null && killWeapon.getItemMeta() instanceof ItemMeta itemMeta) {
            PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();
            if (!(killWeapon.lore() instanceof List<Component> lore)) {
                persistentDataContainer.set(KILL_LORE_COUNT_KEY, PersistentDataType.INTEGER, 1);
                itemMeta.lore(List.of(killLoreComponent, Component.text("total " + string + ": " + 1)));
            }
            else {
//                while (lore.size() >= 3)
//                    lore.removeLast();
                int newTotalKills = persistentDataContainer.get(KILL_LORE_COUNT_KEY, PersistentDataType.INTEGER) + 1;
                persistentDataContainer.set(KILL_LORE_COUNT_KEY, PersistentDataType.INTEGER, newTotalKills);
                lore.set(lore.size() - 1, killLoreComponent);
                lore.addLast(Component.text("total " + string + ": " + newTotalKills));
                // TODO -> make a command for seeing deaths in an itemStack
                itemMeta.lore(lore);
            }
            killWeapon.setItemMeta(itemMeta);
        }
    }
}
