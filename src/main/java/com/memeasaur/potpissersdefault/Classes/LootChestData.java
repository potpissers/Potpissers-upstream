package com.memeasaur.potpissersdefault.Classes;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import javax.annotation.Nullable;

public record LootChestData(LootTableType lootTableType, int minAmount, int lootVariance, int restockTime, @Nullable BlockFace enumChestDirection, Material blockMaterial) {
}