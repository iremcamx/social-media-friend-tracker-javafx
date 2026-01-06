package com.example.friendtracker.service;

import com.example.friendtracker.model.Account; // Account eklendi
import com.example.friendtracker.model.Influencer;
import com.example.friendtracker.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SocialMediaService implements Followable {

    private Account currentUser = null; // User -> Account

    public boolean login(Account user) { // User -> Account
        if (user != null) {
            this.currentUser = user;
            return true;
        }
        return false;
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

    public void addFriend(Account a, Account b) { // User -> Account
        if (a == null || b == null || a.equals(b)) return;
        if (a.addFriend(b) && b.addFriend(a)) {
            String query = "INSERT INTO friends (user_id1, user_id2) SELECT a.id, b.id FROM users a, users b WHERE a.username = ? AND b.username = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, a.getUsername());
                pstmt.setString(2, b.getUsername());
                pstmt.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    @Override
    public void follow(Account follower, Account target) { // User -> Account
        if (follower == null || target == null || follower.equals(target)) return;
        if (follower.follow(target)) {
            String query = "INSERT INTO following (follower_id, target_id) SELECT a.id, b.id FROM users a, users b WHERE a.username = ? AND b.username = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, follower.getUsername());
                pstmt.setString(2, target.getUsername());
                pstmt.executeUpdate();
                if (target instanceof Influencer inf) {
                    inf.increaseFollowerCount();
                    updateFollowerCountInDB(inf);
                }
            } catch (SQLException e) { e.printStackTrace(); }
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
        } catch (SQLException e) { e.printStackTrace(); }
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