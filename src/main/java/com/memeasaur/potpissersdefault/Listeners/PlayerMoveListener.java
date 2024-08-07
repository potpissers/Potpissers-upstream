package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import static com.memeasaur.potpissersdefault.PotpissersDefault.playerDataCache;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static com.memeasaur.potpissersdefault.Util.Claim.Constants.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Methods.getClaim;
import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.PotpissersDefault.worldBorderRadius;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Timer.handleTimerCancel;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.fetchNonnullDict;

public class PlayerMoveListener implements Listener {
    @EventHandler
    void onPlayerMoveCubecore(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        PlayerData data = playerDataCache.get(p);
        if (data.frozen) {
            e.setCancelled(true);
            return;
        }
        // Logout start
        if (p.isSwimming()) {
            if (data.combatTag != 0)
                p.setPose(Pose.STANDING);
            else
                p.setVelocity(p.getLocation().getDirection().multiply(0.07875F));
        }
        else if (p.isGliding() && data.combatTag != 0)
            p.setPose(Pose.STANDING);
        if (e.hasChangedBlock()) {
            // Claims start
            Location location = e.getTo();
            Object toClaim = getClaim(new ClaimCoordinate(location));
            if (!toClaim.equals(data.currentClaim)) {
                switch (toClaim.toString()) {
                    case SPAWN_CLAIM -> {
                        if (data.combatTag != 0) {
//                            e.setCancelled(true);
                            p.sendMessage(getDangerComponent("movement cancelled: combat tagged"));

                            Vector vec = p.getVelocity();
                            vec.setX(0); // TODO -> this should be less sticky
                            vec.setZ(0);
                            {
                                Location from = e.getFrom();
                                Location cancelledTo = location.clone();
                                cancelledTo.setX(from.getBlockX());
                                cancelledTo.setZ(from.getBlockZ());
                                p.teleport(cancelledTo);
                            }
                            p.setVelocity(vec);
                            p.setGliding(false);
                            p.setPose(Pose.STANDING);
                            p.sendBlockChange(location, SPAWN_GLASS_BLOCK_DATA);
                            data.spawnGlassLocations.add(location);
                            return;
                        }
                    }
                    case WILDERNESS_CLAIM -> {
                        if (!p.getGameMode().isInvulnerable()) {
                            if (Math.abs(location.getX()) > worldBorderRadius || Math.abs(location.getZ()) > worldBorderRadius) {
                                e.setCancelled(true);
                                p.sendMessage(getDangerComponent("cancelled (out of bounds)"));
                                return;
                            }
                        }
                    }
                }
                if (data.currentClaim.equals(SPAWN_CLAIM)) {
                    if (data.isQueued() || data.getCurrentParties().anyMatch(AbstractData::isQueued)) {
                        e.setCancelled(true);
                        p.sendMessage(getDangerComponent("cancelled (currently queued)"));
                        return;
                    }
                }

                CompletableFuture<Component> futureExitClaimComponent = data.currentClaim instanceof String stringName
                        ? CompletableFuture.completedFuture(getNormalComponent(stringName))
                        : data.currentClaim instanceof Integer postgresInteger
                        ? fetchArenaComponent(postgresInteger)
                        : null;
                (toClaim instanceof String stringName
                        ? CompletableFuture.completedFuture(getNormalComponent(stringName))
                        : toClaim instanceof Integer postgresInteger
                        ? fetchArenaComponent(postgresInteger)
                        : null)
                        .thenAccept(enterClaimComponent -> futureExitClaimComponent
                                .thenAccept(exitClaimComponent ->
                                        p.sendMessage(
                                                exitClaimComponent.append(getConsoleComponent(" claim exited"))
                                                        .appendNewline()
                                                        .append(enterClaimComponent.append(getConsoleComponent(" claim entered")))
                                        )));
                data.currentClaim = toClaim;
            }
            // Claims end

            // ServerWarping start
            LocationCoordinate locationCoordinate = new LocationCoordinate(location);
            if (serverWarps.containsKey(locationCoordinate)) {
                p.sendPluginMessage(plugin, "potpissers:serverswitcher", serverWarps.get(locationCoordinate).getBytes(StandardCharsets.UTF_8));
                return;
            }
            // ServerWarping end

            // LocationWarping start
            else if (locationWarps.containsKey(locationCoordinate))
                p.teleport(locationWarps.get(locationCoordinate));
            // LocationWarping end

            handleTimerCancel(data, p);
        }
        // Logout end
    }
    CompletableFuture<Component> fetchArenaComponent(Integer postgresInteger) {
        return fetchNonnullDict(POSTGRES_POOL, "SELECT * FROM get_arena_data(?)", new Object[]{postgresInteger})
                .thenCompose(dict ->
                        CompletableFuture.completedFuture(
                                getFocusComponent((String) dict.get("name"))
                                        .append(Component.text(" ("))
                                        .append(getFocusComponent((String) dict.get("creator")))
                                        .append(Component.text(")"))
                        ));
    }
}
