package com.memeasaur.potpissersdefault.Commands;

import com.memeasaur.potpissersdefault.Classes.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.material.Directional;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.sql.Types;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.memeasaur.potpissersdefault.Classes.AbstractPartyData.fetchNonnullPartyName;
import static com.memeasaur.potpissersdefault.Classes.ScoreboardData.SCOREBOARD_STRING;
import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.PotpissersDefault.isMobDamageImmunityToggled;

import static com.memeasaur.potpissersdefault.Util.Claim.Constants.KEY_SUPPLY_DROP_CHEST;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.KEY_SUPPLY_DROP_LOCKED_CHEST;
import static com.memeasaur.potpissersdefault.Util.Crypto.CURRENT_PUNISHMENTS_IP_HMAC_KEY;
import static com.memeasaur.potpissersdefault.Util.Crypto.getHmacBytes;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.*;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods1.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Constants.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Sqlite.Constants.RETURN_STASHED_STATE;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Sqlite.Constants.UPSERT_STASHED_STATE;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Sqlite.Methods.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.*;
import static com.memeasaur.potpissersdefault.Util.Item.Methods.getSpawnCannonElytra;

public class PotpissersOpCommands implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String commandName = command.getName().toLowerCase();
        if (sender instanceof Player p)
            switch (commandName) {
                case "cubecoresign" -> {
                    if (args.length == 0) {
                        p.sendMessage("not args");
                        return true;
                    }
                    if (!(p.getTargetBlockExact(4) instanceof Block block)) {
                        p.sendMessage("not block");
                        return true;
                    } else if (!(block.getState() instanceof Sign)) {
                        p.sendMessage("not sign");
                        return true;
                    }
                    Sign sign = (Sign) block.getState();
                    for (Side side : List.of(Side.FRONT, Side.BACK)) {
                        sign.getSide(side).line(0, Component.text("click here or"));
                        sign.getSide(side).line(1, Component.text("/" + args[0]));
                        if (args.length > 1) {
                            sign.getSide(side).line(2, Component.text(args[1]));
                            if (args.length > 2)
                                sign.getSide(side).line(3, Component.text(args[2]));
                        }
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String arg : args)
                        stringBuilder.append(arg).append(" ");
                    sign.getPersistentDataContainer().set(KEY_CUBECORE_SIGN, PersistentDataType.STRING, stringBuilder.toString());
                    sign.update();
                    p.sendMessage("done");
                    return true;
                }
                case "toggleenforcedwhitelist" -> {
                    boolean bool = !SERVER.isWhitelistEnforced();
                    SERVER.setWhitelistEnforced(bool);
                    p.sendMessage("done: " + bool);
                }
                case "invsee" -> {
                    if (args.length == 0)
                        p.sendMessage("usage");
                    else if (!(Bukkit.getPlayer(args[0]) instanceof Player pArg))
                        p.sendMessage("invalid (player)");
                    else
                        p.openInventory(pArg.getInventory());
                    return true;
                }
                case "vanish" -> {
                    if (!p.isVisibleByDefault())
                        p.sendMessage("already vanished, use /unvanish");
                    else {
                        p.setVisibleByDefault(false);
                        p.sendMessage("done");
                    }
                    return true;
                }
                case "unvanish" -> {
                    if (p.isVisibleByDefault())
                        p.sendMessage("already unvanished");
                    else {
                        p.setVisibleByDefault(true);
                        p.sendMessage("done");
                    }
                    return true;
                }
                case "fly" -> {
                    p.setAllowFlight(!p.getAllowFlight());
                    p.sendMessage("flight: " + p.getAllowFlight());
                    return true;
                }
                case "spectator" -> {
                    p.setGameMode(GameMode.SPECTATOR);
                    p.sendMessage("done");
                    return true;
                }
                case "survival" -> {
                    p.setGameMode(GameMode.SURVIVAL);
                    p.sendMessage("done");
                    return true;
                }
                case "visit" -> {
                    if (args.length < 1)
                        p.sendMessage("?");
                    else if (!(Bukkit.getPlayer(args[0]) instanceof Player pArg))
                        p.sendMessage("?");
                    else {
                        p.teleport(pArg);
                        p.sendMessage("done");
                    }
                    return true;
                }
                case "godmode" -> {
                    p.setInvulnerable(!p.isInvulnerable());
                    p.sendMessage("godmode: " + p.isInvulnerable());
                    return true;
                }
                case "stash" -> {
                    PlayerData data = playerDataCache.get(p);

                    CompletableFuture<byte[]> futureLocation = serializeBukkitBinary(p.getLocation());
                    CompletableFuture<byte[]> futureInventory = serializeBukkitBinary(p.getInventory().getContents());

                    fetchOptionalDict(SQLITE_POOL, RETURN_STASHED_STATE, new Object[]{data.sqliteId})
                            .thenAccept(optionalLoggerUpdateDict -> {
                                futureLocation
                                        .thenAccept(location ->
                                                futureInventory
                                                        .thenAccept(inventory ->
                                                                fetchQueryVoid(SQLITE_POOL, UPSERT_STASHED_STATE, new Object[]{data.sqliteId, p.getHealth(), location, inventory})));
                                optionalLoggerUpdateDict
                                        .ifPresent(loggerUpdateDict -> {
                                            Double health = (Double) loggerUpdateDict.get("health");
                                            CompletableFuture<Location> futureStashedLocation = fetchBukkitObject((byte[]) loggerUpdateDict.get("bukkit_location"), Location.class);
                                            fetchBukkitObject((byte[]) loggerUpdateDict.get("bukkit_inventory"), ItemStack[].class)
                                                    .thenAccept(stashedInventory -> futureStashedLocation.thenAccept(stashedLocation -> {
                                                        p.setHealth(health);
                                                        p.teleport(stashedLocation);
                                                        p.getInventory().setContents(stashedInventory);
                                                    }));
                                        });
                            });

                    Bukkit.broadcast(getFocusComponent(p.getName() + " has /stash'd their current inventory"));
                    return true;
                }
                // LootChests start
                case "setlootchest" -> {
                    handleUpsertLootTableChestCommand(p, args[0], POSTGRESQL_SERVER_ID);
                    return true;
                }
                case "unsetlootchest" -> {
                    handleDeleteLootTableChestCommand(p, POSTGRESQL_SERVER_ID);
                    return true;
                }
                // LootChests end
                // ServerWarping start
                case "addservergateway" -> {
                    if (args.length != 1)
                        sender.sendMessage("not args");
                    else if (!(p.getTargetBlockExact(4) instanceof Block block))
                        p.sendMessage("not block");
                    else if (block.getType().isAir())
                        p.sendMessage("not solid");
                    else
                        handleBlockingFileSerialization("serverWarps", writeBinaryFile(DATA_SERVER_WARPS, serverWarps))
                                .thenAccept(v -> {
                                    serverWarps.put(new LocationCoordinate(block), args[0]);
                                    p.sendMessage("done");
                                });
                    return true;
                }
                case "removeservergateway" -> {
                    if (!(p.getTargetBlockExact(4) instanceof Block block))
                        p.sendMessage("?");
                    else if (!serverWarps.containsKey(new LocationCoordinate(block)))
                        p.sendMessage("?");
                    else
                        handleBlockingFileSerialization("serverWarps", writeBinaryFile(DATA_SERVER_WARPS, serverWarps))
                                .thenAccept(v -> {
                                    p.sendMessage("done");
                                    serverWarps.remove(new LocationCoordinate(block));
                                });
                    return true;
                }
                // ServerWarping end
                // LocationWarping start
                case "addlocationgateway" -> {
                    if (args.length != 1)
                        p.sendMessage("not args");
                    if (!(p.getTargetBlockExact(4) instanceof Block block))
                        p.sendMessage("not block");
                    else if (block.getType().isAir())
                        p.sendMessage("not solid");
                    else if (!privateWarps.containsKey(args[0]))
                        p.sendMessage("not privateWarp");
                    else
                        handleBlockingFileSerialization("locationWarps", writeBinaryFile(DATA_LOCATION_WARPS, locationWarps))
                                .thenAccept(v -> {
                                    p.sendMessage("done");
                                    locationWarps.put(new LocationCoordinate(block), privateWarps.get(args[0]));
                                });
                    return true;
                }
                case "removelocationgateway" -> {
                    if (!(p.getTargetBlockExact(4) instanceof Block block))
                        p.sendMessage("?");
                    else if (!locationWarps.containsKey(new LocationCoordinate(block)))
                        p.sendMessage("?");
                    else
                        handleBlockingFileSerialization("locationWarps", writeBinaryFile(DATA_LOCATION_WARPS, locationWarps))
                                .thenAccept(v -> {
                                    locationWarps.remove(new LocationCoordinate(block));
                                    p.sendMessage("done");
                                });
                    return true;
                }
                // LocationWarping
                // Supply drop start
                case "activatesupplydrop" -> {
                    final OffsetDateTime chestOpenTime;
                    final Integer nullableServerKothId;
                    int restockAmount;
                    int dropRadius;
                    int restockTimer;
                    boolean isMovementRestricted;
                    Block dropCenterLocation = p.getLocation().getBlock();
                    try {
                        chestOpenTime = OffsetDateTime.now().plusSeconds(Integer.parseInt(args[0]));
                        restockAmount = Integer.parseInt(args[1]);
                        dropRadius = Integer.parseInt(args[2]);
                        restockTimer = Integer.parseInt(args[3]) / restockAmount;
                        isMovementRestricted = Boolean.parseBoolean(args[4]);
                    } catch (Exception e) {
                        p.sendMessage("invalid usage: /activatesupplydrop (travelTimeSeconds) (restockAmount) (dropRadius) (durationSeconds) (isMovementRestricted)");
                        return true;
                    }
                    fetchNonnullDict(POSTGRES_POOL, "SELECT * FROM insert_supply_drop_return_data(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[]{POSTGRESQL_SERVER_ID, dropCenterLocation.getWorld().getName(), dropCenterLocation.getX(), dropCenterLocation.getY(), dropCenterLocation.getZ(), dropRadius, chestOpenTime, restockTimer, restockAmount, null, isMovementRestricted})
                            .thenAccept(dict -> {
                                handleSupplyDropRound(handleSupplyDropSpawnReturnLocationCoordinates((int) dict.get("loot_factor"), dropCenterLocation, dropRadius), (Integer) dict.get("supply_drop_id"), chestOpenTime);
                                Bukkit.broadcast(getFocusComponent("a supply drop has appeared around " + dropCenterLocation.getX() + ", " + dropCenterLocation.getY() + ", " + dropCenterLocation.getZ()));
                            });
                    return true;
                }
                // Supply drop end
            }
        switch (commandName) {
            case "heal" -> {
                if (args.length < 2)
                    sender.sendMessage("invalid (args)");
                else if (!(Bukkit.getPlayer(args[0]) instanceof Player pArg))
                    sender.sendMessage("invalid (player)");
                else {
                    pArg.setHealth(20);
                    pArg.setFoodLevel(20);
                    Bukkit.broadcast(getFocusComponent(pArg.getName() + " has been /heal'd by " + sender.getName() + ". reason: " + getPotpissersCommandReason(args)));
                }
                return true;
            }
            case "opmute" -> {
                if (args.length < 3)
                    sender.sendMessage("?");
                else if (!args[1].matches("\\d+"))
                    sender.sendMessage("invalid (value)");
                else
                    doMute(args[0], Long.parseLong(args[1]), String.join(" ", Arrays.copyOfRange(args, 2, args.length)), sender);
                return true;
            }
            case "opunmute" -> {
                handleAbstractReducePunishmentCommand(args, sender, "mute");
                return true;
            }
            case "mute" -> {
                if (args.length < 2)
                    sender.sendMessage("invalid (args). usage: /mute (reason)");
                else
                    doMute(args[0], 3, getPotpissersCommandReason(args), sender);
                return true;
            }
            case "unmute" -> {
                if (args.length < 2)
                    sender.sendMessage("?");
                else {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
                    fetchOptionalDict(POSTGRES_POOL, "SELECT * FROM reduce_user_punishments_by_type_by_length_returning_star(?, ?, ?, ?)", new Object[]{offlinePlayer.getUniqueId(), POSTGRESQL_SERVER_ID, "mute", 3})
                            .thenAccept(optional -> {
                                if (optional.isEmpty())
                                    sender.sendMessage("invalid (mute)");
                                else
                                    Bukkit.broadcast(getFocusComponent(offlinePlayer.getName() + " has been un-mute'd by " + sender.getName() + ". reason: " + args[1]));
                            });
                }
            }
            case "opban" -> {
                if (args.length < 3)
                    sender.sendMessage("invalid (args). usage: /x (minuteLength) (oneWordReason)"); // TODO -> varArgs handle
                else if (!args[1].matches("\\d+"))
                    sender.sendMessage("invalid (value)");
                else {
                    OfflinePlayer oPArg = Bukkit.getOfflinePlayer(args[0]);
                    String reason = getPotpissersCommandReason(args);
                    String playerAddress = getNullablePlayerAddress(oPArg);
                    fetchQueryVoid(POSTGRES_POOL, "call insert_user_punishment(?, ?, ?, ?, ?, ?)", new Object[]{oPArg.getUniqueId(), POSTGRESQL_SERVER_ID, "ban", reason, OffsetDateTime.now().plusMinutes(Long.parseLong(args[1])), getHmacBytes(CURRENT_PUNISHMENTS_IP_HMAC_KEY, playerAddress)})
                            .thenRun(() -> {
                                if (oPArg.getPlayer() instanceof Player pArg) {
                                    Component banComponent = getDangerComponent("you were temp-banned for " + args[1] + " minutes. reason: " + reason);
                                    pArg.kick(banComponent);
                                    sender.sendMessage("kicked");
//                                    for (Player playerIteration : Bukkit.getOnlinePlayers())
//                                        if (playerIteration != pArg && playerDataCache.get(playerIteration).equals(playerAddress)) {
//                                            playerIteration.kick(banComponent);
//                                            p.sendMessage("kicked " + playerIteration.getName());
//                                        } // TODO -> handle isIpExempt
                                }
                                sender.sendMessage("done");
                            });
                }
                return true;
            }
            case "opunban" -> {
                handleAbstractReducePunishmentCommand(args, sender, "ban");
                return true;
            }
            case "tempban" -> {
                // TODO -> impl
            }
            case "feed" -> {
                if (args.length < 2)
                    sender.sendMessage("invalid (args)");
                else if (!(Bukkit.getPlayer(args[0]) instanceof Player pArg))
                    sender.sendMessage("invalid (player)");
                else {
                    pArg.setFoodLevel(20);
                    pArg.setSaturation(20);
                    Bukkit.broadcast(getFocusComponent(pArg.getName() + " has been /feed'd by " + sender.getName() + ". reason: " + getPotpissersCommandReason(args)));
                }
                return true;
            }
            case "freeze" -> {
                handleFreezeCommand(args, sender, true);
                return true;
            }
            case "unfreeze" -> {
                handleFreezeCommand(args, sender, false);
                return true;
            }
            case "setdefaultattackspeed" -> {
                if (args.length < 1)
                    sender.sendMessage("?");
                else if (!ATTACK_SPEED_VALUES.containsKey(args[0]))
                    sender.sendMessage("?");
                else
                    fetchQueryVoid(POSTGRES_POOL, "call update_server_attack_speed(?, ?)", new Object[]{args[0], POSTGRESQL_SERVER_ID})
                            .thenRun(() -> {
                                defaultAttackSpeedName = args[0];
                                Bukkit.broadcast(getFocusComponent("the default attack speed has been updated to " + args[0]));
                            });
                return true;
            }
            case "setworldborder" -> {
                if (args.length < 1)
                    sender.sendMessage("?");
                else {
                    int value = Integer.parseInt(args[0]);
                    fetchQueryVoid(POSTGRES_POOL, "call update_server_world_border_radius(?, ?)", new Object[]{value, POSTGRESQL_SERVER_ID})
                            .thenRun(() -> {
                                worldBorderRadius = value;
                                Bukkit.broadcast(getFocusComponent("the world border radius has been updated to " + value));
                            });
                }
            }
            case "setdefaultkitname" -> {
                if (args.length < 1)
                    sender.sendMessage("?");
                else
                    fetchQueryVoid(POSTGRES_POOL, "call update_default_kit_name(?, ?)", new Object[]{args[0], POSTGRESQL_SERVER_ID})
                            .thenRun(() -> {
                                defaultKitName = args[0];
                                Bukkit.broadcast(getFocusComponent("the default kit name has been updated to " + args[0]));
                            });
            }
            case "togglemobimmunity" -> {
                isMobDamageImmunityToggled = !isMobDamageImmunityToggled;
                Bukkit.broadcast(getFocusComponent(sender.getName() + " has toggled mob damage immunity: " + isMobDamageImmunityToggled));
                return true;
            }
            case "repair" -> {
                if (args.length < 2)
                    sender.sendMessage("invalid (args)");
                else if (!(Bukkit.getPlayer(args[0]) instanceof Player pArg))
                    sender.sendMessage("invalid (player)");
                else {
                    for (ItemStack itemStack : pArg.getInventory().getArmorContents())
                        if (itemStack != null && itemStack.getItemMeta() instanceof Damageable damageable) {
                            damageable.setDamage(0);
                            itemStack.setItemMeta(damageable);
                        }
                    Bukkit.broadcast(getFocusComponent(pArg.getName() + " has been /repair'd by " + sender.getName() + ". reason: " + getPotpissersCommandReason(args)));
                }
                return true;
            }
            case "toggleipexempt" -> {
                if (args.length < 1)
                    sender.sendMessage("?");
                else {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
                    fetchOptionalDict(POSTGRES_POOL, "SELECT * FROM toggle_is_user_ip_exempt_returning_star(?, ?)", new Object[]{offlinePlayer.getUniqueId(), POSTGRESQL_SERVER_ID})
                            .thenAccept(optional ->
                                    sender.sendMessage(offlinePlayer.getName() + " has had their ip exemption toggled: " + optional.isEmpty()));
                }
                return true;
            }
            case "tpall" -> {
                if (args.length == 0)
                    sender.sendMessage("?");
                else if (!(Bukkit.getPlayer(args[0]) instanceof Player pArg))
                    sender.sendMessage("?");
                else {
                    for (Player playerIteration : Bukkit.getOnlinePlayers())
                        playerIteration.teleport(pArg.getLocation());
                    Bukkit.broadcast(getFocusComponent(sender.getName() + " has /tpall'd"));
                }
                return true;
            }

            // Combat tag start
            case "getcombattags" -> {
                StringBuilder stringBuilder = new StringBuilder();
                for (PlayerData data : playerDataCache.values())
                    if (data.combatTag != 0)
                        stringBuilder.append(Bukkit.getOfflinePlayer(data.uuid).getName()).append(" ");
                sender.sendMessage(stringBuilder.toString());
            }
            // Combat tag end

            // PlayerRanks start
            case "addplayertransaction" -> {
                if (args.length > 2)
                    sender.sendMessage("?");
                if (args.length < 3)
                    sender.sendMessage("usage: /addplayertransaction (name) (game_mode_name) (line_item_name)");
                else if (!(Bukkit.getOfflinePlayer(args[0]).getName() instanceof String name))
                    sender.sendMessage("?");
                else
                    fetchQueryVoid(POSTGRES_POOL, "CALL insert_user_free_successful_transaction(?, ?, ?, ?)", new Object[]{Bukkit.getOfflinePlayer(args[0]).getUniqueId(), args[1], args[2], name})
                            .thenRun(() ->
                                    sender.sendMessage("done"));
                return true;
            }
            case "setplayerstaffrank" -> {
                if (args.length > 2)
                    sender.sendMessage("?");
                if (!CHAT_RANK_COLORS.containsKey(args[1]))
                    sender.sendMessage("?");
                else
                    fetchQueryVoid(POSTGRES_POOL, "call upsert_user_staff_rank(?, ?, ?)", new Object[]{Bukkit.getOfflinePlayer(args[0]).getUniqueId(), POSTGRESQL_SERVER_ID, args[1]})
                            .thenRun(() ->
                                    sender.sendMessage("done"));
                return true;
            }
            case "chatmod" -> {
                if (args.length < 1)
                    sender.sendMessage("?");
                fetchPgCallNonnullT("{? = call toggle_is_user_chat_mod_return_result(?)}", Types.BOOLEAN, new Object[]{Bukkit.getOfflinePlayer(args[0]).getUniqueId()}, Boolean.class)
                        .thenAccept(alreadyExists ->
                                sender.sendMessage("done: current value - " + !alreadyExists));
                return true;
            }
            // PlayerRanks end

            // SpawnCannon start
            case "spawncannon" -> {
                if (args.length < 1)
                    sender.sendMessage("?");
                else if (!(Bukkit.getPlayer(args[0]) instanceof Player pArg))
                    sender.sendMessage("?");
                else {
                    pArg.addPotionEffects(SPAWN_CANNON_PHASE_ONE_EFFECTS);
                    ItemStack chestPlate = pArg.getInventory().getChestplate();
                    pArg.getInventory().setChestplate(getSpawnCannonElytra());
                    spawnCannonChestplates.put(pArg.getUniqueId(), chestPlate);
                    new BukkitRunnable() {
                        boolean isPhaseOne = true;

                        @Override
                        public void run() {
                            if (!pArg.isOnline())
                                cancel();
                            else {
                                if (isPhaseOne) {
                                    if (pArg.getY() < 320)
                                        pArg.addPotionEffects(SPAWN_CANNON_PHASE_ONE_EFFECTS);
                                    else {
                                        Collection<PotionEffect> currentPotionEffects = pArg.getActivePotionEffects();
                                        pArg.clearActivePotionEffects();
                                        currentPotionEffects.removeIf(potionEffect -> potionEffect != null && potionEffect.getType().equals(PotionEffectType.LEVITATION) && potionEffect.getAmplifier() == SPAWN_CANNON_LEVITATION.getAmplifier());
                                        pArg.addPotionEffects(currentPotionEffects);
                                        isPhaseOne = false;
                                        return;
                                    }
                                } else {
                                    if (!((LivingEntity) pArg).isOnGround())
                                        pArg.addPotionEffect(SPAWN_CANNON_GLOWING);
                                    else {
                                        pArg.getInventory().setChestplate(spawnCannonChestplates.get(pArg.getUniqueId()));
                                        cancel();
                                        return;
                                    }
                                }
                            }
                        }
                    }.runTaskTimer(plugin, 1L, 1L);
                }
                return true;
            }
            // SpawnCannon end
            // Spawn kit start
            case "setspawnkit" -> {
                if (sender instanceof Player p) {
                    hubKit = p.getInventory().getContents();
                    p.getInventory().setContents(p.getInventory().getContents());
                    writeBukkitBinaryFile(DATA_SPAWN_KIT, hubKit)
                            .thenRun(() -> p.sendMessage("done"));
                }
            }
            // Spawn kit end
            case "setsharpnesslimit" -> {
                if (args.length == 0) {
                    sender.sendMessage("?");
                    return true;
                } else if (!args[0].matches("\\d+")) {
                    sender.sendMessage("invalid (value)");
                    return true;
                } else {
                    int value = Integer.parseInt(args[0]);
                    fetchQueryVoid(POSTGRES_POOL, "CALL update_server_sharpness_limit(?, ?)", new Object[]{value, POSTGRESQL_SERVER_ID})
                            .thenRun(() -> {
                                sharpnessLimit = value;
                                Bukkit.broadcast(getFocusComponent(sender.getName() + " has updated the sharpness limit to " + value));
                            });
                    return true;
                }
            }
            case "setprotectionlimit" -> {
                if (args.length == 0) {
                    sender.sendMessage("?");
                    return true;
                } else if (!args[0].matches("\\d+")) {
                    sender.sendMessage("invalid (value)");
                    return true;
                } else {
                    int value = Integer.parseInt(args[0]);
                    fetchQueryVoid(POSTGRES_POOL, "CALL update_server_protection_limit(?, ?)", new Object[]{value, POSTGRESQL_SERVER_ID})
                            .thenRun(() -> {
                                protectionLimit = value;
                                Bukkit.broadcast(getFocusComponent(sender.getName() + " has updated the protection limit to " + value));
                            });
                    return true;
                }
            }
            case "setpowerlimit" -> {
                if (args.length == 0) {
                    sender.sendMessage("?");
                    return true;
                } else if (!args[0].matches("\\d+")) {
                    sender.sendMessage("invalid (value)");
                    return true;
                } else {
                    int value = Integer.parseInt(args[0]);
                    fetchQueryVoid(POSTGRES_POOL, "CALL update_server_power_limit(?, ?)", new Object[]{value, POSTGRESQL_SERVER_ID})
                            .thenRun(() -> {
                                powerLimit = value;
                                Bukkit.broadcast(getFocusComponent(sender.getName() + " has updated the power limit to " + value));
                            });
                    return true;
                }
            }
            case "toggleuserunlockedchatprefix" -> {
                if (args.length < 2)
                    sender.sendMessage("?");
                else if (Arrays.stream(ChatPrefix.values()).noneMatch(value -> value.name().equals(args[1])))
                    sender.sendMessage("?");
                else
                    fetchPgCallNonnullT("{? = CALL toggle_user_chat_prefix_returning_result(?, ?)}", Types.BOOLEAN, new Object[]{ChatPrefix.valueOf(args[1]).postgresId, Bukkit.getOfflinePlayer(args[0]).getUniqueId()}, Boolean.class)
                            .thenAccept(isTrue -> {
                                sender.sendMessage("done: " + isTrue);
                                if (Bukkit.getOfflinePlayer(args[0]) instanceof Player pArg)
                                    pArg.sendMessage(args[1] + " /prefix access toggled: " + isTrue);
                            });
            }
        }
        return true;
    }
    void doMute(String pArgName, long muteMinutes, String muteReason, CommandSender sender) {
        OfflinePlayer oPArg = Bukkit.getOfflinePlayer(pArgName);
        String hostAddress = getNullablePlayerAddress(oPArg);
        fetchQueryVoid(POSTGRES_POOL, "call insert_user_punishment(?, ?, ?, ?, ?, ?)", new Object[]{oPArg.getUniqueId(), POSTGRESQL_SERVER_ID, "mute", muteReason, OffsetDateTime.now().plusMinutes(muteMinutes), getHmacBytes(CURRENT_PUNISHMENTS_IP_HMAC_KEY, hostAddress)})
                .thenRun(() -> {
                    if (oPArg.getPlayer() instanceof Player pArg) {
                        pArg.sendMessage(getDangerComponent("you were muted for " + muteMinutes + " minutes. reason: " + muteReason));

                        for (Player player : Bukkit.getOnlinePlayers())
                            if (player != pArg && playerDataCache.get(player).hostAddress.equals(hostAddress))
                                player.sendMessage(getNormalComponent("you were ip-muted for " + muteMinutes + " minutes. reason: " + muteReason));
                        // TODO -> handle ip mutes messaging
                    }
                    sender.sendMessage("done. reason: " + muteReason);
                });
    }
    String getNullablePlayerAddress(OfflinePlayer offlinePlayer) {
        return offlinePlayer.getPlayer() instanceof Player player ? playerDataCache.get(player).hostAddress : playerLoggerCache.get(offlinePlayer.getUniqueId()) instanceof Piglin piglin ? loggerDataCache.get(piglin).playerData.hostAddress : null;
    }
    void handleFreezeCommand(String[] args, CommandSender sender, boolean bool) {
        if (args.length < 1)
            sender.sendMessage("usage: /freeze (player) + /unfreeze (player)");
        else if (!(Bukkit.getPlayer(args[0]) instanceof Player pArg))
            sender.sendMessage("invalid (player)");
        else {
            PlayerData dataArg = playerDataCache.get(pArg);
            dataArg.frozen = bool;
            pArg.sendMessage(getFocusComponent("you've been " + (bool ? "frozen" : "unfrozen")));
            sender.sendMessage(bool ? "frozen" : "unfrozen");
            fetchUpdatePlayerDataVoid(new String[]{"frozen"}, new Object[]{dataArg.frozen, dataArg.sqliteId})
                    .thenRun(() ->
                            sender.sendMessage("done"));
        }
    }
    void handleUpsertLootTableChestCommand(Player p, String argLootTableName, int serverId) {
        if (!(p.getTargetBlockExact(4) instanceof Block block) || (!(block.getState() instanceof BlockInventoryHolder blockInventoryHolder)))
            p.sendMessage("?");
        else if (Arrays.stream(LootTableType.values()).noneMatch(enumValue -> enumValue.name().equals(argLootTableName)))
            p.sendMessage("?");
        else {
            LootChestData data = new LootChestData(LootTableType.valueOf(argLootTableName), 1, 2, 600, block.getBlockData() instanceof Directional directional ? directional.getFacing() : null, block.getType());
            LocationCoordinate locationCoordinate = new LocationCoordinate(block);
            fetchQueryVoid(POSTGRES_POOL, "call upsert_loot_table_chest(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[]{serverId, argLootTableName, locationCoordinate.worldName(), locationCoordinate.x(), locationCoordinate.y(), locationCoordinate.z(), data.minAmount(), data.lootVariance(), data.restockTime(), data.enumChestDirection() instanceof BlockFace blockFace ? blockFace.name() : null, data.blockMaterial().name()})
                    .thenRun(() -> {
                        lootChestsCache.put(locationCoordinate, data);
                        handleLootChestLoot(blockInventoryHolder, data.minAmount(), data.lootVariance(), LootTableType.valueOf(argLootTableName));
                        p.sendMessage("done + filled");
                    });
//                                .thenAccept(lootTableId ->
//                                        CompletableFuture.allOf(lootTableType.lootTable.stream()
//                                                .map(entry -> fetchQueryVoid(POSTGRES_POOL, "call insert_loot_table_entry(?, ?, ?)", new Object[]{lootTableId, PlainTextComponentSerializer.plainText().serialize(entry.getKey().get().displayName()), entry.getValue()}))
//                                                .toArray(CompletableFuture[]::new))
//                                                .thenRun(() -> { // TODO fuck doing this shit br
//                                                }));
        }
    }
    void handleDeleteLootTableChestCommand(Player p, int serverId) {
        if (!(p.getTargetBlockExact(4) instanceof Block block) || (!(block.getState() instanceof BlockInventoryHolder)))
            p.sendMessage("?");
        else if (!lootChestsCache.containsKey(new LocationCoordinate(block)))
            p.sendMessage("?");
        else {
            LocationCoordinate locationCoordinate = new LocationCoordinate(block);
            fetchQueryVoid(POSTGRES_POOL, "call delete_server_loot_chest(?, ?, ?, ?, ?)", new Object[]{serverId, locationCoordinate.worldName(), locationCoordinate.x(), locationCoordinate.y(), locationCoordinate.z()})
                    .thenRun(() -> {
                        lootChestsCache.remove(locationCoordinate);
                        p.sendMessage("done");
                    });
        }
    }
    void handleSupplyDropLockedChest(ArrayList<LocationCoordinate> lootChests, Block chestBlock) {
        lootChests.add(new LocationCoordinate(chestBlock));
        chestBlock.setType(Material.CHEST);
        {
            Chest chest = (Chest) chestBlock.getState();
            chest.getPersistentDataContainer().set(KEY_SUPPLY_DROP_LOCKED_CHEST, PersistentDataType.BOOLEAN, Boolean.TRUE);
            chest.update();
        }
    }
    ArrayList<LocationCoordinate> handleSupplyDropSpawnReturnLocationCoordinates(int lootFactor, Block dropCenterLocation, int dropRadius) {
        ArrayList<LocationCoordinate> lootChests = new ArrayList<>();

        while (lootChests.size() < lootFactor * (RANDOM.nextBoolean() ? 1 : 2)) {
            Block blockIteration = dropCenterLocation.getRelative(RANDOM.nextInt(dropRadius * 2 + 1) - dropRadius, RANDOM.nextInt(dropRadius * 2 + 1) - dropRadius, RANDOM.nextInt(dropRadius * 2 + 1) - dropRadius);
            if (blockIteration.getType().isAir()) {
                if (!blockIteration.getRelative(BlockFace.DOWN).getType().isAir())
                    handleSupplyDropLockedChest(lootChests, blockIteration);
            }
            else if (blockIteration.getRelative(BlockFace.UP).getType().isAir())
                handleSupplyDropLockedChest(lootChests, blockIteration.getRelative(BlockFace.UP));
        }

        return lootChests;
    }
    void handleSupplyDropRound(ArrayList<LocationCoordinate> chestLocations, int supplyDropId, OffsetDateTime chestOpenTimestamp) {
        serializeBinary(chestLocations)
                .thenAccept(bytes -> fetchQueryVoid(POSTGRES_POOL, "CALL insert_supply_drop_round(?, ?)", new Object[]{supplyDropId, bytes})
                        .thenRun(() ->
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        long visibleTimer = Duration.between(OffsetDateTime.now(), chestOpenTimestamp).toSeconds();
                                        String teamString = "supply drop: ";
                                        for (Player player : Bukkit.getOnlinePlayers()) {
                                            Scoreboard scoreboard = player.getScoreboard();
                                            if (scoreboard.getTeam(teamString) == null) {
                                                scoreboard.registerNewTeam(teamString).addEntry(teamString);
                                                scoreboard.getObjective(SCOREBOARD_STRING).getScore(teamString).setScore(50); // TODO -> dynamic this
                                            }
                                            scoreboard.getTeam(teamString).suffix(Component.text(visibleTimer));
                                        }
                                        if (visibleTimer <= 0) {
                                            cancel();
                                            for (LocationCoordinate locationCoordinate : chestLocations) {
                                                Block block = locationCoordinate.toLocation().getBlock();
                                                Chest chest = (Chest) block.getState();
                                                {
                                                    PersistentDataContainer persistentDataContainer = chest.getPersistentDataContainer();
                                                    persistentDataContainer.set(KEY_SUPPLY_DROP_CHEST, PersistentDataType.INTEGER, supplyDropId);
                                                    persistentDataContainer.remove(KEY_SUPPLY_DROP_LOCKED_CHEST);
                                                    chest.update();
                                                }
                                                handleLootChestLoot(chest, 1, 0, LootTableType.SUPPLY_DROP);
                                            }
                                            // TODO delay ?
                                            fetchOptionalDict(POSTGRES_POOL, "SELECT * FROM insert_supply_drop_round_data_return_data_if_continuing(?)", new Object[]{supplyDropId})
                                                    .thenAccept(optionalDict -> {
                                                        if (optionalDict.orElse(null) instanceof HashMap<String, Object> dict) {
                                                            handleSupplyDropRound(handleSupplyDropSpawnReturnLocationCoordinates((Integer) dict.get("loot_factor"), new Location(Bukkit.getWorld((String) dict.get("world_name")), (Integer) dict.get("x"), (Integer) dict.get("y"), (Integer) dict.get("z")).getBlock(), (Integer) dict.get("radius")), supplyDropId, OffsetDateTime.now().plusSeconds((Integer) dict.get("restock_timer")));
                                                        } else {
                                                            for (Player player : Bukkit.getOnlinePlayers()) {
                                                                Scoreboard scoreboard = player.getScoreboard();
                                                                if (scoreboard.getTeam(teamString) instanceof Team team) {
                                                                    team.unregister();
                                                                    scoreboard.getObjective(SCOREBOARD_STRING).getScore(teamString).setScore(50);
                                                                }
                                                            }
                                                            new BukkitRunnable() {
                                                                @Override
                                                                public void run() {
                                                                    chestLocations
                                                                            .removeIf(location ->
                                                                                    !(location.toLocation().getBlock().getState() instanceof Chest chest && chest.getPersistentDataContainer().has(KEY_SUPPLY_DROP_CHEST)));
                                                                    if (chestLocations.isEmpty()) {
                                                                        CompletableFuture.allOf(currentSupplyDropChestQueries.toArray(new CompletableFuture[0]))
                                                                                .thenRun(() ->
                                                                                        fetchNonnullDict(POSTGRES_POOL, "SELECT * FROM get_supply_drop_winner_data(?)", new Object[]{supplyDropId})
                                                                                                .thenAccept(dict -> {
                                                                                                    try {
                                                                                                        int highestPartyAmount = dict.get("highest_party_amount") instanceof Integer integer ? integer : 0;
                                                                                                        int highestUserAmount = (int) dict.get("highest_user_amount");
                                                                                                        String pName = Bukkit.getOfflinePlayer((UUID) dict.get("highest_user_uuid")).getName();

                                                                                                        (highestPartyAmount > highestUserAmount
                                                                                                                ? fetchNonnullPartyName((UUID) dict.get("highest_party_uuid")) // highestPartyAmount > userAmount == (partyUuid != null)
                                                                                                                .thenCompose(partyName -> CompletableFuture.completedFuture(getSupplyDropWinnerPrefix(partyName, highestPartyAmount) + ". top player: " + pName + ". (" + highestUserAmount + ")"))
                                                                                                                : CompletableFuture.completedFuture(getSupplyDropWinnerPrefix(pName, highestUserAmount)))
                                                                                                                .thenAccept(message -> fetchQueryVoid(POSTGRES_POOL, "CALL update_finished_supply_drop(?, ?)", new Object[]{message, supplyDropId}).thenRun(() ->
                                                                                                                        Bukkit.broadcast(getFocusComponent(message))));
                                                                                                    } catch (
                                                                                                            Exception e) {
                                                                                                        handlePotpissersExceptions(null, e);
                                                                                                        throw new RuntimeException();
                                                                                                    }
                                                                                                }));
                                                                        cancel();
                                                                    }
                                                                }
                                                            }.runTaskTimer(plugin, 40L, 20L);
                                                        }
                                                    });
                                        }
                                    }
                                }.runTaskTimer(plugin, 20L, 20L)));
    }
    String getSupplyDropWinnerPrefix(String name, long amount) {
        return "supply drop winner: " + name + ". (" + amount + ")";
    }
    void handleAbstractReducePunishmentCommand(String[] args, CommandSender sender, String punishmentTypeName) {
        if (args.length < 2)
            sender.sendMessage("?");
        else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
            fetchOptionalDict(POSTGRES_POOL, "SELECT * FROM reduce_user_punishments_by_type_returning_star(?, ?, ?)", new Object[]{offlinePlayer.getUniqueId(), POSTGRESQL_SERVER_ID, punishmentTypeName})
                    .thenAccept(optional -> {
                        if (optional.isEmpty())
                            sender.sendMessage("invalid (" + punishmentTypeName + ")");
                        else
                            Bukkit.broadcast(getFocusComponent(offlinePlayer.getName() + " has been un-" + punishmentTypeName + "'d by " + sender.getName() + ". reason: " + args[1]));
                    });
        }
    }
}

