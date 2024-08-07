package com.memeasaur.potpissersdefault.Classes;

import java.time.LocalDateTime;
import java.util.UUID;

public record KillData(UUID killer, UUID victim, LocalDateTime timeStamp, byte[] killWeapon, byte[] killerInventory, byte[] victimInventory, float resultingDtr) {
}
