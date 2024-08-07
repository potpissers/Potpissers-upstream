package com.memeasaur.potpissersdefault.Classes;

import org.bukkit.Location;
import org.bukkit.block.Block;

import java.io.Serializable;

public record ClaimCoordinate(String worldName, int x, int z) implements Serializable {
    public ClaimCoordinate(Block block) {
        this(block.getWorld().getName(), block.getX(), block.getZ());
    }
    public ClaimCoordinate(Location location) {
        this(location.getWorld().getName(), location.getBlockX(), location.getBlockZ());
    }
    public ClaimCoordinate(String worldName, String x, String z) {
        this(worldName, Integer.parseInt(x), Integer.parseInt(z));
    }
}
