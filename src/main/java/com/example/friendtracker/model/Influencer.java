package com.example.friendtracker.model;

/**
 * Influencer sınıfı, sistemdeki yüksek takipçili kullanıcıları temsil eder.
 * Account soyut sınıfından miras alır (Inheritance).
 */
public class Influencer extends Account {
    // Influencer'a özgü ek özellik
    private int followerCount;

    /**
     * Influencer oluşturucu (Constructor)
     * @param username Kullanıcı adı
     * @param password Şifre
     */
    public Influencer(String username, String password) {
        // Üst sınıf olan Account'un constructor'ını çağırır
        super(username, password);
    }

    /**
     * Takipçi sayısını döndürür.
     */
    public int getFollowerCount() {
        return followerCount;
    }

    /**
     * Takipçi sayısını bir artırır.
     */
    public void increaseFollowerCount() {
        followerCount++;
    }

    /**
     * Takipçi sayısını bir azaltır (0'ın altına düşemez).
     */
    public void decreaseFollowerCount() {
        if (followerCount > 0) followerCount--;
    }

    /**
     * Account sınıfındaki abstract toString metodunu override eder.
     * Hem Account'tan gelen kullanıcı adını hem de bu sınıfa özgü takipçi sayısını döndürür.
     */
    @Override
    public String toString() {
        // getUsername() Account sınıfından gelir, getFollowerCount() Influencer sınıfından
        return getUsername() + " (Influencer) - " + getFollowerCount() + " Takipçi";
    }
}