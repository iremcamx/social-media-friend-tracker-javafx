package com.example.friendtracker;

import com.example.friendtracker.model.Account;
import com.example.friendtracker.model.Influencer;
import com.example.friendtracker.model.User;
import com.example.friendtracker.service.SocialMediaService;
import com.example.friendtracker.service.DatabaseManager;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.image.Image;
import java.util.Arrays;


public class MainApp extends Application {

    private Stage primaryStage;
    private SocialMediaService service = new SocialMediaService();

    private final ObservableList<Account> users = FXCollections.observableArrayList();
    private final ListView<Account> userListView = new ListView<>(users);
    private final ListView<Account> friendsListView = new ListView<>();
    private final ListView<Account> followingListView = new ListView<>();
    private final TextArea logArea = new TextArea();

    private Button logoutBtn, addFriendBtn, removeFriendBtn, followBtn, unfollowBtn, deleteUserBtn, searchBtn;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        try {
            // "Image" kırmızılığı yukarıdaki import ile düzelecektir.
            // Dosya yolunun başına '/' koymayı unutma.
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));
        } catch (Exception e) {
            System.out.println("İkon yüklenemedi, varsayılan ikon kullanılacak.");
        }

        showLoginScreen();
        stage.show();
    }

    private void showLoginScreen() {
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(50));
        Label title = new Label("Sistem Girişi");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        TextField userField = new TextField();
        userField.setPromptText("Kullanıcı Adı");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Şifre");
        Button loginSubmitBtn = new Button("Giriş Yap");
        loginSubmitBtn.setStyle("-fx-background-color: #1da1f2; -fx-text-fill: white;");
        Button registerBtn = new Button("Yeni Kayıt Oluştur");

        loginSubmitBtn.setOnAction(e -> {
            String username = userField.getText();
            String password = passField.getText();

            // HATA BURADAYDI: service.login(found) yerine doğrudan kullanıcı adı ve şifre gönderiyoruz
            if (service.login(username, password)) {
                showMainDashboard();
                log("Sistem: " + username + " başarıyla oturum açtı.");
            } else {
                alert("Hatalı kullanıcı adı veya şifre!");
            }
        });
        registerBtn.setOnAction(e -> onAddUser());
        layout.getChildren().addAll(title, userField, passField, loginSubmitBtn, registerBtn);
        primaryStage.setScene(new Scene(layout, 400, 350));
        primaryStage.show();
    }

    private void showMainDashboard() {
        // Sol taraftaki profil listesine tıklandığında detayları yenile
        userListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> refreshDetails(newV));

        logArea.setEditable(false);
        logArea.setPrefHeight(120);
        userListView.getItems().clear();
        userListView.getItems().add(service.getCurrentUser());

        // --- MODERN AKILLI BUTONLAR ---
        // Bu buton onSmartSearch metodunu çağırdığı için o metodun üzerindeki grilik gidecektir.
        Button smartSearchBtn = new Button("🔍 Kullanıcı Ara");
        smartSearchBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #2196f3; -fx-text-fill: white;");
        smartSearchBtn.setOnAction(e -> onSmartSearch());

        Button discoverBtn = new Button("✨ Keşfet");
        discoverBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-weight: bold;");
        discoverBtn.setOnAction(e -> onDiscoverPeople());

        // Diğer butonlar (Seçim yapılana kadar pasif/gri kalacaklar)
        removeFriendBtn = new Button("👤 Arkadaşı Çıkar");
        removeFriendBtn.setOnAction(e -> onRemoveFriend());
        removeFriendBtn.setDisable(true);

        unfollowBtn = new Button("❌ Takibi Bırak");
        unfollowBtn.setOnAction(e -> onUnfollow());
        unfollowBtn.setDisable(true);

        deleteUserBtn = new Button("🗑️ Hesabı Sil");
        deleteUserBtn.setOnAction(e -> onDeleteUser());

        logoutBtn = new Button("🚪 Çıkış Yap");
        logoutBtn.setOnAction(e -> {
            service.logout();
            showLoginScreen();
        });

        // --- SEÇİM DİNLEYİCİLERİ (Butonları Canlandıran Kısım) ---

        // Takip Edilenler listesinde birine tıklandığında "Takipten Çık" butonunun griliği gider
        followingListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                // DİĞER LİSTEDEKİ SEÇİMİ TEMİZLE
                friendsListView.getSelectionModel().clearSelection();
                // Butonları ayarla
                unfollowBtn.setDisable(false);
                removeFriendBtn.setDisable(true);
            } else {
                unfollowBtn.setDisable(true);
            }
        });
        friendsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                // DİĞER LİSTEDEKİ SEÇİMİ TEMİZLE
                followingListView.getSelectionModel().clearSelection();
                // Butonları ayarla
                removeFriendBtn.setDisable(false);
                unfollowBtn.setDisable(true);
            } else {
                removeFriendBtn.setDisable(true);
            }
        });

        // --- ARAYÜZ YERLEŞİMİ ---
        HBox actions = new HBox(12);
        actions.setPadding(new Insets(15));
        actions.setAlignment(Pos.CENTER);
        // Eski gereksiz butonları (onFollow, onSearchAndAdd) buradan kaldırdık
        actions.getChildren().addAll(smartSearchBtn, discoverBtn, removeFriendBtn, unfollowBtn, deleteUserBtn, logoutBtn);

        VBox details = new VBox(10,
                new Label("👥 Arkadaşlarım"), friendsListView,
                new Label("🌟 Takip Ettiklerim"), followingListView
        );
        details.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setLeft(new VBox(10, new Label("👤 Profilim"), userListView));
        root.setCenter(details);
        root.setBottom(new VBox(8, actions, new Label("📜 İşlem Geçmişi"), logArea));

        primaryStage.setScene(new Scene(root, 1100, 750));

        // Sayfayı ilk açtığımızda kullanıcının güncel verilerini ekrana bas
        refreshDetails(service.getCurrentUser());
        log("Sistem: Giriş yapıldı. " + service.getCurrentUser().getUsername() + " olarak oturum açtınız.");
    }


    private void handleFoundUser(Account target) {
        Alert choice = new Alert(Alert.AlertType.CONFIRMATION);
        choice.setTitle("Kullanıcı Bulundu");
        choice.setHeaderText(target.getUsername() + " bulundu.");
        choice.setContentText("Hangi işlemi yapmak istersiniz?");
        ButtonType btnFriend = new ButtonType("Arkadaş Ekle");
        ButtonType btnFollow = new ButtonType("Takip Et");
        ButtonType btnCancel = new ButtonType("İptal", ButtonBar.ButtonData.CANCEL_CLOSE);
        choice.getButtonTypes().setAll(btnFriend, btnFollow, btnCancel);

        choice.showAndWait().ifPresent(response -> {
            if (response == btnFriend) {
                if (target instanceof Influencer) alert("Hata: Influencerlar arkadaş eklenemez!");
                else {
                    service.addFriend(service.getCurrentUser(), target);
                    log("İşlem: " + target.getUsername() + " arkadaş olarak eklendi."); // LOG
                }
            } else if (response == btnFollow) {
                if (target instanceof User) alert("Hata: Normal kullanıcılar takip edilemez!");
                else {
                    service.follow(service.getCurrentUser(), target);
                    log("İşlem: " + target.getUsername() + " takip edilmeye başlandı."); // LOG
                }
            }
            refreshDetails(service.getCurrentUser());
        });
    }
    private void onSmartSearch() {
        Account current = service.getCurrentUser();
        // 1. Kullanıcıyı seç
        Account target = pickTargetUser("Kullanıcı Ara", "Etkileşime geçmek istediğiniz kişiyi seçin:", current);

        if (target != null) {
            // 2. İşlem tipini seçtir (Keşfet butonundaki mantık)
            List<String> options = Arrays.asList("Arkadaş Ekle", "Takip Et");
            ChoiceDialog<String> dialog = new ChoiceDialog<>("Arkadaş Ekle", options);
            dialog.setTitle("İşlem Seçin");
            dialog.setHeaderText(target.getUsername() + " için ne yapmak istersiniz?");
            dialog.setContentText("İşlem:");

            Optional<String> result = dialog.showAndWait();

            if (result.isPresent()) {
                String choice = result.get();

                if (choice.equals("Arkadaş Ekle")) {
                    // --- ARKADAŞLIK KONTROLLERİ ---
                    if (target instanceof Influencer) {
                        alert("Hata: Influencerlar arkadaş olarak eklenemez, sadece takip edilebilir!");
                    } else if (current.getFriends().stream().anyMatch(f -> f.getUsername().equalsIgnoreCase(target.getUsername()))) {
                        alert("Uyarı: " + target.getUsername() + " ile zaten arkadaşsınız!");
                    } else {
                        service.addFriend(current, target);
                        log("İşlem: " + target.getUsername() + " arkadaş eklendi.");
                        refreshDetails(current);
                    }
                } else if (choice.equals("Takip Et")) {
                    // --- TAKİP KONTROLLERİ ---
                    if (!(target instanceof Influencer)) {
                        alert("Hata: Sadece Influencerlar takip edilebilir!");
                    } else if (current.getFollowing().stream().anyMatch(a -> a.getUsername().equalsIgnoreCase(target.getUsername()))) {
                        alert("Uyarı: " + target.getUsername() + " zaten takip listenizde!");
                    } else {
                        service.follow(current, target);
                        log("İşlem: " + target.getUsername() + " takip edildi. (Yeni Takipçi: " + ((Influencer)target).getFollowerCount() + ")");
                        refreshDetails(current);
                    }
                }
            }
        }
    }

    private void onDiscoverPeople() {
        Account me = service.getCurrentUser();
        if (me == null) return;
        Map<Account, Integer> recommendations = new HashMap<>();

        // 1. Ortak Arkadaş (2 Puan)
        for (Account myFriend : me.getFriends()) {
            for (Account pFriend : myFriend.getFriends()) {
                if (!pFriend.equals(me) && !me.getFriends().contains(pFriend)) {
                    recommendations.put(pFriend, recommendations.getOrDefault(pFriend, 0) + 2);
                }
            }
        }
        // 2. Influencer Önerisi (1 Puan)
        for (Account acc : service.getAllUsersFromDB()) {
            if (acc instanceof Influencer && !me.getFollowing().contains(acc)) {
                recommendations.put(acc, recommendations.getOrDefault(acc, 0) + 1);
            }
        }

        List<Account> sorted = recommendations.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .map(Map.Entry::getKey).collect(Collectors.toList());

        if (sorted.isEmpty()) alert("Şu an öneri yok.");
        else {
            ChoiceDialog<Account> dialog = new ChoiceDialog<>(sorted.get(0), sorted);
            dialog.setTitle("Keşfet");
            dialog.setHeaderText("Önerilen Kişiler");
            dialog.showAndWait().ifPresent(this::handleFoundUser);
        }
    }

    private void onAddUser() {
        TextField nameField = new TextField();
        PasswordField passField = new PasswordField();
        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("User", "Influencer"));
        typeBox.setValue("User");
        VBox layout = new VBox(10, new Label("Kullanıcı Adı:"), nameField, new Label("Şifre:"), passField, typeBox);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(layout);
        dialog.setTitle("Yeni Kullanıcı Kaydı");

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String inputName = nameField.getText().trim();
                if (service.getAllUsersFromDB().stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(inputName))) {
                    alert("Hata: Bu kullanıcı adı zaten alınmış!");
                    return;
                }
                Account u = typeBox.getValue().equals("Influencer") ? new Influencer(inputName, passField.getText()) : new User(inputName, passField.getText());
                service.saveUserToDB(u);
                users.add(u);
                alert("Kayıt başarılı!");
            }
        });
    }

    private void onDeleteUser() {
        if (new Alert(Alert.AlertType.CONFIRMATION, "Hesabınızı silmek istiyor musunuz?").showAndWait().get() == ButtonType.OK) {
            String name = service.getCurrentUser().getUsername();
            service.deleteAccount(service.getCurrentUser());
            users.remove(service.getCurrentUser());
            service.logout();
            showLoginScreen();
            // Bu logu giriş ekranındaki log alanına yazar
            System.out.println("Sistem: " + name + " hesabı silindi.");
        }
    }


    private void onRemoveFriend() {
        Account selected = friendsListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            service.removeFriend(service.getCurrentUser(), selected);
            log("İşlem: " + selected.getUsername() + " arkadaştan çıkarıldı."); // LOG
            refreshDetails(service.getCurrentUser());
        }
    }

    private void onFollow() {
        Account target = pickTargetUser("Takip Et", "Kimi takip etmek istiyorsun?", service.getCurrentUser());

        if (target != null) {
            Account me = service.getCurrentUser();

            // 1. KONTROL: Zaten takip ediliyor mu? (Username üzerinden kontrol en sağlamıdır)
            boolean isAlreadyFollowing = me.getFollowing().stream()
                    .anyMatch(a -> a.getUsername().equals(target.getUsername()));

            if (isAlreadyFollowing) {
                alert("Hata: " + target.getUsername() + " zaten takip listenizde!");
                return;
            }

            // 2. KONTROL: Zaten arkadaş mısınız?
            boolean isAlreadyFriend = me.getFriends().stream()
                    .anyMatch(f -> f.getUsername().equals(target.getUsername()));

            if (isAlreadyFriend) {
                alert("Hata: Arkadaş olduğunuz birini takip edemezsiniz!");
                return;
            }

            // 3. İŞLEM: Sadece Influencerlar takip edilebilir
            if (target instanceof Influencer) {
                service.follow(me, target); // Servisi çağır

                log("İşlem: " + target.getUsername() + " takip edildi. (Güncel Takipçi: " + ((Influencer) target).getFollowerCount() + ")");

                refreshDetails(me);
            } else {
                alert("Hata: Sadece Influencerlar takip edilebilir!");
            }
        }
    }

    private void onUnfollow() {
        Account selected = followingListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            service.unfollow(service.getCurrentUser(), selected);

            String logMesaji = "İşlem: " + selected.getUsername() + " takibi bırakıldı.";
            if (selected instanceof Influencer) {
                logMesaji += " (Güncel: " + ((Influencer) selected).getFollowerCount() + ")";
            }
            log(logMesaji);

            // Listeyi anında güncelle
            refreshDetails(service.getCurrentUser());
        } else {
            alert("Lütfen listeden birini seçin.");
        }
    }

    private void refreshDetails(Account selected) {
        if (selected == null) return;

        Account me = service.getCurrentUser();

        // Listeleri doldur
        friendsListView.setItems(FXCollections.observableArrayList(selected.getFriends()));
        followingListView.setItems(FXCollections.observableArrayList(selected.getFollowing()));

        // --- ÖNEMLİ: Seçilen kişi zaten takip ediliyorsa uyarıyı burada da yönetebiliriz ---
        if (selected instanceof Influencer) {
            boolean isAlreadyFollowing = me.getFollowing().stream()
                    .anyMatch(a -> a.getUsername().equalsIgnoreCase(selected.getUsername()));

            // Eğer zaten takip ediliyorsa log alanına bir not düşebiliriz
            if(isAlreadyFollowing) {
                log("Sistem Notu: Bu Influencer zaten takibinizde.");
            }
        }

        // Arayüzü tazele
        userListView.refresh();
        followingListView.refresh();
        friendsListView.refresh();
    }

    private Account pickTargetUser(String title, String header, Account exclude) {
        ChoiceDialog<Account> dialog = new ChoiceDialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        // HATA DÜZELTİLDİ: service.getAllUsers() yerine service.getAllUsersFromDB() kullanıldı
        service.getAllUsersFromDB().stream()
                .filter(u -> !u.getUsername().equals(exclude.getUsername()))
                .forEach(u -> dialog.getItems().add(u));

        return dialog.showAndWait().orElse(null);
    }

    private void log(String msg) { logArea.appendText(msg + "\n"); }
    private void alert(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
    public static void main(String[] args) { launch(args); }
}