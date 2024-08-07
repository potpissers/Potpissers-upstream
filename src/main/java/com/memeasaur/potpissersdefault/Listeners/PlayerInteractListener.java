package com.memeasaur.potpissersdefault.Listeners;

import com.memeasaur.potpissersdefault.Classes.ClaimCoordinate;
import com.memeasaur.potpissersdefault.Classes.LocationCoordinate;
import com.memeasaur.potpissersdefault.Classes.PlayerData;
import com.memeasaur.potpissersdefault.Classes.ScoreboardTimer;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.memeasaur.potpissersdefault.Commands.PotpissersCommands.getShulkerTimer;
import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.*;
import static com.memeasaur.potpissersdefault.Util.CubecoreSwords.Constants.*;
import static com.memeasaur.potpissersdefault.Util.Item.Constants.KEY_SERVER_COMPASS;
import static com.memeasaur.potpissersdefault.Util.Item.Constants.SERVER_COMPASS_INVENTORY;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Constants1.PHANTOM_BLADE_CD;
import static com.memeasaur.potpissersdefault.Util.Claim.Methods.getClaim;
import static com.memeasaur.potpissersdefault.Util.CubecoreSwords.Methods.doLoneSwordEffects;
import static com.memeasaur.potpissersdefault.Util.CubecoreSwords.Methods.doPluviasStormEffects;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Component.getDangerComponent;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Timer.handleScoreboardTimer;
import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods1.handleLootChestBreak;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.KEY_CUBECORE_CHEST;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.SPAWN_CLAIM;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Sqlite.Methods.fetchUpdatePlayerDataIntVoid;

