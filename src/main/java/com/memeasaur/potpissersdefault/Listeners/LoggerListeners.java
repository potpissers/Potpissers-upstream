package com.memeasaur.potpissersdefault.Listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Piglin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;

import static com.memeasaur.potpissersdefault.Classes.LoggerData.KEY_PIGLIN_LOGGER;
import static com.memeasaur.potpissersdefault.PotpissersDefault.loggerDataCache;

public class LoggerListeners implements Listener {
    @EventHandler
    void onEntityLoad(EntitiesLoadEvent e) {
        for (Entity entity : e.getEntities()) {
            switch (entity.getType()) {
                case PIGLIN -> {
                    Piglin piglin = (Piglin) entity;
                    if (piglin.getPersistentDataContainer().has(KEY_PIGLIN_LOGGER) && !loggerDataCache.containsKey(piglin))
                        piglin.remove();
                }
            }
        }
    }
}
