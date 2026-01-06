package com.example.friendtracker.service;

import com.example.friendtracker.model.Account; // User yerine Account

public interface Followable {
    void follow(Account follower, Account target);
    void unfollow(Account follower, Account target);
}