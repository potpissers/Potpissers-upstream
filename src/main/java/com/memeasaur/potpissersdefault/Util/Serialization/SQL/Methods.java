package com.memeasaur.potpissersdefault.Util.Serialization.SQL;

import com.zaxxer.hikari.HikariDataSource;

import javax.annotation.Nullable;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.memeasaur.potpissersdefault.PotpissersDefault.*;
import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.handlePotpissersExceptions;

public class Methods {
    public static CompletableFuture<Void> fetchQueryVoid(HikariDataSource hikariDataSource, String query, @Nullable Object[] orderedMap) {
        CompletableFuture<Void> futureVoid = new CompletableFuture<>();

        SCHEDULER.runTaskAsynchronously(plugin, () -> {
            try (Connection connection = hikariDataSource.getConnection()) {
                getOpenPreparedStatement(connection, query, orderedMap).execute();
                SCHEDULER.runTask(plugin, () ->
                        futureVoid.complete(null));

            } catch (SQLException e) {
                handlePotpissersExceptions(futureVoid, e);
            }
        });
        return futureVoid;
    }
    public static <T> CompletableFuture<Optional<T>> fetchSqliteOptionalT(String query, @Nullable Object[] orderedMap, String columnName, Class<T> clazz) {
        CompletableFuture<Optional<T>> futureOptionalElement = new CompletableFuture<>();

        SCHEDULER.runTaskAsynchronously(plugin, () -> {
            try (Connection connection = SQLITE_POOL.getConnection(); PreparedStatement preparedStatement = getOpenPreparedStatement(connection, query, orderedMap); ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    Optional<T> resultElement = Optional.of(resultSet.getObject(columnName, clazz));
                    SCHEDULER.runTask(plugin, () ->
                            futureOptionalElement.complete(resultElement));
                } else
                    SCHEDULER.runTask(plugin, () ->
                            futureOptionalElement.complete(Optional.empty()));

            } catch (SQLException e) {
                handlePotpissersExceptions(futureOptionalElement, e);
            }
        });
        return futureOptionalElement;
    }
    public static <T> CompletableFuture<T> fetchSqliteNonnullT(String query, @Nullable Object[] orderedMap, String columnName, Class<T> clazz) {
        CompletableFuture<T> futureElement = new CompletableFuture<>();

        SCHEDULER.runTaskAsynchronously(plugin, () -> {
            try (Connection connection = SQLITE_POOL.getConnection(); PreparedStatement preparedStatement = getOpenPreparedStatement(connection, query, orderedMap); ResultSet resultSet = preparedStatement.executeQuery()) {

                if (resultSet.next()) {
                    T resultElement = resultSet.getObject(columnName, clazz);
                    SCHEDULER.runTask(plugin, () ->
                            futureElement.complete(resultElement));
                } else
                    throw new SQLException("THE METHOD CLEARLY SAYS NON NULL!!!!!!!!");

            } catch (SQLException e) {
                handlePotpissersExceptions(futureElement, e);
            }
        });
        return futureElement;
    }
    public static CompletableFuture<Optional<HashMap<String, Object>>> fetchOptionalDict(HikariDataSource hikariDataSource, String query, @Nullable Object[] objects) {
        CompletableFuture<Optional<HashMap<String, Object>>> futureDict = new CompletableFuture<>();

        SCHEDULER.runTaskAsynchronously(plugin, () -> {
            try (Connection connection = hikariDataSource.getConnection(); PreparedStatement preparedStatement = getOpenPreparedStatement(connection, query, objects); ResultSet resultSet = preparedStatement.executeQuery()) {

                if (resultSet.next()) {
                    HashMap<String, Object> resultDict = new HashMap<>();
                    ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                    for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++)
                        resultDict.put(resultSetMetaData.getColumnName(i), resultSet.getObject(i));
                    SCHEDULER.runTask(plugin, () ->
                            futureDict.complete(Optional.of(resultDict)));
                } else
                    SCHEDULER.runTask(plugin, () ->
                            futureDict.complete(Optional.empty()));

            } catch (SQLException e) {
                handlePotpissersExceptions(futureDict, e);
            }
        });
        return futureDict;
    }
    public static CompletableFuture<HashMap<String, Object>> fetchNonnullDict(HikariDataSource hikariDataSource, String query, @Nullable Object[] orderedMap) {
        CompletableFuture<HashMap<String, Object>> futureDict = new CompletableFuture<>();

        SCHEDULER.runTaskAsynchronously(plugin, () -> {
            try (Connection connection = hikariDataSource.getConnection(); PreparedStatement preparedStatement = getOpenPreparedStatement(connection, query, orderedMap); ResultSet resultSet = preparedStatement.executeQuery()) {

                if (resultSet.next()) {
                    HashMap<String, Object> resultDict = new HashMap<>();
                    ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                    for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++)
                        resultDict.put(resultSetMetaData.getColumnName(i), resultSet.getObject(i));
                    SCHEDULER.runTask(plugin, () ->
                            futureDict.complete(resultDict));
                }
                else
                    throw new SQLException("THE METHOD CLEARLY SAYS NON NULL!");

            } catch (SQLException e) {
                handlePotpissersExceptions(futureDict, e);
            }
        });
        return futureDict;
    }
    public static CompletableFuture<ArrayList<HashMap<String, Object>>> fetchDictList(HikariDataSource hikariDataSource, String query, @Nullable Object[] orderedMap) {
        CompletableFuture<ArrayList<HashMap<String, Object>>> futureDictList = new CompletableFuture<>();

        SCHEDULER.runTaskAsynchronously(plugin, () -> {
            try (Connection connection = hikariDataSource.getConnection(); PreparedStatement preparedStatement = getOpenPreparedStatement(connection, query, orderedMap); ResultSet resultSet = preparedStatement.executeQuery()) {

                ArrayList<HashMap<String, Object>> resultDictList = new ArrayList<>();
                while (resultSet.next()) {
                    HashMap<String, Object> resultDictIteration = new HashMap<>();
                    ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                    for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++)
                        resultDictIteration.put(resultSetMetaData.getColumnName(i), resultSet.getObject(i));
                    resultDictList.add(resultDictIteration);
                }
                SCHEDULER.runTask(plugin, () ->
                        futureDictList.complete(resultDictList));

            } catch (SQLException e) {
                handlePotpissersExceptions(futureDictList, e);
            }
        });
        return futureDictList;
    }
    public static <T> CompletableFuture<ArrayList<T>> fetchSqliteTList(String query, @Nullable Object[] orderedMap, String columnName, Class<T> clazz) {
        CompletableFuture<ArrayList<T>> futureElementList = new CompletableFuture<>();

        SCHEDULER.runTaskAsynchronously(plugin, () -> {
            try (Connection connection = SQLITE_POOL.getConnection(); PreparedStatement preparedStatement = getOpenPreparedStatement(connection, query, orderedMap); ResultSet resultSet = preparedStatement.executeQuery()) {
                ArrayList<T> resultElementList = new ArrayList<>();
                while (resultSet.next())
                    resultElementList.add(resultSet.getObject(columnName, clazz));

                SCHEDULER.runTask(plugin, () ->
                        futureElementList.complete(resultElementList));

            } catch (SQLException e) {
                handlePotpissersExceptions(futureElementList, e);
            }
        });
        return futureElementList;
    }

    private static void handlePreparedStatementObjects(Object[] objects, int index, PreparedStatement preparedStatement, Connection connection) throws SQLException {
        if (objects != null) {
            int i = index;
            for (Object object : objects) {
                if (object instanceof UUID uuid)
                    preparedStatement.setObject(i, uuid, Types.OTHER); // TODO -> this sucks
                else if (object instanceof UUID[] uuids)
                    preparedStatement.setArray(i, connection.createArrayOf("uuid", uuids));
                else
                    preparedStatement.setObject(i, object);
                i++;
            }
        }
    }
    public static PreparedStatement getOpenPreparedStatement(Connection connection, String query, @Nullable Object[] orderedList) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        handlePreparedStatementObjects(orderedList, 1, preparedStatement, connection);
        return preparedStatement;
    }
    private static Object getFinalCallObject(Object object) throws SQLException {
        return object instanceof Array array ? array.getArray() : object instanceof Blob blob ? blob.getBytes(1, (int) blob.length()) : object;
    }
    public static <T> CompletableFuture<T> fetchPgCallNonnullT(String query, int sqlType, @Nullable Object[] orderedMap, Class<T> clazz) {
        CompletableFuture<T> futureT = new CompletableFuture<>();

        SCHEDULER.runTaskAsynchronously(plugin, () -> {
            try (Connection connection = POSTGRES_POOL.getConnection(); CallableStatement callableStatement = connection.prepareCall(query)) {
                callableStatement.registerOutParameter(1, sqlType);
                handlePreparedStatementObjects(orderedMap, 2, callableStatement, connection);

                callableStatement.execute();

                if (getFinalCallObject(callableStatement.getObject(1)) instanceof Object object)
                    SCHEDULER.runTask(plugin, () ->
                            futureT.complete(clazz.cast(object)));
                else
                    throw new RuntimeException("ERR: NON NULL!");
            } catch (SQLException e) {
                handlePotpissersExceptions(futureT, e);
            }
        });

        return futureT;
    }
    public static <T> CompletableFuture<Optional<T>> fetchPgCallOptionalT(String query, int sqlType, @Nullable Object[] orderedMap, Class<T> clazz) {
        CompletableFuture<Optional<T>> futureT = new CompletableFuture<>();

        SCHEDULER.runTaskAsynchronously(plugin, () -> {
            try (Connection connection = POSTGRES_POOL.getConnection(); CallableStatement callableStatement = connection.prepareCall(query)) {
                callableStatement.registerOutParameter(1, sqlType);
                handlePreparedStatementObjects(orderedMap, 2, callableStatement, connection);

                callableStatement.execute();

                Object object = getFinalCallObject(callableStatement.getObject(1));
                if (object != null)
                    SCHEDULER.runTask(plugin, () ->
                            futureT.complete(Optional.of(clazz.cast(object))));
                else
                    SCHEDULER.runTask(plugin, () ->
                            futureT.complete(Optional.empty()));
            } catch (SQLException e) {
                handlePotpissersExceptions(futureT, e);
            }
        });

        return futureT;
    }
}
