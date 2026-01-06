package com.example.friendtracker.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    // Veritabanı dosyasının yolu (Başına ./ ekleyerek proje kök dizinini garantiye aldık)
    private static final String URL = "jdbc:sqlite:./friendtracker.db";

    public static Connection getConnection() throws SQLException {
        // SQLite sürücüsünün yüklendiğinden emin olmak için (Bazı eski sistemler için gerekebilir)
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver bulunamadı!");
        }
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() {
        // Kullanıcılar tablosu (Şifre sütunu dahil)
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL," +
                "type TEXT NOT NULL," +
                "follower_count INTEGER DEFAULT 0" +
                ");";

        // Arkadaşlık tablosu
        String createFriendsTable = "CREATE TABLE IF NOT EXISTS friends (" +
                "user_id1 INTEGER," +
                "user_id2 INTEGER," +
                "FOREIGN KEY(user_id1) REFERENCES users(id)," +
                "FOREIGN KEY(user_id2) REFERENCES users(id)" +
                ");";

        // Takip tablosu
        String createFollowingTable = "CREATE TABLE IF NOT EXISTS following (" +
                "follower_id INTEGER," +
                "target_id INTEGER," +
                "FOREIGN KEY(follower_id) REFERENCES users(id)," +
                "FOREIGN KEY(target_id) REFERENCES users(id)" +
                ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Sorguları sırayla çalıştır
            stmt.execute(createUsersTable);
            stmt.execute(createFriendsTable);
            stmt.execute(createFollowingTable);

            System.out.println("Veritabanı bağlantısı kuruldu ve tablolar güncellendi.");
        } catch (SQLException e) {
            System.err.println("Veritabanı hatası: " + e.getMessage());
        }
    }
}