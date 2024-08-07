package com.memeasaur.potpissersdefault.Classes;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public enum LootTableType {
    KILL_STREAK_REWARDS(List.of(
    )),
    SUPPLY_DROP(List.of(
    ));
    public final List<Map.Entry<Supplier<ItemStack>, Double>> lootTable;
    LootTableType(List<Map.Entry<Supplier<ItemStack>, Double>> lootTable) {
        this.lootTable = lootTable;
    }
}
