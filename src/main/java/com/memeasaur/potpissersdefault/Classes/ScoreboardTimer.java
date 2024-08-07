package com.memeasaur.potpissersdefault.Classes;

import java.util.UUID;

import static com.memeasaur.potpissersdefault.Util.Potpissers.Methods.Timer.handleScoreboardTimer;

public class ScoreboardTimer {
    public final ScoreboardData scoreboardData;
    public int timer;

    public Integer optionalSpamBuffer;
    public ScoreboardTimer(ScoreboardData scoreboardData, int initialTimer, Integer optionalSpamBuffer, PlayerData data, UUID uuid, int sqliteId) {
        this.scoreboardData = scoreboardData;

        this.timer = initialTimer;
        this.optionalSpamBuffer = optionalSpamBuffer;

        if (initialTimer != 0)
            handleScoreboardTimer(this, 0, data, uuid, sqliteId);
    }
}