public class PlayerInteractListener implements Listener {
    @EventHandler
    void onPlayerInteract(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        if (e.getAction().isRightClick()) {
            Player p = e.getPlayer();
            if (!p.getGameMode().isInvulnerable() && block != null && e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                Object blockClaim = getClaim(new ClaimCoordinate(block));
                Material blockType = block.getType();
                switch (blockType) {
                    case CHEST -> {
                        Chest chest = (Chest) block.getState();
                        PersistentDataContainer persistentDataContainer = chest.getPersistentDataContainer();
                        if ((persistentDataContainer.has(KEY_CUBECORE_CHEST) && !blockClaim.equals(WILDERNESS_CLAIM) && !playerDataCache.get(p).currentClaim.equals(SPAWN_CLAIM))
                                || persistentDataContainer.has(KEY_SUPPLY_DROP_LOCKED_CHEST)) {
                            e.setCancelled(true);
                            return;
                        }
                    }
                    // Shulker cd start
                    case SHULKER_BOX, ENDER_CHEST, RED_SHULKER_BOX,
                         BLUE_SHULKER_BOX,
                         BLACK_SHULKER_BOX,
                         YELLOW_SHULKER_BOX,
                         BROWN_SHULKER_BOX,
                         LIME_SHULKER_BOX,
                         GRAY_SHULKER_BOX,
                         LIGHT_GRAY_SHULKER_BOX,
                         MAGENTA_SHULKER_BOX,
                         GREEN_SHULKER_BOX,
                         WHITE_SHULKER_BOX,
                         ORANGE_SHULKER_BOX,
                         PINK_SHULKER_BOX,
                         PURPLE_SHULKER_BOX,
                         CYAN_SHULKER_BOX,
                         LIGHT_BLUE_SHULKER_BOX -> {
                        PlayerData data = playerDataCache.get(p);
                        data.shulkerLocation = new LocationCoordinate(block);
                        if (data.shulkerCd < getShulkerTimer(data)) {
                            e.setCancelled(true);
                            if (data.shulkerTimerTask instanceof BukkitTask task && !task.isCancelled()) {
                                p.sendMessage(getDangerComponent("cancelled (shulker timer)"));
                                return;
                            }
                            else {
                                data.shulkerCd = Math.max(data.shulkerCd, data.logoutTeleportTimer);
                                doShulkerActionBarMsg(p, data);
                                data.shulkerTimerTask = new BukkitRunnable() {
                                    CompletableFuture<Void> saveTimerQuery = fetchUpdatePlayerDataIntVoid("shulker_cd", data.shulkerCd, data.sqliteId);
                                    @Override
                                    public void run() {
                                        data.shulkerCd++;
                                        if (data.shulkerCd >= getShulkerTimer(data)) {
                                            p.sendActionBar(Component.text(""));
                                            saveTimerQuery
                                                    .thenRun(() -> fetchUpdatePlayerDataIntVoid("shulker_cd", data.shulkerCd, data.sqliteId));
                                            cancel();
                                        }
                                        else {
                                            doShulkerActionBarMsg(p, data);
                                            if (saveTimerQuery.isDone())
                                                saveTimerQuery = fetchUpdatePlayerDataIntVoid("shulker_cd", data.shulkerCd, data.sqliteId);
                                        }
                                    }
                                }.runTaskTimer(plugin, 20L, 20L);
                            }
                        }
                    }
                    // Shulker cd end
                }
            }
            else {
                // Grapple start
                ItemStack is = e.getItem();
                if (is != null) { // TODO instanceof
                    switch (is.getType()) {
                        // Movement cooldown start
                        case BOW -> {
                            if (is.getEnchantmentLevel(Enchantment.PUNCH) > 0 && playerDataCache.get(p).movementTimer.timer != 0) {
                                e.setCancelled(true);
                                p.sendActionBar(getDangerComponent("movement cd active"));
                                return;
                            }
                        }
                        // Movement cooldown end

                        // Server switcher start
                        case COMPASS, RECOVERY_COMPASS -> {
                            if (is.getPersistentDataContainer().has(KEY_SERVER_COMPASS))
                                p.openInventory(SERVER_COMPASS_INVENTORY);
                        }
                        // Server switcher end
                    }
                    // Cubecore swords start
                    if (is.getPersistentDataContainer().has(KEY_EVENT_SWORD)) {
                        String eventSwordType = is.getPersistentDataContainer().getOrDefault(KEY_EVENT_SWORD, PersistentDataType.STRING, "null");
                        switch (eventSwordType) {
                            case LEGENDARY_SWORD_PHANTOM_BLADE ->
                                    handlePhantomBladeEffect(p, e, is);
                            case LEGENDARY_SWORD_AGNIS_RAGE -> {
                                // applies agni's rage effect on next attack
                            }
                            case LEGENDARY_SWORD_LONE_SWORD -> {
                                String eventSwordCooldownString = is.getPersistentDataContainer().get(KEY_EVENT_SWORD_COOLDOWN, PersistentDataType.STRING);
                                if (eventSwordCooldownString != null && LocalDateTime.now().isBefore(LocalDateTime.parse(eventSwordCooldownString))) {
                                    p.sendMessage(getDangerComponent("cancelled (cooldown)"));
                                    return;
                                }
                                else {
                                    ItemMeta itemMeta = is.getItemMeta();
                                    itemMeta.getPersistentDataContainer().set(KEY_EVENT_SWORD_COOLDOWN, PersistentDataType.STRING, LocalDateTime.now().plusMinutes(3).toString());
                                    is.setItemMeta(itemMeta);

                                    p.addPotionEffect(LONE_SWORD_HEALTH_BOOST);
                                    p.addPotionEffect(LONE_SWORD_REGENERATION);
                                    doLoneSwordEffects(p);
                                }
                            }
                            case LEGENDARY_SWORD_PLUVIAS_STORM -> {
                                String eventSwordCooldownString = is.getPersistentDataContainer().get(KEY_EVENT_SWORD_COOLDOWN, PersistentDataType.STRING);
                                if (eventSwordCooldownString != null && LocalDateTime.now().isBefore(LocalDateTime.parse(eventSwordCooldownString))) {
                                    p.sendMessage(getDangerComponent("cancelled (cooldown)"));
                                    return;
                                }
                                else {
                                    ItemMeta itemMeta = is.getItemMeta();
                                    itemMeta.getPersistentDataContainer().set(KEY_EVENT_SWORD_COOLDOWN, PersistentDataType.STRING, LocalDateTime.now().plusMinutes(3).toString());
                                    is.setItemMeta(itemMeta);

                                    p.setFoodLevel(Math.min(p.getFoodLevel() + 5, 20));
                                    p.setSaturation(Math.min(p.getSaturation() + 6, 20));
                                    doPluviasStormEffects(p, 11);
                                }
                            }
                        }
                    }
                    // Cubecore swords end
                }
                // Grapple end
            }
        }
        // LootChest start
        else {
            // LootChest start
            if (block != null && block.getState() instanceof Chest chest) {
                if (lootChestsCache.containsKey(new LocationCoordinate(block)))
                    handleLootChestBreak(block, chest, null);
                else {
                    PersistentDataContainer persistentDataContainer = chest.getPersistentDataContainer();
                    if (persistentDataContainer.get(KEY_SUPPLY_DROP_CHEST, PersistentDataType.INTEGER) instanceof Integer id)
                        handleLootChestBreak(block, chest, Map.entry(id, playerDataCache.get(e.getPlayer())));
                }
            }
            // LootChest end
            // Cubecore swords start
            if (e.getItem() instanceof ItemStack is) {
                Player p = e.getPlayer();
                if (is.getPersistentDataContainer().has(KEY_EVENT_SWORD)) {
                    String eventSwordType = is.getPersistentDataContainer().getOrDefault(KEY_EVENT_SWORD, PersistentDataType.STRING, "null");
                    switch (eventSwordType) {
                        case LEGENDARY_BOW_PHANTOM_BOW ->
                                handlePhantomBladeEffect(p, e, is);
                    }
                }
            }
            // Cubecore swords end
        }
        // LootChest end
    }
    // Cubecore swords start
    void handlePhantomBladeEffect(Player p, Cancellable e, ItemStack is) {
        PlayerData data = playerDataCache.get(p);
        ScoreboardTimer movementTimer = data.movementTimer;
        if (movementTimer.timer != 0) {
            e.setCancelled(true);
            p.sendMessage(getDangerComponent("cancelled (movement cooldown)"));
            return;
        } else {
            handleScoreboardTimer(data.movementTimer, PHANTOM_BLADE_CD, data, data.uuid, data.sqliteId);
            is.damage(8, p);
            p.setVelocity(p.getVelocity().add(p.getLocation().getDirection().multiply(1.3F))); // TODO -> use get-block thing
            p.getWorld().playSound(p, Sound.ENTITY_BREEZE_SLIDE, 1F, 1F);
            p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, p.getLocation(), 3);
        }
    }
    // Cubecore swords end

    void doShulkerActionBarMsg(Player p, PlayerData data) {
        p.sendActionBar(Component.text("shulker (" + (int) p.getLocation().distance(data.shulkerLocation.toLocation()) + ") access in " + (getShulkerTimer(data) - data.shulkerCd)));
    }
    public static void doEnchantmentUpdate(ItemStack is, Enchantment enchantment, int enchantmentMaxLevel, Player p, Cancellable e) {
        if (enchantmentMaxLevel == 0)
            is.removeEnchantment(enchantment);
        else {
            HashMap<Enchantment, Integer> enchantments = new HashMap<>(is.getEnchantments());
            enchantments.put(enchantment, enchantmentMaxLevel);
            is.removeEnchantments();
            is.addEnchantments(enchantments);
        }
        p.sendMessage(getDangerComponent(enchantment.getKey().getKey() + " reduced"));
        p.getWorld().playSound(p, Sound.BLOCK_ANVIL_USE, 1, 1);
        if (e != null)
            e.setCancelled(true);
    }
}
