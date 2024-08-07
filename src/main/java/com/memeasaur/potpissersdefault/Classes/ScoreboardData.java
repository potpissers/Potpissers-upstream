package com.memeasaur.potpissersdefault.Classes;

import net.kyori.adventure.text.format.NamedTextColor;

import static com.memeasaur.potpissersdefault.PotpissersDefault.GAMEMODE_NAME;

public enum ScoreboardData {
    COMBAT(0, "§lcombat§r: ", NamedTextColor.YELLOW, "combat_tag"),
    MOVEMENT(1, "§lmovement§r: ", NamedTextColor.LIGHT_PURPLE, "movement_cd"),
    OPPLE(20, "§lenchanted golden apple§r: ", NamedTextColor.WHITE, "opple_cd"),
    TOTEM(30, "§ltotem of undying§r: ", NamedTextColor.WHITE, "totem_cd");
    public final int score;
    public final String string;
    public final NamedTextColor color;
    public final String sqliteColumnName;
    ScoreboardData(int score, String string, NamedTextColor color, String sqliteColumnName) {
        this.score = score;
        this.string = string;
        this.color = color;
        this.sqliteColumnName = sqliteColumnName;
    }
    public static final String SCOREBOARD_STRING = GAMEMODE_NAME + ".potpissers.com";
}
