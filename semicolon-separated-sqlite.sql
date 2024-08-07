CREATE TABLE IF NOT EXISTS users -- TODO -> switch this to something optimized for writes, probably nosql since this shit ain't that relational
(
    id        INTEGER PRIMARY KEY,
    user_uuid TEXT UNIQUE NOT NULL
);
CREATE TABLE IF NOT EXISTS player_data
(
    user_id                  INTEGER PRIMARY KEY,
    frozen                   BOOLEAN DEFAULT FALSE,
    combat_tag               INTEGER DEFAULT 0,
    logout_teleport_timer    INTEGER DEFAULT 0,
    logout_teleport_string   TEXT,
    logout_teleport_location BLOB,
    movement_cd              INTEGER DEFAULT 0,
    shulker_cd               INTEGER DEFAULT 0,
    opple_cd INTEGER DEFAULT 0,
    totem_cd INTEGER DEFAULT 0,
-- Kollusion start
    speed_pot_cd             INTEGER DEFAULT 0,
    grenade_cd               INTEGER DEFAULT 0,
    shear_cd                 INTEGER DEFAULT 0,
    zombie_sickness_timer    INTEGER,
    hydration_exhaustion     REAL    DEFAULT 10,
    is_potential_bandit      BOOLEAN DEFAULT FALSE,
    bleeding_timers          BLOB,
    sugar_slowness_timers    BLOB,
-- Kollusion end
-- Cubecore start
    cubecore_class           TEXT,
    current_balance          INTEGER DEFAULT 0,

    gapple_cd                INTEGER DEFAULT 0,
    antidote_milk_cd         INTEGER DEFAULT 0,

    sugar_cd                 INTEGER DEFAULT 0,
    feather_cd               INTEGER DEFAULT 0,
    membrane_cd              INTEGER DEFAULT 0,
    regen_cd                 INTEGER DEFAULT 0,
    invis_cd                 INTEGER DEFAULT 0,
    fres_cd                  INTEGER DEFAULT 0,
    water_cd                 INTEGER DEFAULT 0,
    haste_cd                 INTEGER DEFAULT 0,
    wheat_cd                 INTEGER DEFAULT 0,
    strength_cd              INTEGER DEFAULT 0,
    resistance_cd            INTEGER DEFAULT 0,
    melon_cd                 INTEGER DEFAULT 0,
    weakness_cd              INTEGER DEFAULT 0,
    poison_cd                INTEGER DEFAULT 0,
    slowness_cd              INTEGER DEFAULT 0,
    wither_cd                INTEGER DEFAULT 0,
    hunger_cd                INTEGER DEFAULT 0,
    fatigue_cd               INTEGER DEFAULT 0,
    fall_cd                  INTEGER DEFAULT 0,
-- Cubecore end
-- Hcf start
    pvp_protection_timer     INTEGER DEFAULT 0
-- Hcf end
);

CREATE TABLE IF NOT EXISTS logger_updates
(
    user_id          INTEGER PRIMARY KEY,
    health           DOUBLE PRECISION,
    bukkit_location  BLOB,
    bukkit_inventory BLOB
);
CREATE TABLE IF NOT EXISTS stashed_states
(
    user_id          INTEGER PRIMARY KEY,
    health           DOUBLE PRECISION,
    bukkit_location  BLOB,
    bukkit_inventory BLOB
);