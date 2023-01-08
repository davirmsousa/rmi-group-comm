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
        String sql = "CREATE TABLE IF NOT EXISTS message (\n"
                + "	id integer PRIMARY KEY,\n"
                + "	sender text NOT NULL,\n"
                + "	message text NOT NULL\n"
                + ");";

        try {
            Statement stmt = connection.createStatement();
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void insert(Connection conn, int id, String sender, String message) {
        String sql = "INSERT INTO message(id, sender, message) VALUES(?,?,?)";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            pstmt.setString(2, sender);
            pstmt.setString(2, message);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public boolean selectAll(Connection conn) throws SQLException {
        String sql = "SELECT * FROM message";

        try {
            Statement stmt = conn.createStatement();
            stmt.executeQuery(sql);

            return true;
        } catch (SQLException e) {
            throw e;
        }
    }
}
