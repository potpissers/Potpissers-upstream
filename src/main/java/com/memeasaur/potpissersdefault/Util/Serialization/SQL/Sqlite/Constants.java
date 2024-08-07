package com.memeasaur.potpissersdefault.Util.Serialization.SQL.Sqlite;

public class Constants {
    public static final String RETURN_PLAYER_DATA_LOGGER_UPDATE = """
            SELECT pd.*, lu.health, lu.bukkit_location, lu.bukkit_inventory
            FROM users
                     JOIN player_data pd ON users.id = pd.user_id
                     JOIN logger_updates lu ON users.id = lu.user_id
            WHERE users.user_uuid = ?""";
    public static final String INSERT_LOGGER_UPDATE = """
            INSERT OR
            REPLACE INTO logger_updates (user_id, health, bukkit_location, bukkit_inventory)
            VALUES (?, ?, ?, ?)""";
    public static final String INSERT_NULL_LOGGER_UPDATE = """
            INSERT OR
            REPLACE INTO logger_updates (user_id, health, bukkit_location, bukkit_inventory)
            VALUES (?, NULL, NULL, NULL)""";
    public static final String RETURN_STASHED_STATE = """
            SELECT health, bukkit_location, bukkit_inventory
            FROM stashed_states
            WHERE user_id = ?""";
    public static final String UPSERT_STASHED_STATE = """
            INSERT OR
            REPLACE INTO stashed_states (user_id, health, bukkit_location, bukkit_inventory)
            VALUES (?, ?, ?, ?)""";
    public static final String INSERT_USER_UUID_RETURN_ID = """
            INSERT OR
            REPLACE INTO users (user_uuid)
            VALUES (?)
            RETURNING id""";
    public static final String INSERT_PLAYER_DATA = """
            INSERT OR
            REPLACE INTO player_data (user_id)
            VALUES (?)""";
    public static final String UPDATE_LOGGER_UPDATE_NULL = """
            UPDATE logger_updates
            SET health          = NULL,
                bukkit_location = NULL
            WHERE user_id = ?""";
    public static final String UPDATE_LOGGER_UPDATE_NULL_INVENTORY = """
            UPDATE logger_updates
            SET bukkit_inventory = NULL
            WHERE user_id = ?""";
}
