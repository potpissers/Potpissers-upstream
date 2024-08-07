package com.memeasaur.potpissersdefault.Commands.Kits;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.SPAWN_CLAIM;
import static com.memeasaur.potpissersdefault.Util.Combat.Methods.handleGivePlayerItem;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.EST;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getConsoleComponent;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getNormalComponent;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.fetchBukkitObject;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Postgres.Methods.executeDefaultConsumableKitNamesCacheUpdate;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Postgres.Methods.fetchDefaultKitContents;

public class KitsCommands implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof Player p) {
            switch (command.getName().toLowerCase()) {
                case "save" -> { // sends to save kit area OR checks contents, which sounds kinda aids
                    // TODO impl
                }
                case "load" -> {
                    if (!IS_KIT_SERVER)
                        p.sendMessage("invalid (disabled)");
                    // TODO impl
                }
                case "loaddefault" -> {
                    if (!IS_KIT_SERVER)
                        p.sendMessage("invalid (disabled)");
                    else {
                        String kitName = strings.length > 0 ? strings[0] : defaultKitName;
                        if (!playerDataCache.get(p).currentClaim.equals(SPAWN_CLAIM) && !p.getGameMode().equals(GameMode.CREATIVE))
                            p.sendMessage("invalid (spawn)");
                        else if (!immutableDefaultKitNamesCache.contains(kitName))
                            p.sendMessage("invalid (kit name)");
                        else
                            fetchDefaultKitContents(kitName)
                                    .thenAccept(kitContents -> {
                                        p.getInventory().setContents(kitContents);
                                        p.sendMessage(getNormalComponent("loaded: " + kitName + " default"));
                                    });
                    }
                    return true;
                }
                case "remove" -> {
                    if (strings.length == 0)
                        // TODO gui
                        return true;
                    else {
                        // TODO impl
                    }
                    return true;
                }
                case "removeall" -> {
                    if (strings.length != 0) {
                        p.sendMessage("usage: ./removeall");
                        return true;
                    }
                    // TODO impl
                }
                case "kit" -> {
                    executeDefaultConsumableKitNamesCacheUpdate();

                    if (strings.length > 1)
                        p.sendMessage(getConsoleComponent("?"));
                    if (strings.length == 0)
                        p.sendMessage("invalid (args)");
                    else
                        fetchOptionalDict(POSTGRES_POOL, "SELECT * FROM get_nullable_newest_server_consumable_kits_data_timestamp(?, ?, ?)", new Object[]{p.getUniqueId(), POSTGRESQL_SERVER_ID, strings[0]})
                                .thenAccept(optionalDict -> {
                                    if (optionalDict.isEmpty())
                                        p.sendMessage("invalid (kit name)");
                                    else {
                                        HashMap<String, Object> dict = optionalDict.get();
                                        Integer cooldownSeconds = (Integer) dict.get("cooldown");
                                        if (dict.get("timestamp") instanceof Timestamp timestamp && timestamp.toLocalDateTime().atZone(EST).plusSeconds(cooldownSeconds).isAfter(ZonedDateTime.now()))
                                            p.sendMessage("invalid (cooldown)");
                                        else {
                                            CompletableFuture<ItemStack[]> futureKitContents = fetchBukkitObject((byte[]) dict.get("bukkit_kit_contents"), ItemStack[].class);
                                            fetchQueryVoid(POSTGRES_POOL, "call insert_user_consumable_kit_history_entry(?, ?)", new Object[]{p.getUniqueId(), dict.get("id")})
                                                    .thenRun(() -> futureKitContents
                                                            .thenAccept(kitContents -> {
                                                                PlayerInventory playerInventory = p.getInventory();
                                                                for (ItemStack itemStack : kitContents)
                                                                    handleGivePlayerItem(playerInventory, p, itemStack);
                                                                p.sendMessage(getNormalComponent(strings[0] + " kit consumed"));
                                                            }));
                                        }
                                    }
                                });
                    return true;
                }
            }
        }
        return false;
    }
}
