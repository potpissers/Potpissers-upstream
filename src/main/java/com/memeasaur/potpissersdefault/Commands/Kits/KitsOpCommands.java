package com.memeasaur.potpissersdefault.Commands.Kits;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.KEY_CUBECORE_CHEST;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getFocusComponent;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.serializeBukkitBinary;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.fetchPgCallNonnullT;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.fetchQueryVoid;

public class KitsOpCommands implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof Player p) {
            switch (command.getName().toLowerCase()) {
                case "savedefault" -> {
                    if (strings.length > 1)
                        p.sendMessage("?");
                    PlayerInventory pInv = p.getInventory();
                    ItemStack[] pi = pInv.getContents();
                    serializeBukkitBinary(pi.clone())
                            .thenAccept(bytes ->
                                    fetchQueryVoid(POSTGRES_POOL, "call upsert_default_kit(?, ?)", new Object[]{strings[0], bytes})
                                            .thenRun(() -> {
                                                immutableDefaultKitNamesCache = Stream.concat(immutableDefaultKitNamesCache.stream(), Stream.of(strings[0])).toList();
                                                pInv.setContents(pi);
                                                Bukkit.broadcast(getFocusComponent(p.getName() + " has added " + strings[0] + " to /loaddefault"));
                                            }));
                    return true;
                }
                case "removedefault" -> { // TODO -> do variable args meme
                    if (strings.length > 1)
                        p.sendMessage("?");
                    fetchPgCallNonnullT("{? = CALL delete_default_kit_return_remaining_names(?)}", Types.ARRAY, new Object[]{strings[0]}, String[].class)
                            .thenAccept(array -> {
                                immutableDefaultKitNamesCache = List.of(array);
                                Bukkit.broadcast(getFocusComponent(p.getName() + " has removed " + strings[0] + " from /loaddefault"));
                            });
                    return true;
                }
                case "adddefaultpersonalkit" -> {
                    if (strings.length > 2)
                        p.sendMessage("?");
                    ItemStack[] kitContents = Arrays.stream(p.getInventory().getContents())
                            .filter(Objects::nonNull)
                            .map(ItemStack::clone)
                            .toArray(ItemStack[]::new);
                    serializeBukkitBinary(kitContents)
                            .thenAccept(bytes ->
                                    fetchQueryVoid(POSTGRES_POOL, "call upsert_server_consumable_kit(?, ?, ?, ?)", new Object[]{POSTGRESQL_SERVER_ID, strings[0], bytes, strings.length > 1 ? Integer.parseInt(strings[1]) : Integer.MAX_VALUE})
                                            .thenAccept(v ->
                                                    Bukkit.broadcast(getFocusComponent(p.getName() + " has added " + strings[0] + " to /kit"))));
                    return true;
                }
                case "cubecorechest" -> {
                    Block block = p.getTargetBlockExact(4);
                    if (block != null && block.getState() instanceof Chest chest) {
                        PersistentDataContainer pdc = chest.getPersistentDataContainer();
                        if (pdc.has(KEY_CUBECORE_CHEST)) {
                            p.sendMessage("?");
                            return true;
                        }
                        pdc.set(KEY_CUBECORE_CHEST, PersistentDataType.BOOLEAN, Boolean.TRUE);
                        chest.update();
                        p.sendMessage("cubecorechest set");
                        return true;
                    } else {
                        p.sendMessage("?");
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
