
package com.memeasaur.potpissersdefault.Util.Potpissers.Constants;

import java.util.HashSet;
import java.util.List;

public class LogoutTeleport {
    // Logout + tag start
    public static final int SAFE_LOGOUT_TIMER = 8;
    public static final int TAG_LOGOUT_TIMER = 30;
    public static final String STRING_LOGOUT = "/logout in ";
    // Claims start
    public static final String STRING_TPA = "/tpa in ";
    public static final String STRING_SPAWN = "/spawn in ";
    public static final String STRING_WARP = "/warp in ";
    // Claims end
    public static final HashSet<String> PVP_WARPS = new HashSet<>(List.of(
            // Claims start
            STRING_TPA
            // Claims end
    ));
    // Logout + tag end

    // Shulker cd start
    public static final int LONG_LOGOUT_TIMER = 180;
    // Shulker cd end
}