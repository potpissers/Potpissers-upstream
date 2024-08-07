package com.memeasaur.potpissersdefault.Commands.Kits;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;

public class KitsTabCompleter implements TabCompleter {
    //                        fetchDictList(postgresqlConnection, RETURN_KITS, null)
//                                .thenAccept(list -> {
//                            for (HashMap<String, Object> dict : list) // TODO -> this doesn't add the kit deserialization the the blocking tasks
//                                fetchBukkitObject((byte[]) dict.get("bukkit_default_loadout"), ItemStack[].class)
//                                        .thenAccept(kitContents -> defaultKits.put((String) dict.get("kit_name"), kitContents));
//                        }),
//                        fetchDictList(postgresqlConnection, RETURN_CONSUMABLE_KITS, List.of(postgresqlServerId))
//                                .thenAccept(list -> {
//                            for (HashMap<String, Object> dict : list) // TODO -> this doesn't add the kit deserialization the the blocking tasks
//                                fetchBukkitObject((byte[]) dict.get("bukkit_kit_contents"), ItemStack[].class)
//                                        .thenAccept(kitContents -> {
//                                            consumableKits.put((String) dict.get("kit_name"), kitContents);
//                                            if (dict.get("cooldown") != null)
//                                                refillingConsumableKitsCooldowns.put((String) dict.get("kit_name"), (Integer) dict.get("cooldown"));
//                                        });
//                        }),
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof Player) {
            switch (command.getName().toLowerCase()) {
                case "loaddefault", "anon" -> {
                    if (strings.length == 1)
                        return immutableDefaultKitNamesCache;
                }
                case "kit" -> {
                    if (strings.length == 1)
                        return consumableKitNamesCache;
                }
            }
        }
        return List.of();
    }
}
