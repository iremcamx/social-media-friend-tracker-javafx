package com.example.friendtracker.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class Account { // Abstract tanımlandı
    private final String username;
    private String password;
    private final List<Account> friends = new ArrayList<>();
    private final List<Account> following = new ArrayList<>();

    public Account(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Ortak metodlar
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public List<Account> getFriends() { return Collections.unmodifiableList(friends); }
    public List<Account> getFollowing() { return Collections.unmodifiableList(following); }

    public boolean addFriend(Account acc) {
        if (acc == null || acc.equals(this) || friends.contains(acc)) return false;
        return friends.add(acc);
    }

    public boolean removeFriend(Account acc) { return friends.remove(acc); }

    public boolean follow(Account acc) {
        if (acc == null || acc.equals(this) || following.contains(acc)) return false;
        return following.add(acc);
    }

    public boolean unfollow(Account acc) { return following.remove(acc); }

    // Soyut metod: Her alt sınıf kendini nasıl tanıtacağını kendi belirlemeli
    @Override
    public abstract String toString();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account account)) return false;
        return username.equalsIgnoreCase(account.username);
    }

    @Override
    public int hashCode() { return Objects.hash(username.toLowerCase()); }

}