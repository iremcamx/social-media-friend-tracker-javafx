package com.example.friendtracker;

import com.example.friendtracker.model.Account; // Account import edildi
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

public class MainApp extends Application {

    private Stage primaryStage;
    private SocialMediaService service = new SocialMediaService();

    // User yerine Account kullanıyoruz (Polymorphism)
    private final ObservableList<Account> users = FXCollections.observableArrayList();
    private final ListView<Account> userListView = new ListView<>(users);
    private final ListView<Account> friendsListView = new ListView<>();
    private final ListView<Account> followingListView = new ListView<>();
    private final TextArea logArea = new TextArea();

    private Button loginBtn, logoutBtn, addUserBtn, addFriendBtn, removeFriendBtn, followBtn, unfollowBtn, deleteUserBtn;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Sosyal Medya Arkadaş Takibi");

        DatabaseManager.initializeDatabase();
        users.addAll(service.getAllUsersFromDB());

        // --- İKON YÜKLEME ---
        try {
            var iconStream = getClass().getResourceAsStream("/icon.png");
            if (iconStream != null) {
                stage.getIcons().add(new javafx.scene.image.Image(iconStream));
            }
        } catch (Exception e) {
            System.out.println("İkon hatası: " + e.getMessage());
        }

        // İlk ekranı yükle
        showLoginScreen();

        // --- CSS EKLEME BÖLÜMÜ ---
        // showLoginScreen içinde stage.setScene yapıldığı için burada scene'e ulaşabiliriz
        if (stage.getScene() != null) {
            applyStyle(stage.getScene());
        }

        stage.show();
    }

    // Tekrardan kaçınmak için bu yardımcı metodu MainApp içine ekle
    private void applyStyle(Scene scene) {
        try {
            String css = getClass().getResource("/style.css").toExternalForm();
            scene.getStylesheets().clear(); // Varsa eskiyi temizle
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("CSS dosyası yüklenemedi! style.css dosyasının resources klasöründe olduğundan emin olun.");
        }
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
            String user = userField.getText(); // Arayüzden kullanıcı adını al
            String pass = passField.getText(); // Arayüzden şifreyi al

            // Hata veren satır burasıydı. Şimdi 2 argüman (user ve pass) gönderiyoruz:
            if (service.login(user, pass)) {
                showMainDashboard();
                log("Sistem: Giriş başarılı!");
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
        userListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> refreshDetails(newV));
        logArea.setEditable(false);

        logoutBtn = new Button("Çıkış Yap");
        logoutBtn.setOnAction(e -> {
            service.logout();
            showLoginScreen();
        });

        addFriendBtn = new Button("Arkadaş Ekle");
        addFriendBtn.setOnAction(e -> onAddFriend());

        removeFriendBtn = new Button("Arkadaş Çıkar");
        removeFriendBtn.setOnAction(e -> onRemoveFriend());

        followBtn = new Button("Takip Et");
        followBtn.setOnAction(e -> onFollow());

        unfollowBtn = new Button("Takipten Çık");
        unfollowBtn.setOnAction(e -> onUnfollow());

        deleteUserBtn = new Button("Kullanıcıyı Sil");
        deleteUserBtn.setOnAction(e -> onDeleteUser());

        HBox actions = new HBox(10, addFriendBtn, removeFriendBtn, followBtn, unfollowBtn, deleteUserBtn, logoutBtn);
        actions.setPadding(new Insets(10));

        VBox details = new VBox(10, new Label("Arkadaşlar"), friendsListView, new Label("Takip Edilenler"), followingListView);
        details.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setLeft(new VBox(10, new Label("Tüm Kullanıcılar"), userListView));
        root.setCenter(details);
        root.setBottom(new VBox(8, actions, new Label("İşlem Logları"), logArea));

        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.setTitle("Friend Tracker - " + service.getCurrentUser().getUsername());
        refreshDetails(service.getCurrentUser());
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
                String inputPass = passField.getText();

                if (inputName.isEmpty() || inputPass.isEmpty()) {
                    alert("Hata: Boş alan bırakılamaz!");
                    return;
                }

                if (users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(inputName))) {
                    alert("Hata: Bu kullanıcı adı zaten alınmış!");
                    return;
                }

                // Polymorphism: Account referansı, User veya Influencer tutabilir
                Account u = typeBox.getValue().equals("Influencer") ?
                        new Influencer(inputName, inputPass) :
                        new User(inputName, inputPass);

                service.saveUserToDB(u);
                users.add(u);
                log(inputName + " başarıyla kaydedildi.");
            }
        });
    }

    private void onDeleteUser() {
        Account selected = userListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            service.deleteUser(selected.getUsername());
            users.remove(selected);
            log("Silindi: " + selected.getUsername());
        }
    }

    private void onAddFriend() {
        Account target = pickTargetUser("Arkadaş Ekle", "Kimi eklemek istiyorsun?", service.getCurrentUser());
        if (target != null) {
            service.addFriend(service.getCurrentUser(), target);
            refreshDetails(service.getCurrentUser());
            log(target.getUsername() + " ile arkadaş olundu.");
        }
    }

    private void onRemoveFriend() {
        Account selected = friendsListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            service.removeFriend(service.getCurrentUser(), selected);
            friendsListView.getItems().remove(selected);
            log(selected.getUsername() + " arkadaştan çıkarıldı.");
        }
    }

    private void onFollow() {
        Account target = pickTargetUser("Takip Et", "Kimi takip etmek istiyorsun?", service.getCurrentUser());
        if (target != null) {
            service.follow(service.getCurrentUser(), target);
            refreshDetails(service.getCurrentUser());
            userListView.refresh();
            log(target.getUsername() + " takip edildi.");
        }
    }

    private void onUnfollow() {
        Account selected = followingListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            service.unfollow(service.getCurrentUser(), selected);
            followingListView.getItems().remove(selected);
            userListView.refresh();
            log(selected.getUsername() + " takipten çıkıldı.");
        }
    }

    private void refreshDetails(Account selected) {
        if (selected == null) return;
        friendsListView.setItems(FXCollections.observableArrayList(selected.getFriends()));
        followingListView.setItems(FXCollections.observableArrayList(selected.getFollowing()));
    }

    private Account pickTargetUser(String title, String header, Account exclude) {
        ChoiceDialog<Account> dialog = new ChoiceDialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        for (Account u : users) { if (!u.equals(exclude)) dialog.getItems().add(u); }
        return dialog.showAndWait().orElse(null);
    }

    private void log(String msg) { logArea.appendText(msg + "\n"); }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.showAndWait();
    }

    public static void main(String[] args) { launch(args); }
}