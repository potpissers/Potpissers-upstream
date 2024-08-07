package com.memeasaur.potpissersdefault.Util.Serialization.SQL.Sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.SQL.Methods.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.handlePotpissersExceptions;

public class Methods {
    public static CompletableFuture<Void> fetchUpdatePlayerDataVoid(String[] sqliteElementNames, Object[] orderedMap) {
        StringBuilder query = new StringBuilder("UPDATE player_data SET ");
        for (int i = 0; i < sqliteElementNames.length; i++) {
            if (i > 0)
                query.append(", ");
            query.append(sqliteElementNames[i]).append(" = ?");
        }
        query.append(" WHERE user_id = ?");
        return fetchQueryVoid(SQLITE_POOL, query.toString(), orderedMap);
    }
    public static CompletableFuture<Void> fetchUpdatePlayerDataIntVoid(String timerName, int timer, int sqliteId) {
        CompletableFuture<Void> futureVoid = new CompletableFuture<>();

        SCHEDULER.runTaskAsynchronously(plugin, () ->
        {
            try (Connection connection = SQLITE_POOL.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("UPDATE player_data SET " + timerName + " = ? WHERE user_id = ?")) {
                preparedStatement.setInt(1, timer);
                preparedStatement.setInt(2, sqliteId);
                preparedStatement.executeUpdate();
                SCHEDULER.runTask(plugin, () ->
                        futureVoid.complete(null));
            } catch (SQLException e) {
                handlePotpissersExceptions(futureVoid, e);
            }
        });

        return futureVoid;
    }
}
