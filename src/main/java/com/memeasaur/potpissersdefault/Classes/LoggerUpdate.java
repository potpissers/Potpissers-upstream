package com.memeasaur.potpissersdefault.Classes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Timer.handleTimerCancel;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.serializeBukkitBinary;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.fetchQueryVoid;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Sqlite.Constants.INSERT_NULL_LOGGER_UPDATE;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Sqlite.Constants.INSERT_LOGGER_UPDATE;

public record LoggerUpdate(Double health, Location location, ItemStack[] inventory) {
    public CompletableFuture<Void> handleLoggerUpdateData(PlayerData data, Piglin piglin) {
        loggerUpdateCache.put(data.uuid, this);
        playerLoggerCache.remove(data.uuid);
        loggerDataCache.remove(piglin);
        handleTimerCancel(data, piglin);
        piglin.remove();

        CompletableFuture<Void> completableQuery = new CompletableFuture<>();

        CompletableFuture<byte[]> futureLocation = serializeBukkitBinary(this.location().clone());
        serializeBukkitBinary(this.inventory()) // TODO deep copy ? or handling
                .thenAccept(inventoryBytes -> futureLocation
                        .thenAccept(locationBytes ->
                                fetchQueryVoid(SQLITE_POOL, INSERT_LOGGER_UPDATE, new Object[]{data.sqliteId, this.health(), locationBytes, inventoryBytes}))
                        .thenRun(() -> {
                            if (Bukkit.getPlayer(data.uuid) instanceof Player p) {
                                p.setHealth(this.health());
                                p.teleport(this.location());
                                fetchQueryVoid(SQLITE_POOL, INSERT_NULL_LOGGER_UPDATE, new Object[]{data.sqliteId})
                                        .thenRun(() -> {
                                            p.getInventory().setContents(this.inventory());
                                            completableQuery.complete(null);
                                        });
                            }
                            else
                                completableQuery.complete(null);
                        }));

        return completableQuery;
    }
}
