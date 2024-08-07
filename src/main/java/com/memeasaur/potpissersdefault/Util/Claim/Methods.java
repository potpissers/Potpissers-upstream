package com.memeasaur.potpissersdefault.Util.Claim;

import com.memeasaur.potpissersdefault.Classes.ClaimCoordinate;
import com.memeasaur.potpissersdefault.Classes.LocationCoordinate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Claim.Constants.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Constants.DATA_CLAIMS;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.writeBinaryFile;

public class Methods {
    public static Object getClaim(ClaimCoordinate claimCoordinate) {
        return claims.getOrDefault(claimCoordinate, WILDERNESS_CLAIM);
    }
    public static void handleClaimsSave() {
        if (saveClaimsTask.isDone())
            blockingTasks.add(saveClaimsTask = writeBinaryFile(DATA_CLAIMS, claims));
        else if (queueSaveClaimsTaskTask.isDone())
            queueSaveClaimsTaskTask = saveClaimsTask
                    .thenRun(() ->
                            blockingTasks.add(saveClaimsTask = writeBinaryFile(DATA_CLAIMS, claims)));
    }
    public static boolean isUnbreakableClaim(Object blockClaim) {
        return !blockClaim.equals(WILDERNESS_CLAIM);
    }
    public static void handleOpClaimArea(Player p, Location pos1, Location pos2, Object string) {
        if (pos1 == null || pos2 == null) {
            p.sendMessage("?");
            return;
        }
        else if (!pos1.getWorld().equals(pos2.getWorld())) {
            p.sendMessage("invalid (claim worlds)");
            return;
        }
        int counter = doClaimIterationReturnCounter(new ClaimCoordinate(pos1), new ClaimCoordinate(pos2), string);
        p.sendMessage("claimed, " + counter + " iterations blocked");
    }
    public static int doClaimIterationReturnCounter(ClaimCoordinate pos1, ClaimCoordinate pos2, Object string) {
        int counter = 0;
        String worldName = pos1.worldName();
        for (int x = Math.min(pos1.x(), pos2.x()); x <= Math.max(pos1.x(), pos2.x()); x++)
            for (int z = Math.min(pos1.z(), pos2.z()); z <= Math.max(pos1.z(), pos2.z()); z++) {
                if (claims.putIfAbsent(new ClaimCoordinate(worldName, x, z), string) != null)
                    counter++;
            }
        handleClaimsSave();
        return counter;
    }
    public static int doUnclaimIterationReturnCounter(ClaimCoordinate pos1, ClaimCoordinate pos2, Object claimObject) {
        int counter = 0;
        String worldName = pos1.worldName();
        for (int x = Math.min(pos1.x(), pos2.x()); x <= Math.max(pos1.x(), pos2.x()); x++)
            for (int z = Math.min(pos1.z(), pos2.z()); z <= Math.max(pos1.z(), pos2.z()); z++) {
                ClaimCoordinate claimCoordinate = new ClaimCoordinate(worldName, x, z);
                if (getClaim(claimCoordinate).equals(claimObject)) {
                    claims.remove(claimCoordinate);
                    counter++;
                }
            }
        handleClaimsSave();
        return counter;
    }

    public static void doObjectUnclaim(Object object) {
        claims.entrySet()
                .removeIf(entry ->
                        entry.getValue().equals(object));
        handleClaimsSave();
    }

    public static void doClaimPosSet(Player p, Location mutableClaimPos) {
        Location location = p.getLocation();
        mutableClaimPos.setWorld(location.getWorld());
        mutableClaimPos.set(location.x(), location.y(), location.z());
        mutableClaimPos.setPitch(location.getPitch());
        mutableClaimPos.setYaw(location.getYaw());
        p.sendMessage(location.x() + ", " + location.y() + ", " + location.z());
    }
    public static void handleClaimBlockTask(Block block, int timerDuration, Material originalBlockMaterial) {
        LocationCoordinate locationCoordinate = new LocationCoordinate(block);
        int[] timer = new int[]{timerDuration / 2};
        claimsBlocks.put(locationCoordinate, timer);
        new BukkitRunnable() {
            final World world = block.getWorld();
            @Override
            public void run() {
                if (world.getBlockAt(locationCoordinate.toLocation()).getType().equals(originalBlockMaterial)) {
                    claimsBlocks.remove(locationCoordinate);
                    cancel();
                    return;
                }
                else if (timer[0] != 0)
                    timer[0]--;
                else {
                    world.getBlockAt(locationCoordinate.toLocation()).setType(originalBlockMaterial);
                    claimsBlocks.remove(locationCoordinate);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 40L, 40L);
    }
}
