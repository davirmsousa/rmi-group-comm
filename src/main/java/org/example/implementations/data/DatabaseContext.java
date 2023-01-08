package org.example.implementations.data;

import java.sql.*;

public class DatabaseContext {
    private Connection connection = null;

    public DatabaseContext(String databaseName) throws SQLException {
        // db parameters
        String url = "jdbc:sqlite:{file}";
        // create a connection to the database
        connection = DriverManager.getConnection(url);
        System.out.println("[DatabaseContext] Connection to SQLite has been established.");

        createDatabase(databaseName);
    }

    private void createDatabase(String databaseName) throws SQLException {
        String sql = "CREATE DATABASE " + databaseName +";";
        Statement stmt = connection.createStatement();
        if (!stmt.execute(sql))
            throw new SQLException("Não foi possível criar o banco de dados");



        sql = "CREATE TABLE IF NOT EXISTS message (\n"
                + "	id integer PRIMARY KEY,\n"
                + "	message text NOT NULL\n"
                + ");";
        stmt = connection.createStatement();
        if (!stmt.execute(sql))
            throw new SQLException("Não foi possível criar o a tabela");
    }
}
