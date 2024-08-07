package com.memeasaur.potpissersdefault.Classes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public record LocationCoordinate(String worldName, int x, int y, int z, float yaw, float pitch) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    public LocationCoordinate(Location location) {
         this(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getYaw(), location.getPitch());
    }
    public LocationCoordinate(String name, int x, int y, int z) {
        this(name, x, y, z, 0, 0);
    }
    public LocationCoordinate(Block block) {
        this(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }
    public Location toLocation() {
        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null ||
                getClass() != o.getClass())
            return false;
        LocationCoordinate that = (LocationCoordinate) o;
        return x == that.x && y == that.y && z == that.z && Objects.equals(worldName, that.worldName);
    }
    @Override
    public int hashCode() {
        int result = worldName.hashCode();
        result = result * 31 + x;
        result = result * 31 + y;
        result = result * 31 + z;
        return result;
    }
}
