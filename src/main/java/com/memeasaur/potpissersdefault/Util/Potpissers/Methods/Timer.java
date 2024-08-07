package com.memeasaur.potpissersdefault.Util.Potpissers.Methods;

import com.memeasaur.potpissersdefault.Classes.LocationCoordinate;
import com.memeasaur.potpissersdefault.Classes.PlayerData;
import com.memeasaur.potpissersdefault.Classes.ScoreboardData;
import com.memeasaur.potpissersdefault.Classes.ScoreboardTimer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;

import static com.memeasaur.potpissersdefault.Classes.ScoreboardData.SCOREBOARD_STRING;
import static com.memeasaur.potpissersdefault.Util.Combat.Constants.HARMING_DAMAGE_CD;

import static com.memeasaur.potpissersdefault.PotpissersDefault.plugin;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.memeasaur.potpissersdefault.Util.Combat.Methods.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.COMBAT_TAG;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Sqlite.Methods.fetchUpdatePlayerDataIntVoid;

public class Timer {
    public static void handleTimerCancel(PlayerData data, LivingEntity playerEntity) {
        data.logoutTeleportTimer = 0;
        if (data.logoutTeleportTask instanceof BukkitTask task && !task.isCancelled()) {
            task.cancel();
            fetchUpdatePlayerDataIntVoid("logout_teleport_timer", data.logoutTeleportTimer, data.sqliteId);
            playerEntity.sendActionBar(Component.text(""));
        }
        // Claims start
        data.handleTpaCancel(playerEntity);
        // Claims end

        // Shulker cd start
        if (!(data.shulkerLocation instanceof LocationCoordinate locationCoordinate))
            data.shulkerCd = 0;
        else if (playerEntity.getLocation().distance(locationCoordinate.toLocation()) > 5) {
            if (data.shulkerTimerTask instanceof BukkitTask task && !task.isCancelled()) {
                task.cancel();
                playerEntity.sendActionBar(Component.text(""));
            }
            data.shulkerCd = 0;
            data.shulkerLocation = null;
        }
        // Shulker cd end
    }
    // Scoreboard timers start
    public static void handleScoreboardTimer(ScoreboardTimer scoreboardTimer, int cooldown, PlayerData data, UUID uuid, Integer sqliteId) {
        final ScoreboardData scoreboardData = scoreboardTimer.scoreboardData;
        scoreboardTimer.timer += cooldown;

        if (cooldown != 0 && scoreboardTimer.timer > cooldown)
            handleCombatTag(data, COMBAT_TAG);

        if (scoreboardTimer.timer == cooldown || cooldown == 0) {
            Scoreboard scoreboard = getScoreboard(uuid);
            if (scoreboard != null)
                doScoreboardNumbers(scoreboard, scoreboardTimer.scoreboardData, scoreboardTimer.timer);

            new BukkitRunnable() {
                CompletableFuture<Void> saveLogoutTimerQuery = fetchUpdatePlayerDataIntVoid(scoreboardData.sqliteColumnName, scoreboardTimer.timer, sqliteId);
                @Override
                public void run() {
                    if (scoreboardTimer.timer > 0)
                        scoreboardTimer.timer--;
                    if (scoreboardTimer.optionalSpamBuffer != null && scoreboardTimer.optionalSpamBuffer > 0)
                        scoreboardTimer.optionalSpamBuffer--;
                    Scoreboard scoreboard = getScoreboard(uuid);
                    if (scoreboard != null)
                        doScoreboardNumbers(scoreboard, scoreboardData, scoreboardTimer.timer);
                    if (scoreboardTimer.timer == 0) {
                        if (scoreboard != null)
                            scoreboard.getObjective(SCOREBOARD_STRING).getScore(scoreboardData.string).resetScore();

                        saveLogoutTimerQuery.thenRun(() ->
                                fetchUpdatePlayerDataIntVoid(scoreboardData.sqliteColumnName, scoreboardTimer.timer, sqliteId));

                        cancel();
                    }
                    else
                        if (saveLogoutTimerQuery.isDone())
                            saveLogoutTimerQuery = fetchUpdatePlayerDataIntVoid(scoreboardData.sqliteColumnName, scoreboardTimer.timer, sqliteId);
                }
            }.runTaskTimer(plugin, 20L, 20L);
        }
    }
    // Scoreboard timers end

    // Harming cd start
    public static void doHarmingDamageCd(PlayerData data) {
        if (data.harmingDamageCd == 0)
            new BukkitRunnable() {
                @Override
                public void run() {
                    data.harmingDamageCd--;
                    if (data.harmingDamageCd == 0)
                        cancel();
                }
            }.runTaskTimer(plugin, 20L, 20L);

        data.harmingDamageCd = HARMING_DAMAGE_CD;
    }
    // Harming cd end
}
