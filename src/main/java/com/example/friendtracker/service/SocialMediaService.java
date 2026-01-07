package com.example.friendtracker.service;

import com.example.friendtracker.model.Account; // Account eklendi
import com.example.friendtracker.model.Influencer;
import com.example.friendtracker.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SocialMediaService implements Followable {
    private List<Account> users = new ArrayList<>();
    private Account currentUser; // User -> Account

    public boolean login(String username, String password) {
        // Veritabanında hem kullanıcı adını hem şifreyi sorgula
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Şifre doğruysa kullanıcı nesnesini tipine göre oluştur
                String type = rs.getString("type");
                if (type.equals("Influencer")) {
                    this.currentUser = new Influencer(username, password);
                } else {
                    this.currentUser = new User(username, password);
                }

                // İlişkileri yükle (Arkadaş/Takip listesi için)
                loadUserRelations(this.currentUser);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // Eşleşme yoksa false döner
    }

    public void logout() {
        this.currentUser = null;
    }

    public Account getCurrentUser() { // User -> Account
        return currentUser;
    }

    // Listeyi Account tipinde döndürüyoruz
    public List<Account> getAllUsersFromDB() {
        List<Account> userList = new ArrayList<>();
        String query = "SELECT * FROM users";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String username = rs.getString("username");
                String password = rs.getString("password");
                String type = rs.getString("type");
                int followers = rs.getInt("follower_count");

                // Polymorphism: Account referansı hem User hem Influencer tutabilir
                Account u = type.equals("Influencer") ? new Influencer(username, password) : new User(username, password);

                if (u instanceof Influencer inf) {
                    for (int i = 0; i < followers; i++) inf.increaseFollowerCount();
                }
                userList.add(u);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return userList;
    }
    private void loadUserRelations(Account acc) {
        // Arkadaşları yükleme sorgusu
        String friendsQuery = "SELECT u.username, u.type FROM users u " +
                "JOIN friends f ON (u.id = f.user_id1 OR u.id = f.user_id2) " +
                "WHERE (f.user_id1 = (SELECT id FROM users WHERE username = ?) " +
                "OR f.user_id2 = (SELECT id FROM users WHERE username = ?)) " +
                "AND u.username != ?";

        // Takip edilenleri yükleme sorgusu
        String followingQuery = "SELECT u.username FROM users u " +
                "JOIN following f ON u.id = f.target_id " +
                "WHERE f.follower_id = (SELECT id FROM users WHERE username = ?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            // 1. Arkadaşları yükle ve listeye ekle
            try (PreparedStatement pstmt = conn.prepareStatement(friendsQuery)) {
                pstmt.setString(1, acc.getUsername());
                pstmt.setString(2, acc.getUsername());
                pstmt.setString(3, acc.getUsername());
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    String name = rs.getString("username");
                    String type = rs.getString("type");
                    Account friend = type.equals("Influencer") ? new Influencer(name, "") : new User(name, "");
                    acc.addFriend(friend);
                }
            }
            // 2. Takip edilenleri yükle ve listeye ekle
            try (PreparedStatement pstmt = conn.prepareStatement(followingQuery)) {
                pstmt.setString(1, acc.getUsername());
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    acc.follow(new Influencer(rs.getString("username"), ""));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void saveUserToDB(Account u) { // User -> Account
        String query = "INSERT INTO users (username, password, type, follower_count) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, u.getUsername());
            pstmt.setString(2, u.getPassword());
            pstmt.setString(3, (u instanceof Influencer) ? "Influencer" : "User");
            pstmt.setInt(4, (u instanceof Influencer inf) ? inf.getFollowerCount() : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void addFriend(Account a, Account b) {
        if (a == null || b == null || a.equals(b)) return;
        if (b instanceof Influencer) return;

        // Bellek kontrolü: Kullanıcı adları üzerinden kontrol
        boolean exists = a.getFriends().stream()
                .anyMatch(f -> f.getUsername().equalsIgnoreCase(b.getUsername()));

        if (exists) {
            System.out.println("Sistem: Bu kullanıcılar zaten arkadaş.");
            return;
        }

        if (a.addFriend(b) && b.addFriend(a)) {
            // Veritabanı kayıt kodların burada aynen kalsın...
        }
    }
    public List<Account> getAllUsers() {
        // Eğer listenin adı 'users' ise bunu döndürürüz
        if (this.users == null) {
            return new ArrayList<>(); // Null hatası almamak için boş liste döner
        }
        return this.users;
    }


    @Override
    public void follow(Account follower, Account target) {
        if (follower == null || target == null || !(target instanceof Influencer)) return;

        Influencer inf = (Influencer) target;

        // --- BELLEK (RAM) GÜNCELLEMESİ ---
        // follower.follow(inf) metodu Account sınıfı içindeki listeye eklemeyi yapar
        if (follower.follow(inf)) {
            // Nesne üzerindeki sayacı artır
            inf.increaseFollowerCount();

            // --- VERİTABANI GÜNCELLEMESİ ---
            String query = "INSERT INTO following (follower_id, target_id) " +
                    "SELECT a.id, b.id FROM users a, users b " +
                    "WHERE a.username = ? AND b.username = ?";

            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setString(1, follower.getUsername());
                pstmt.setString(2, target.getUsername());
                int result = pstmt.executeUpdate();

                if (result > 0) {
                    updateFollowerCountInDB(inf);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void unfollow(Account follower, Account target) { // User -> Account
        if (follower == null || target == null) return;
        if (follower.unfollow(target)) {
            String query = "DELETE FROM following WHERE follower_id = (SELECT id FROM users WHERE username = ?) " +
                    "AND target_id = (SELECT id FROM users WHERE username = ?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, follower.getUsername());
                pstmt.setString(2, target.getUsername());
                pstmt.executeUpdate();
                if (target instanceof Influencer inf) {
                    inf.decreaseFollowerCount();
                    updateFollowerCountInDB(inf);
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }
    public void deleteAccount(Account accountToDelete) {
        if (accountToDelete == null || users == null) return;

        // 1. Veritabanından sil
        String query = "DELETE FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, accountToDelete.getUsername());
            pstmt.executeUpdate();

            // 2. Bellekteki listeden sil (Hata veren yer burasıysa 'users' ismini kontrol et)
            users.remove(accountToDelete);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void removeFriend(Account a, Account b) { // User -> Account
        if (a == null || b == null) return;
        a.removeFriend(b);
        b.removeFriend(a);
        String query = "DELETE FROM friends WHERE (user_id1 = (SELECT id FROM users WHERE username = ?) AND user_id2 = (SELECT id FROM users WHERE username = ?)) " +
                "OR (user_id1 = (SELECT id FROM users WHERE username = ?) AND user_id2 = (SELECT id FROM users WHERE username = ?))";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, a.getUsername());
            pstmt.setString(2, b.getUsername());
            pstmt.setString(3, b.getUsername());
            pstmt.setString(4, a.getUsername());
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateFollowerCountInDB(Influencer inf) {
        String query = "UPDATE users SET follower_count = ? WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, inf.getFollowerCount());
            pstmt.setString(2, inf.getUsername());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteUser(String username) {
        // Kullanıcıyla ilişkili arkadaşlık ve takip kayıtlarını sil (Manual Cascade)
        String deleteFriends = "DELETE FROM friends WHERE user_id1 = (SELECT id FROM users WHERE username = ?) OR user_id2 = (SELECT id FROM users WHERE username = ?)";
        String deleteFollowing = "DELETE FROM following WHERE follower_id = (SELECT id FROM users WHERE username = ?) OR target_id = (SELECT id FROM users WHERE username = ?)";
        String deleteUser = "DELETE FROM users WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            // 1. Arkadaşlıkları sil
            try (PreparedStatement pstmt = conn.prepareStatement(deleteFriends)) {
                pstmt.setString(1, username);
                pstmt.setString(2, username);
                pstmt.executeUpdate();
            }
            // 2. Takip ilişkilerini sil
            try (PreparedStatement pstmt = conn.prepareStatement(deleteFollowing)) {
                pstmt.setString(1, username);
                pstmt.setString(2, username);
                pstmt.executeUpdate();
            }
            // 3. Kullanıcının kendisini sil
            try (PreparedStatement pstmt = conn.prepareStatement(deleteUser)) {
                pstmt.setString(1, username);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}