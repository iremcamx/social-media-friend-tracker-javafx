package com.example.friendtracker.model;

/**
 * User sınıfı, sistemdeki standart kullanıcıyı temsil eder.
 * Account soyut sınıfından miras alır (Inheritance).
 */
public class User extends Account {

    /**
     * Kullanıcı oluşturucu (Constructor)
     * Üst sınıf olan Account'un constructor'ını çağırır.
     * * @param username Kullanıcı adı
     * @param password Şifre
     */
    public User(String username, String password) {
        super(username, password);
    }

    /**
     * Account sınıfında tanımlanan soyut (abstract) toString metodunun uygulaması.
     * Bu metod, nesne bir ListView veya log ekranında gösterildiğinde
     * nasıl görüneceğini belirler.
     */
    @Override
    public String toString() {
        // getUsername() metodu Account sınıfından miras alınmıştır.
        return getUsername() + " (Kullanıcı)";
    }
}