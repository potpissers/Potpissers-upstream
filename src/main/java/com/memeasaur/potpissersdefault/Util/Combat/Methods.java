package com.memeasaur.potpissersdefault.Util.Combat;

import com.memeasaur.potpissersdefault.Classes.LootTableType;
import com.memeasaur.potpissersdefault.Classes.PlayerData;
import com.memeasaur.potpissersdefault.Classes.ScoreboardData;
import com.memeasaur.potpissersdefault.Classes.ScoreboardTimer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.memeasaur.potpissersdefault.Classes.ScoreboardData.*;
import static com.memeasaur.potpissersdefault.PotpissersDefault.*;

import java.util.function.Supplier;

import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.KNOCKBACK_CD;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Timer.handleScoreboardTimer;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Sqlite.Methods.fetchUpdatePlayerDataIntVoid;

public class Methods {
    // Combat tag start
    public static void handleCombatTag(PlayerData data, int tagLength) {
        if (data.combatTag > 0)
            data.combatTag = Math.max(data.combatTag, tagLength);
        else {
            data.combatTag = tagLength;

            Scoreboard scoreboard = getScoreboard(data.uuid);
            if (scoreboard != null)
                doScoreboardNumbers(scoreboard, COMBAT, data.combatTag);

            new BukkitRunnable() {
                CompletableFuture<Void> saveCombatTagQuery = fetchUpdatePlayerDataIntVoid("combat_tag", data.combatTag, data.sqliteId);
                final UUID uuid = data.uuid;
                @Override
                public void run() {
                    if (data.combatTag > 0)
                        data.combatTag--;

                    Scoreboard scoreboard = getScoreboard(uuid);
                    if (scoreboard != null)
                        doScoreboardNumbers(scoreboard, COMBAT, data.combatTag);

                    if (data.combatTag == 0) {
                        // timers check TODO ?
//                        currentlyTaggedPlayers.remove(data.uuid);
                        if (Bukkit.getPlayer(uuid) instanceof Player player)
                            data.spawnGlassLocations.forEach(location ->
                                    player.sendBlockChange(location, location.getBlock().getBlockData()));
                        data.spawnGlassLocations.clear();

                        saveCombatTagQuery
                                .thenRun(() -> fetchUpdatePlayerDataIntVoid("combat_tag", data.combatTag, data.sqliteId));

                        if (scoreboard != null)
                            scoreboard.getObjective(SCOREBOARD_STRING).getScore(COMBAT.string).resetScore();
                        cancel();
                    }
                    else if (saveCombatTagQuery.isDone())
                        saveCombatTagQuery = fetchUpdatePlayerDataIntVoid("combat_tag", data.combatTag, data.sqliteId);
                }
            }.runTaskTimer(plugin, 20L, 20L);
        }
    }
    @Nullable
    public static Scoreboard getScoreboard(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null ? player.getScoreboard() : null;
    }
    public static void doScoreboardNumbers(Scoreboard scoreboard, ScoreboardData scoreboardData, int number) {
        scoreboard.getObjective(SCOREBOARD_STRING).getScore(scoreboardData.string).setScore(scoreboardData.score);
        if (number >= 60)
            scoreboard.getTeam(scoreboardData.string).suffix(Component.text((number / 60) + "m" + number % 60 + "s"));
        else
            scoreboard.getTeam(scoreboardData.string).suffix(Component.text(number + "s"));
    }
    // Combat tag end

    // KillTracker start
    public static List<ItemStack> getLootTableLoot(int minLoot, int lootVariance, LootTableType lootTableType)  {
        var lootTable = lootTableType.lootTable;
        if (lootTable.isEmpty())
            return List.of();
        else {
            List<ItemStack> loot = new ArrayList<>();
            while (loot.size() < minLoot + lootVariance) {
                Map.Entry<Supplier<ItemStack>, Double> entry = lootTable.get(new Random().nextInt(lootTable.size()));
                if (entry.getValue() > Math.random())
                    loot.add(entry.getKey().get());
            }
            return loot;
        }
    }
    public static void handleGivePlayerItem(PlayerInventory pi, Player p, ItemStack is) {
        if (pi.getItemInMainHand().isEmpty())
            pi.setItemInMainHand(is);
        else if (pi.firstEmpty() != -1)
            pi.addItem(is);
        else {
            Item item = p.getWorld().dropItem(p.getEyeLocation(), is);
            item.setVelocity(p.getLocation().getDirection().multiply(0.3));
        }
    } // test magnitude TODO
    // KillTracker end

    // Movement cd start
    public static void handleAirborneSpammableMovementCd(Player p, PlayerData data, int cooldown) {
        ScoreboardTimer movement = data.movementTimer;
        movement.optionalSpamBuffer = Math.max(movement.optionalSpamBuffer, KNOCKBACK_CD);
        new BukkitRunnable() { // TODO could also try storing locations and calculating based on that
            @Override
            public void run() {
                if (p.getLocation().getBlock().getRelative(BlockFace.DOWN).isSolid() && p.getVelocity().getY() >= 0) {
                    movement.optionalSpamBuffer = 0;
                    cancel();
                    return;
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);

        handleScoreboardTimer(movement, cooldown, data, data.uuid, data.sqliteId);
    }
    // Movement cd end
    // Parties start
    public static String getLocationString(Location location) {
        return location.getWorld().getEnvironment().name() + ", " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }
    // Parties end
}
